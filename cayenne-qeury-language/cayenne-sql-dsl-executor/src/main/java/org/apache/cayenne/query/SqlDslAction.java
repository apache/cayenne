package org.apache.cayenne.query;

import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.BaseSQLAction;
import org.apache.cayenne.access.jdbc.ConnectionAwareResultIterator;
import org.apache.cayenne.access.jdbc.JDBCResultIterator;
import org.apache.cayenne.access.jdbc.ParameterBinding;
import org.apache.cayenne.access.jdbc.RowDescriptorBuilder;
import org.apache.cayenne.access.jdbc.SQLStatement;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.types.ExtendedTypeMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SqlDslAction<T> extends BaseSQLAction {

    private final SqlQuery query;
    private final boolean loggable;

    public SqlDslAction(SqlQuery query, DataNode dataNode) {
        super(dataNode);
        this.query = query;
        this.loggable = dataNode.getJdbcEventLogger().isLoggable();
    }


    @Override
    public void performAction(Connection connection, OperationObserver observer) throws SQLException, Exception {
        // bind either positional or named parameters;
        // for legacy reasons named parameters are processed as a batch.. this
        // should go away after 4.0; newer positional parameter only support a single set of values.
        List<Number> counts = runWithPositionalParameters(connection, observer);

        // notify of combined counts of all queries inside SQLTemplate
        // multiplied by the number of parameter sets...
        int[] ints = new int[counts.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = counts.get(i).intValue();
        }

        observer.nextBatchCount(query, ints);
    }

    private List<Number> runWithPositionalParameters(Connection connection, OperationObserver callback) throws Exception {

        SQLStatement compiled = new SQLStatement(
                query.getSql().toString(), // TODO use db dependent formatter
                query.getResultColumns(),
                query.getParametersBindings()
        );

        if (loggable) {
            dataNode.getJdbcEventLogger().logQuery(compiled.getSql(), Arrays.asList(compiled.getBindings()));
        }

        return execute(connection, callback, compiled);
    }

    protected List<Number> execute(Connection connection, OperationObserver callback, SQLStatement compiled) throws SQLException, Exception {

        long t1 = System.currentTimeMillis();
        List<Number> updateCounts = new LinkedList<Number>();
        boolean iteratedResult = callback.isIteratedResult();
        PreparedStatement statement = connection.prepareStatement(compiled.getSql());
        try {
            bind(statement, compiled.getBindings());

            // process a mix of results
            boolean isResultSet = statement.execute();
            boolean firstIteration = true;
            while (true) {

                if (firstIteration) {
                    firstIteration = false;
                } else {
                    isResultSet = statement.getMoreResults();
                }

                if (isResultSet) {

                    ResultSet resultSet = statement.getResultSet();

                    if (resultSet != null) {

                        try {
                            processSelectResult(compiled, connection, statement, resultSet, callback, t1);
                        } finally {
                            if (!iteratedResult) {
                                resultSet.close();
                            }
                        }

                        // ignore possible following update counts and bail early on iterated results
                        if (iteratedResult) {
                            break;
                        }
                    }
                } else {
                    int updateCount = statement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }

                    updateCounts.add(updateCount);
                    dataNode.getJdbcEventLogger().logUpdateCount(updateCount);
                }
            }
        } finally {
            if (!iteratedResult) {
                statement.close();
            }
        }

        return updateCounts;
    }

    protected void processSelectResult(SQLStatement compiled, Connection connection, Statement statement,
                                       ResultSet resultSet, OperationObserver callback, final long startTime) throws Exception {

        boolean iteratedResult = callback.isIteratedResult();

        ExtendedTypeMap types = dataNode.getAdapter().getExtendedTypes();
        RowDescriptorBuilder builder = configureRowDescriptorBuilder(compiled, resultSet);

        RowReader<T> rowReader = (RowReader<T>) dataNode.rowReader(builder.getDescriptor(types), null/*queryMetadata*/);

        ResultIterator<T> it = new JDBCResultIterator<T>(statement, resultSet, rowReader);
        if (iteratedResult) {
            it = new ConnectionAwareResultIterator<T>(it, connection) {
                @Override
                protected void doClose() {
                    dataNode.getJdbcEventLogger().logSelectCount(rowCounter, System.currentTimeMillis() - startTime);
                    super.doClose();
                }
            };

            try {
                callback.nextRows(query, it);
            } catch (Exception ex) {
                it.close();
                throw ex;
            }
        } else {
            // note that we are not closing the iterator here, relying on caller
            // to close the underlying ResultSet on its own... this is a hack,
            // maybe a cleaner flow is due here.
            List<T> resultRows = it.allRows();

            dataNode.getJdbcEventLogger().logSelectCount(resultRows.size(), System.currentTimeMillis() - startTime);

            callback.nextRows(query, resultRows);
        }
    }

    /**
     * @since 3.0
     */
    protected RowDescriptorBuilder configureRowDescriptorBuilder(SQLStatement compiled, ResultSet resultSet)
            throws SQLException {
        RowDescriptorBuilder builder = new RowDescriptorBuilder();
        builder.setResultSet(resultSet);

        // SQLTemplate #result columns take precedence over other ways to determine the type
        if (compiled.getResultColumns().length > 0) {
            builder.setColumns(compiled.getResultColumns());
        }

        return builder;
    }

    /**
     * Binds parameters to the PreparedStatement.
     */
    protected void bind(PreparedStatement preparedStatement, ParameterBinding[] bindings)
            throws SQLException, Exception {

        if (bindings.length > 0) {
            int len = bindings.length;
            for (int i = 0; i < len; i++) {
                dataNode.getAdapter().bindParameter(preparedStatement, bindings[i].getValue(), i + 1,
                        bindings[i].getJdbcType(), bindings[i].getScale());
            }
        }
    }

}
