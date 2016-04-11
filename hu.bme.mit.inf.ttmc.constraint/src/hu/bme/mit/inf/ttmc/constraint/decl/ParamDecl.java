package hu.bme.mit.inf.ttmc.constraint.decl;

import hu.bme.mit.inf.ttmc.constraint.expr.ParamRefExpr;
import hu.bme.mit.inf.ttmc.constraint.type.Type;

public interface ParamDecl<DeclType extends Type> extends Decl<DeclType, ParamDecl<DeclType>> {

	@Override
	public ParamRefExpr<DeclType> getRef();

}