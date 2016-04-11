package hu.bme.mit.inf.ttmc.constraint.decl.defaults;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.inf.ttmc.constraint.decl.Decl;
import hu.bme.mit.inf.ttmc.constraint.type.Type;

public abstract class AbstractDecl<DeclType extends Type, DeclKind extends Decl<DeclType, DeclKind>>
		implements Decl<DeclType, DeclKind> {

	private final String name;
	private final DeclType type;

	protected AbstractDecl(final String name, final DeclType type) {
		checkNotNull(name);
		checkArgument(name.length() > 0);
		this.name = name;
		this.type = checkNotNull(type);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public DeclType getType() {
		return type;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(getDeclLabel());
		sb.append("(");
		sb.append(getName());
		sb.append(", ");
		sb.append(getType());
		sb.append(")");
		return sb.toString();
	}

	protected abstract String getDeclLabel();

}