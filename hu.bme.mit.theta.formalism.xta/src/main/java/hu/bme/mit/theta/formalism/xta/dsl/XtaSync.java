package hu.bme.mit.theta.formalism.xta.dsl;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.common.dsl.Environment;
import hu.bme.mit.theta.core.Expr;
import hu.bme.mit.theta.core.utils.TypeUtils;
import hu.bme.mit.theta.formalism.xta.ChanType;
import hu.bme.mit.theta.formalism.xta.Label;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.SyncContext;

final class XtaSync {

	private final XtaExpression expression;
	private final SyncKind syncKind;

	public XtaSync(final XtaTransition scope, final SyncContext context) {
		checkNotNull(context);
		expression = new XtaExpression(scope, context.fExpression);
		syncKind = context.fReceive != null ? SyncKind.RECEIVE : SyncKind.EMIT;
	}

	public static enum SyncKind {
		EMIT, RECEIVE
	}

	public Label instantiate(final Environment env) {
		final Expr<?> expr = expression.instantiate(env);
		final Expr<ChanType> chanExpr = TypeUtils.cast(expr, ChanType.getInstance());
		if (syncKind == SyncKind.EMIT) {
			return Label.emit(chanExpr);
		} else if (syncKind == SyncKind.RECEIVE) {
			return Label.receive(chanExpr);
		} else {
			throw new AssertionError();
		}
	}

}
