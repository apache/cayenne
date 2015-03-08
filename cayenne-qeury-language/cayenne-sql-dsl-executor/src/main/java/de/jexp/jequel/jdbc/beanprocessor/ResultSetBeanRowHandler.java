package de.jexp.jequel.jdbc.beanprocessor;

import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 20:35:52 (c) 2007 jexp.de
 */
public class ResultSetBeanRowHandler<I> implements RowCallbackHandler {
    private final BeanRowHandler<I> beanRowHandler;
    private I beanWrapper;

    public ResultSetBeanRowHandler(final BeanRowHandler<I> beanRowHandler) {
        this.beanRowHandler = beanRowHandler;
    }

    public void processRow(final ResultSet rs) throws SQLException {
        beanRowHandler.handleBean(getBeanWrapper(rs));
    }

    protected I getBeanWrapper(final ResultSet rs) throws SQLException {
        if (beanWrapper == null) {
            beanWrapper = ResultSetBeanWrapper.createBeanHandlerWrapper(rs, beanRowHandler);
        }
        return beanWrapper;
    }
}