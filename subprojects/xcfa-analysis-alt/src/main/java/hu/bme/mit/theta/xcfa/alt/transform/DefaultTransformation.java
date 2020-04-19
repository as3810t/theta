package hu.bme.mit.theta.xcfa.alt.transform;

import com.google.common.base.Preconditions;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.xcfa.WaitStmt;
import hu.bme.mit.theta.xcfa.XCFA;

import java.util.List;
import java.util.Map;

/**
 * Transforms empty edges to edge with skip stmt.
 * Transforms wait stmt's to two separate stmts. TODO finish
 */
public class DefaultTransformation extends EmptyTransformation {

    public DefaultTransformation(XCFA old) {
        super(old);
    }

    @Override
    protected XCFA.Process.Procedure.Edge copy(Object _builder, XCFA.Process.Procedure.Edge edge) {
        if (edge.getStmts().size() == 0) {
            var source = transformed(_builder, edge.getSource());
            var target = transformed(_builder, edge.getTarget());
            return new XCFA.Process.Procedure.Edge(
                    source, target, List.of(SkipStmt.getInstance())
            );
        }
        Preconditions.checkArgument(edge.getStmts().size() == 1);
        if (edge.getStmts().get(0) instanceof WaitStmt) {
            var stmt = (WaitStmt) edge.getStmts().get(0);
            var source = transformed(_builder, edge.getSource());
            var target = transformed(_builder, edge.getTarget());
            var mid = new XCFA.Process.Procedure.Location(edge.getSource() + source.getName() + "_midwait",
                    Map.of());
            var e1 = new XCFA.Process.Procedure.Edge(source, mid, List.of(
                    new WaitStmt(stmt.getSyncVar())
            ));
            var e2 = new XCFA.Process.Procedure.Edge(mid, target, List.of(
                    new WaitStmt(stmt.getSyncVar())
            ));

            var builder = (XCFA.Process.Procedure.Builder)_builder;
            builder.addLoc(mid);
            builder.addEdge(e2);
            return e1; //builder.addEdge(e1);
        } else {
            return super.copy(_builder, edge);
        }
    }
}
