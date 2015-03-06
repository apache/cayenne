package de.jexp.jequel.jdbc.valuehandler;

import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 20:35:52 (c) 2007 jexp.de
 */
public class ResultSetValueRowHandler implements RowCallbackHandler {
    private final ValueRowHandler valueRowHandler;
    private ValueRowHandlerInvoker valueRowHandlerInvoker;

    public ResultSetValueRowHandler(final ValueRowHandler valueRowHandler) {
        this.valueRowHandler = valueRowHandler;

    }

    public void processRow(final ResultSet rs) throws SQLException {
        getValueRowMapperInvoker(valueRowHandler, rs).processValues(rs);
    }

    private ValueRowHandlerInvoker getValueRowMapperInvoker(final ValueRowHandler valueRowHandler, final ResultSet rs) throws SQLException {
        if (valueRowHandlerInvoker == null) {
            valueRowHandlerInvoker = ValueRowHandlerInvoker.createInvoker(valueRowHandler, rs);
        }
        return valueRowHandlerInvoker;
    }

}