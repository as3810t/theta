package hu.bme.mit.inf.ttmc.constraint.expr;

import hu.bme.mit.inf.ttmc.constraint.expr.EqExpr;
import hu.bme.mit.inf.ttmc.constraint.type.BoolType;
import hu.bme.mit.inf.ttmc.constraint.type.Type;

public interface EqExpr extends BinaryExpr<Type, Type, BoolType> {
	
	@Override
	public EqExpr withOps(final Expr<? extends Type> leftOp, final Expr<? extends Type> rightOp);
	
	@Override
	public EqExpr withLeftOp(final Expr<? extends Type> leftOp);

	@Override
	public EqExpr withRightOp(final Expr<? extends Type> rightOp);
	
}