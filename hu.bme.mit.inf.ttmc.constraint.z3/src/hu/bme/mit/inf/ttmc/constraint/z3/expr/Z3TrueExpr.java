package hu.bme.mit.inf.ttmc.constraint.z3.expr;

import com.microsoft.z3.Context;

import hu.bme.mit.inf.ttmc.constraint.ConstraintManager;
import hu.bme.mit.inf.ttmc.constraint.expr.defaults.AbstractTrueExpr;
import hu.bme.mit.inf.ttmc.constraint.type.BoolType;

public class Z3TrueExpr extends AbstractTrueExpr implements Z3Expr<BoolType> {

	@SuppressWarnings("unused")
	private final Context context;

	private final com.microsoft.z3.BoolExpr term;

	public Z3TrueExpr(final ConstraintManager manager, final Context context) {
		super(manager);
		this.context = context;
		term = context.mkTrue();
	}

	@Override
	public com.microsoft.z3.BoolExpr getTerm() {
		return term;
	}
}