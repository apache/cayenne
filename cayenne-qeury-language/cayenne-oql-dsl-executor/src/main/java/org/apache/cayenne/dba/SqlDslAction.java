package org.apache.cayenne.dba;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.BaseSQLAction;
import org.apache.cayenne.query.DslObjectSelect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlDslAction<T> extends BaseSQLAction {

    private final Sql<T> query;

    public SqlDslAction(DslObjectSelect<T> query, DataNode dataNode) {
        super(dataNode);
        this.query = query;
    }


    @Override
    public void performAction(Connection connection, OperationObserver observer) throws SQLException, Exception {
        String template = extractTemplateString();

        // sanity check - misconfigured templates
        if (template == null) {
            throw new CayenneException("No template string configured for adapter " + dbAdapter.getClass().getName());
        }

        boolean loggable = dataNode.getJdbcEventLogger().isLoggable();
        List<Number> counts = new ArrayList<Number>();

        // bind either positional or named parameters;
        // for legacy reasons named parameters are processed as a batch.. this
        // should go away after 4.0; newer positional parameter only support a
        // single set of values.
        if (query.getPositionalParams().isEmpty()) {
            runWithNamedParametersBatch(connection, callback, template, counts, loggable);
        } else {
            runWithPositionalParameters(connection, callback, template, counts, loggable);
        }

        // notify of combined counts of all queries inside SQLTemplate
        // multiplied by the
        // number of parameter sets...
        int[] ints = new int[counts.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = counts.get(i).intValue();
        }

        callback.nextBatchCount(query, ints);

    }
}
