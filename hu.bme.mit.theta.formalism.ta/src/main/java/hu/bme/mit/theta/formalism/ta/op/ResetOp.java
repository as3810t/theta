package hu.bme.mit.theta.formalism.ta.op;

import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.type.IntType;
import hu.bme.mit.theta.core.type.RatType;
import hu.bme.mit.theta.formalism.common.decl.ClockDecl;

public interface ResetOp extends ClockOp {

	public ClockDecl getClock();

	public int getValue();

	@Override
	public AssignStmt<RatType, IntType> toStmt();

}