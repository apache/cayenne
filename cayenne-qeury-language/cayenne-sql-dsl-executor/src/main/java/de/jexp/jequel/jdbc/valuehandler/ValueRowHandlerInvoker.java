package de.jexp.jequel.jdbc.valuehandler;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 08:03:08 (c) 2007 jexp.de
 */
class ValueRowHandlerInvoker extends ValueRowProcessorInvoker {
    public ValueRowHandlerInvoker(final ValueRowHandler valueRowHandler, final ResultSet rs) throws SQLException {
        super(valueRowHandler, rs, ValueRowHandler.HANDLE_VALUE_METHOD);
    }

    public static ValueRowHandlerInvoker createInvoker(final ValueRowHandler valueRowHandler, final ResultSet rs) throws SQLException {
        return new ValueRowHandlerInvoker(valueRowHandler, rs);
    }

    public void processValues(final ResultSet rs) throws SQLException {
        processRow(rs);
    }
}
