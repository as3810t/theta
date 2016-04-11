package hu.bme.mit.inf.ttmc.formalism.cfa.impl;

import java.util.List;

import hu.bme.mit.inf.ttmc.common.Tuple2;
import hu.bme.mit.inf.ttmc.common.Tuples;
import hu.bme.mit.inf.ttmc.constraint.factory.ExprFactory;
import hu.bme.mit.inf.ttmc.constraint.type.Type;
import hu.bme.mit.inf.ttmc.formalism.cfa.CFA;
import hu.bme.mit.inf.ttmc.formalism.cfa.CFAEdge;
import hu.bme.mit.inf.ttmc.formalism.cfa.CFALoc;
import hu.bme.mit.inf.ttmc.formalism.common.factory.StmtFactory;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.AssertStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.AssignStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.AssumeStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.BlockStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.DeclStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.DoStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.HavocStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.IfElseStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.IfStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.ReturnStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.SkipStmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.Stmt;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.WhileStmt;
import hu.bme.mit.inf.ttmc.formalism.program.ProgramManager;
import hu.bme.mit.inf.ttmc.formalism.utils.StmtVisitor;

public class SBECreator {

	public static CFA create(final ProgramManager manager, final Stmt stmt) {
		final MutableCFA cfa = new MutableCFA();
		final SBECreatorVisitor visitor = new SBECreatorVisitor(cfa, manager);
		stmt.accept(visitor, Tuples.of(cfa.getInitLoc(), cfa.getFinalLoc()));
		return ImmutableCFA.copyOf(cfa);
	}

	private static class SBECreatorVisitor implements StmtVisitor<Tuple2<CFALoc, CFALoc>, Void> {

		private final MutableCFA cfa;
		private final ExprFactory ef;
		private final StmtFactory sf;

		private SBECreatorVisitor(final MutableCFA cfa, final ProgramManager manager) {
			this.cfa = cfa;
			ef = manager.getExprFactory();
			sf = manager.getStmtFactory();
		}

		private Void visitSimple(final Stmt stmt, final Tuple2<CFALoc, CFALoc> param) {
			final CFALoc source = param._1();
			final CFALoc target = param._2();

			final CFAEdge edge = cfa.createEdge(source, target);
			edge.getStmts().add(stmt);

			return null;
		}

		@Override
		public Void visit(final SkipStmt stmt, final Tuple2<CFALoc, CFALoc> param) {
			final CFALoc source = param._1();
			final CFALoc target = param._2();

			cfa.createEdge(source, target);

			return null;
		}

		@Override
		public <DeclType extends Type, ExprType extends DeclType> Void visit(final DeclStmt<DeclType, ExprType> stmt,
				final Tuple2<CFALoc, CFALoc> param) {
			final CFALoc source = param._1();
			final CFALoc target = param._2();

			final CFAEdge edge = cfa.createEdge(source, target);
			if (stmt.getInitVal().isPresent()) {
				edge.getStmts().add(sf.Assign(stmt.getVarDecl(), stmt.getInitVal().get()));
			} else {
				edge.getStmts().add(sf.Havoc(stmt.getVarDecl()));
			}

			return null;
		}

		@Override
		public Void visit(final AssumeStmt stmt, final Tuple2<CFALoc, CFALoc> param) {
			return visitSimple(stmt, param);
		}

		@Override
		public Void visit(final AssertStmt stmt, final Tuple2<CFALoc, CFALoc> param) {
			final CFALoc source = param._1();
			final CFALoc target = param._2();

			final CFAEdge normalEdge = cfa.createEdge(source, target);
			normalEdge.getStmts().add(sf.Assume(stmt.getCond()));

			final CFAEdge errorEdge = cfa.createEdge(source, cfa.getErrorLoc());
			errorEdge.getStmts().add(sf.Assume(ef.Not(stmt.getCond())));

			return null;
		}

		@Override
		public <DeclType extends Type, ExprType extends DeclType> Void visit(final AssignStmt<DeclType, ExprType> stmt,
				final Tuple2<CFALoc, CFALoc> param) {
			return visitSimple(stmt, param);
		}

		@Override
		public <DeclType extends Type> Void visit(final HavocStmt<DeclType> stmt, final Tuple2<CFALoc, CFALoc> param) {
			return visitSimple(stmt, param);
		}

