package hu.bme.mit.inf.ttmc.formalism.tcfa;

import java.util.Collection;

import hu.bme.mit.inf.ttmc.constraint.expr.Expr;
import hu.bme.mit.inf.ttmc.constraint.type.BoolType;
import hu.bme.mit.inf.ttmc.formalism.common.Loc;

public interface TCFALoc extends Loc<TCFALoc, TCFAEdge> {

	public boolean isUrgent();

	public Collection<Expr<? extends BoolType>> getInvars();

}