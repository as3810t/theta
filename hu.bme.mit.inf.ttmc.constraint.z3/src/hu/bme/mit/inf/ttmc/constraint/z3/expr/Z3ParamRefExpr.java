package hu.bme.mit.inf.ttmc.constraint.z3.expr;

import com.microsoft.z3.Context;

import hu.bme.mit.inf.ttmc.constraint.ConstraintManager;
import hu.bme.mit.inf.ttmc.constraint.decl.ParamDecl;
import hu.bme.mit.inf.ttmc.constraint.expr.defaults.AbstractParamRefExpr;
import hu.bme.mit.inf.ttmc.constraint.type.Type;
import hu.bme.mit.inf.ttmc.constraint.z3.decl.Z3ParamDecl;

public class Z3ParamRefExpr<DeclType extends Type> extends AbstractParamRefExpr<DeclType> implements Z3Expr<DeclType> {

	private final Context context;

	private volatile com.microsoft.z3.Expr term;

	public Z3ParamRefExpr(final ConstraintManager manager, final ParamDecl<DeclType> paramDecl, final Context context) {
		super(manager, paramDecl);
		this.context = context;
	}

	@Override
	public com.microsoft.z3.Expr getTerm() {
		if (term == null) {
			final Z3ParamDecl<?> paramDecl = (Z3ParamDecl<?>) getDecl();
			final com.microsoft.z3.FuncDecl paramSymbol = paramDecl.getSymbol();
			term = context.mkConst(paramSymbol);
		}
		return term;
	}
}