		@Override
		public Void visit(final BlockStmt stmt, final Tuple2<CFALoc, CFALoc> param) {
			final CFALoc source = param._1();
			final CFALoc target = param._2();

			final List<? extends Stmt> stmts = stmt.getStmts();

			if (stmts.isEmpty()) {
				cfa.createEdge(source, target);
			} else {
				final Stmt head = stmts.get(0);
				final List<? extends Stmt> tail = stmts.subList(1, stmts.size());
				processNonEmptyBlock(cfa, source, target, head, tail);
			}

			return null;
		}

		private void processNonEmptyBlock(final MutableCFA cfa, final CFALoc source, final CFALoc target,
				final Stmt head, final List<? extends Stmt> tail) {

			if (head instanceof ReturnStmt<?> || tail.isEmpty()) {
				head.accept(this, Tuples.of(source, target));
			} else {
				final CFALoc middle = cfa.createLoc();
				head.accept(this, Tuples.of(source, middle));

				final Stmt newHead = tail.get(0);
				final List<? extends Stmt> newTail = tail.subList(1, tail.size());

				processNonEmptyBlock(cfa, middle, target, newHead, newTail);
			}
		}

		@Override
		public <ReturnType extends Type> Void visit(final ReturnStmt<ReturnType> stmt,
				final Tuple2<CFALoc, CFALoc> param) {
			final CFALoc source = param._1();

			final CFAEdge edge = cfa.createEdge(source, cfa.getFinalLoc());
			edge.getStmts().add(stmt);

			return null;
		}

		@Override
		public Void visit(final IfStmt stmt, final Tuple2<CFALoc, CFALoc> param) {
			final CFALoc source = param._1();
			final CFALoc target = param._2();

			final CFALoc thenLoc = cfa.createLoc();
			final CFAEdge thenEdge = cfa.createEdge(source, thenLoc);
			thenEdge.getStmts().add(sf.Assume(stmt.getCond()));
			stmt.getThen().accept(this, Tuples.of(thenLoc, target));

			final CFAEdge elseEdge = cfa.createEdge(source, target);
			elseEdge.getStmts().add(sf.Assume(ef.Not(stmt.getCond())));

			return null;
		}

		@Override
		public Void visit(final IfElseStmt stmt, final Tuple2<CFALoc, CFALoc> param) {
			final CFALoc source = param._1();
			final CFALoc target = param._2();

			final CFALoc thenLoc = cfa.createLoc();
			final CFAEdge thenEdge = cfa.createEdge(source, thenLoc);
			thenEdge.getStmts().add(sf.Assume(stmt.getCond()));
			stmt.getThen().accept(this, Tuples.of(thenLoc, target));

			final CFALoc elseLoc = cfa.createLoc();
			final CFAEdge elseEdge = cfa.createEdge(source, elseLoc);
			elseEdge.getStmts().add(sf.Assume(ef.Not(stmt.getCond())));
			stmt.getElse().accept(this, Tuples.of(elseLoc, target));

			return null;
		}

		@Override
		public Void visit(final WhileStmt stmt, final Tuple2<CFALoc, CFALoc> param) {
			final CFALoc source = param._1();
			final CFALoc target = param._2();

			final CFALoc doLoc = cfa.createLoc();
			final CFAEdge enterEdge = cfa.createEdge(source, doLoc);
			enterEdge.getStmts().add(sf.Assume(stmt.getCond()));

			stmt.getDo().accept(this, Tuples.of(doLoc, source));

			final CFAEdge exitEdge = cfa.createEdge(source, target);
			exitEdge.getStmts().add(sf.Assume(ef.Not(stmt.getCond())));

			return null;
		}

		@Override
		public Void visit(final DoStmt stmt, final Tuple2<CFALoc, CFALoc> param) {
			final CFALoc source = param._1();
			final CFALoc target = param._2();

			final CFALoc doLoc = cfa.createLoc();
			stmt.getDo().accept(this, Tuples.of(source, doLoc));

			final CFAEdge entryEdge = cfa.createEdge(doLoc, source);
			entryEdge.getStmts().add(sf.Assume(stmt.getCond()));

			final CFAEdge exitEdge = cfa.createEdge(doLoc, target);
			exitEdge.getStmts().add(sf.Assume(stmt.getCond()));

			return null;
		}

	}

}