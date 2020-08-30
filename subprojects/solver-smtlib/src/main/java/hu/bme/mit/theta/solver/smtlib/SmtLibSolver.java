package hu.bme.mit.theta.solver.smtlib;

import com.google.common.collect.ImmutableList;
import hu.bme.mit.theta.core.decl.ConstDecl;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.arraytype.ArrayType;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.bvtype.BvType;
import hu.bme.mit.theta.core.type.functype.FuncType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverStatus;
import hu.bme.mit.theta.solver.Stack;
import hu.bme.mit.theta.solver.UCSolver;
import hu.bme.mit.theta.solver.UnknownSolverStatusException;
import hu.bme.mit.theta.solver.impl.StackImpl;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Lexer;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser;
import hu.bme.mit.theta.solver.smtlib.parser.CheckSatResponse;
import hu.bme.mit.theta.solver.smtlib.parser.GeneralResponse;
import hu.bme.mit.theta.solver.smtlib.parser.GetModelResponse;
import hu.bme.mit.theta.solver.smtlib.parser.GetUnsatCoreResponse;
import hu.bme.mit.theta.solver.smtlib.parser.ThrowExceptionErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class SmtLibSolver implements UCSolver, Solver {
    protected final SmtLibSymbolTable symbolTable;
    protected final SmtLibTransformationManager transformationManager;
    protected final SmtLibTermTransformer termTransformer;

    protected final SmtLibSolverBinary solverBinary;
    private final boolean unsatCoreEnabled;

    protected final Stack<Expr<BoolType>> assertions;
    protected final Map<String, Expr<BoolType>> assumptions;
    protected final Stack<ConstDecl<?>> declarationStack;

    private static final String ASSUMPTION_LABEL = "_LABEL_%d";
    private int labelNum = 0;

    private Valuation model;
    private Collection<Expr<BoolType>> unsatCore;
    private SolverStatus status;

    public SmtLibSolver(
        final SmtLibSymbolTable symbolTable, final SmtLibTransformationManager transformationManager,
        final SmtLibTermTransformer termTransformer, final SmtLibSolverBinary solverBinary, boolean unsatCoreEnabled
    ) {
        this.solverBinary = solverBinary;
        this.symbolTable = symbolTable;
        this.transformationManager = transformationManager;
        this.termTransformer = termTransformer;

        this.unsatCoreEnabled = unsatCoreEnabled;

        assertions = new StackImpl<>();
        assumptions = new HashMap<>();
        declarationStack = new StackImpl<>();

        init();
    }

    @Override
    public void add(Expr<BoolType> assertion) {
        final var consts = ExprUtils.getConstants(assertion);
        consts.removeAll(declarationStack.toCollection());
        declarationStack.add(consts);

        final var term = transformationManager.toTerm(assertion);

        consts.stream().map(symbolTable::getDeclaration).forEach(this::issueGeneralCommand);
        issueGeneralCommand(String.format("(assert %s)%n", term));

        clearState();
    }

    public void add(final Expr<BoolType> assertion, final String term) {
        final var consts = ExprUtils.getConstants(assertion);
        consts.removeAll(declarationStack.toCollection());
        declarationStack.add(consts);

        consts.stream().map(symbolTable::getDeclaration).forEach(this::issueGeneralCommand);
        issueGeneralCommand(String.format("(assert %s)%n", term));

        clearState();
    }

    @Override
    public void track(Expr<BoolType> assertion) {
        final var consts = ExprUtils.getConstants(assertion);
        consts.removeAll(declarationStack.toCollection());
        declarationStack.add(consts);

        final var term = transformationManager.toTerm(assertion);
        final var label = String.format(ASSUMPTION_LABEL, labelNum++);
        assumptions.put(label, assertion);

        consts.stream().map(symbolTable::getDeclaration).forEach(this::issueGeneralCommand);
        issueGeneralCommand(String.format("(assert (! %s :named %s))%n", term, label));

        clearState();
    }

    @Override
    public SolverStatus check() {
        var res = parseResponse(solverBinary.issueCommand("(check-sat)"));
        if(res.isError()) {
            throw new SmtLibSolverException(res.getReason());
        }
        else if(res.isSpecific()) {
            final CheckSatResponse checkSatResponse = res.asSpecific();
            if(checkSatResponse.isSat()) {
                status = SolverStatus.SAT;
            }
            else if(checkSatResponse.isUnsat()) {
                status = SolverStatus.UNSAT;
            }
            else {
                throw new UnknownSolverStatusException();
            }
        }
        else {
            throw new AssertionError();
        }

        return status;
    }

    @Override
    public void push() {
        assertions.push();
        declarationStack.push();
        issueGeneralCommand("(push)");
    }

    @Override
    public void pop(int n) {
        assertions.pop(n);
        declarationStack.pop(n);
        issueGeneralCommand("(pop)");
        clearState();
    }

    @Override
    public void reset() {
        issueGeneralCommand("(reset)");
        clearState();
        init();
    }

    @Override
    public SolverStatus getStatus() {
        checkState(status != null, "Solver status is unknown.");
        return status;
    }

    @Override
    public Valuation getModel() {
        checkState(status == SolverStatus.SAT, "Cannot get model if status is not SAT.");

        if (model == null) {
            model = extractModel();
        }

        return model;
    }

    private Valuation extractModel() {
        assert status == SolverStatus.SAT;
        assert model == null;

        final var res = parseResponse(solverBinary.issueCommand("(get-model)"));
        if(res.isError()) {
            throw new SmtLibSolverException(res.getReason());
        }
        else if(res.isSpecific()) {
            final GetModelResponse getModelResponse = res.asSpecific();
            return new SmtLibValuation(getModelResponse.getModel());
        }
        else {
            throw new AssertionError();
        }
    }

    @Override
    public Collection<Expr<BoolType>> getUnsatCore() {
        checkState(status == SolverStatus.UNSAT, "Cannot get unsat core if status is not UNSAT");

        if (unsatCore == null) {
            unsatCore = extractUnsatCore();
        }

        return Collections.unmodifiableCollection(unsatCore);
    }

    private Collection<Expr<BoolType>> extractUnsatCore() {
        assert status == SolverStatus.UNSAT;
        assert unsatCore == null;

        final Collection<Expr<BoolType>> unsatCore = new LinkedList<>();
        final Collection<String> unsatCoreLabels;

        final var res = parseResponse(solverBinary.issueCommand("(get-unsat-core)"));
        if(res.isError()) {
            throw new SmtLibSolverException(res.getReason());
        }
        else if(res.isSpecific()) {
            final GetUnsatCoreResponse getUnsatCoreResponse = res.asSpecific();
            unsatCoreLabels = getUnsatCoreResponse.getLabels();
        }
        else {
            throw new AssertionError();
        }

        for(final var label : unsatCoreLabels) {
            final Expr<BoolType> assumption = assumptions.get(label);
            assert assumption != null;
            unsatCore.add(assumption);
        }

        return unsatCore;
    }

    @Override
    public Collection<Expr<BoolType>> getAssertions() {
        return assertions.toCollection();
    }

    private void init() {
        issueGeneralCommand("(set-option :print-success true)");
        issueGeneralCommand("(set-option :produce-models true)");
        if(unsatCoreEnabled) {
            issueGeneralCommand("(set-option :produce-unsat-cores true)");
        }
        issueGeneralCommand("(set-logic ALL)");
    }

    private void clearState() {
        status = null;
        model = null;
        unsatCore = null;
    }

    private void issueGeneralCommand(String command) {
        var res = parseResponse(solverBinary.issueCommand(command));
        if(res.isError()) {
            throw new SmtLibSolverException(res.getReason());
        }
    }

    private GeneralResponse parseResponse(final String response) {
        try {
            final var lexer = new SMTLIBv2Lexer(CharStreams.fromString(response));
            final var parser = new SMTLIBv2Parser(new CommonTokenStream(lexer));
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowExceptionErrorListener());
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowExceptionErrorListener());
            return GeneralResponse.fromContext(parser.response());
        }
        catch (Exception e) {
            throw new SmtLibSolverException("Could not parse solver output: " + response, e);
        }
    }

    private final class SmtLibValuation extends Valuation {
        private final SmtLibModel model;
        private final Map<Decl<?>, LitExpr<?>> constToExpr;
        private volatile Collection<ConstDecl<?>> constDecls = null;

        public SmtLibValuation(final SmtLibModel model) {
            this.model = model;
            constToExpr = new HashMap<>();
        }

        @Override
        public Collection<? extends Decl<?>> getDecls() {
            Collection<ConstDecl<?>> result = constDecls;
            if (result == null) {
                result = constDeclsOf(model);
                constDecls = result;
            }
            return result;
        }

        @Override
        public <DeclType extends Type> Optional<LitExpr<DeclType>> eval(Decl<DeclType> decl) {
            checkNotNull(decl);

            if (!(decl instanceof ConstDecl)) {
                return Optional.empty();
            }

            final ConstDecl<DeclType> constDecl = (ConstDecl<DeclType>) decl;

            LitExpr<?> val = constToExpr.get(constDecl);
            if (val == null) {
                val = extractLiteral(constDecl);
                if (val != null) {
                    constToExpr.put(constDecl, val);
                }
            }

            @SuppressWarnings("unchecked") final LitExpr<DeclType> tVal = (LitExpr<DeclType>) val;
            return Optional.ofNullable(tVal);
        }

        private <DeclType extends Type> LitExpr<?> extractLiteral(final ConstDecl<DeclType> decl) {
            final String symbol = transformationManager.toSymbol(decl);
            final Type type = decl.getType();

            if(type instanceof FuncType) {
                return extractFuncLiteral(symbol, (FuncType<?, ?>) type);
            }
            else if(type instanceof ArrayType) {
                return extractArrayLiteral(symbol, (ArrayType<?, ?>) type);
            }
            else if (type instanceof BvType) {
                return extractBvConstLiteral(symbol, (BvType) type);
            }
            else {
                return extractConstLiteral(symbol, type);
            }
        }

        private LitExpr<?> extractFuncLiteral(final String symbol, final FuncType<?, ?> type) {
            final String term = model.getTerm(symbol);
            if (term == null) {
                return null;
            } else {
                return checkNotNull(termTransformer.toFuncLitExpr(term, type, model));
            }
        }

        private LitExpr<?> extractArrayLiteral(final String symbol, final ArrayType<?, ?> type) {
            final String term = model.getTerm(symbol);
            if (term == null) {
                return null;
            } else {
                return checkNotNull(termTransformer.toArrayLitExpr(term, type, model));
            }
        }

        private LitExpr<?> extractBvConstLiteral(final String symbol, final BvType type) {
            final String term = model.getTerm(symbol);
            if (term == null) {
                return null;
            } else {
                return checkNotNull(termTransformer.toBvLitExpr(term, type, model));
            }
        }

        private LitExpr<?> extractConstLiteral(final String symbol, final Type type) {
            final String term = model.getTerm(symbol);
            if (term == null) {
                return null;
            } else {
                return checkNotNull(termTransformer.toLitExpr(term, type, model));
            }
        }

        @Override
        public Map<Decl<?>, LitExpr<?>> toMap() {
            getDecls().forEach(this::eval);
            return Collections.unmodifiableMap(constToExpr);
        }

        private Collection<ConstDecl<?>> constDeclsOf(final SmtLibModel model) {
            final ImmutableList.Builder<ConstDecl<?>> builder = ImmutableList.builder();
            for (final var symbol : model.getDecls()) {
                if (symbolTable.definesSymbol(symbol)) {
                    final ConstDecl<?> constDecl = symbolTable.getConst(symbol);
                    builder.add(constDecl);
                }
            }
            return builder.build();
        }
    }

}
