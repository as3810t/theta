package hu.bme.mit.inf.ttmc.constraint.z3.factory;

import static com.google.common.base.Preconditions.checkNotNull;

import com.microsoft.z3.Context;

import hu.bme.mit.inf.ttmc.constraint.ConstraintManager;
import hu.bme.mit.inf.ttmc.constraint.factory.TypeFactory;
import hu.bme.mit.inf.ttmc.constraint.type.ArrayType;
import hu.bme.mit.inf.ttmc.constraint.type.BoolType;
import hu.bme.mit.inf.ttmc.constraint.type.FuncType;
import hu.bme.mit.inf.ttmc.constraint.type.IntType;
import hu.bme.mit.inf.ttmc.constraint.type.RatType;
import hu.bme.mit.inf.ttmc.constraint.type.Type;
import hu.bme.mit.inf.ttmc.constraint.z3.type.Z3ArrayType;
import hu.bme.mit.inf.ttmc.constraint.z3.type.Z3BoolType;
import hu.bme.mit.inf.ttmc.constraint.z3.type.Z3FuncType;
import hu.bme.mit.inf.ttmc.constraint.z3.type.Z3IntType;
import hu.bme.mit.inf.ttmc.constraint.z3.type.Z3RatType;

public final class Z3TypeFactory implements TypeFactory {

	final ConstraintManager manager;

	final Context context;

	final BoolType boolType;
	final IntType intType;
	final RatType ratType;

	public Z3TypeFactory(final ConstraintManager manager, final Context context) {
		this.manager = manager;
		this.context = context;

		boolType = new Z3BoolType(manager, context);
		intType = new Z3IntType(manager, context);
		ratType = new Z3RatType(manager, context);
	}

	@Override
	public BoolType Bool() {
		return boolType;
	}

	@Override
	public IntType Int() {
		return intType;
	}

	@Override
	public RatType Rat() {
		return ratType;
	}

	@Override
	public <P extends Type, R extends Type> FuncType<P, R> Func(final P paramType, final R resultType) {
		checkNotNull(paramType);
		checkNotNull(resultType);
		return new Z3FuncType<>(manager, paramType, resultType, context);
	}

	@Override
	public <I extends Type, E extends Type> ArrayType<I, E> Array(final I indexType, final E elemType) {
		checkNotNull(indexType);
		checkNotNull(elemType);
		return new Z3ArrayType<>(manager, indexType, elemType, context);
	}

}