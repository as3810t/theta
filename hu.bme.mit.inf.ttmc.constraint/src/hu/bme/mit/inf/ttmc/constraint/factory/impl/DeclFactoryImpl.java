package hu.bme.mit.inf.ttmc.constraint.factory.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;

import hu.bme.mit.inf.ttmc.constraint.ConstraintManager;
import hu.bme.mit.inf.ttmc.constraint.decl.ConstDecl;
import hu.bme.mit.inf.ttmc.constraint.decl.ParamDecl;
import hu.bme.mit.inf.ttmc.constraint.decl.impl.ConstDeclImpl;
import hu.bme.mit.inf.ttmc.constraint.decl.impl.ParamDeclImpl;
import hu.bme.mit.inf.ttmc.constraint.factory.DeclFactory;
import hu.bme.mit.inf.ttmc.constraint.type.Type;

public class DeclFactoryImpl implements DeclFactory {

	private final HashMap<String, ConstDecl<?>> nameToConst;

	private final ConstraintManager manager;

	public DeclFactoryImpl(final ConstraintManager manager) {
		this.manager = manager;
		nameToConst = new HashMap<String, ConstDecl<?>>();
	}

	@Override
	public <T extends Type> ConstDecl<T> Const(final String name, final T type) {
		checkNotNull(name);
		checkNotNull(type);
		checkArgument(name.length() > 0);
		checkArgument(nameToConst.get(name) == null);

		final ConstDecl<T> constDecl = new ConstDeclImpl<>(manager, name, type);
		nameToConst.put(name, constDecl);
		return constDecl;
	}

	@Override
	public <T extends Type> ParamDecl<T> Param(final String name, final T type) {
		checkNotNull(name);
		checkNotNull(type);
		checkArgument(name.length() > 0);
		final ParamDecl<T> paramDecl = new ParamDeclImpl<T>(manager, name, type);
		return paramDecl;
	}

}