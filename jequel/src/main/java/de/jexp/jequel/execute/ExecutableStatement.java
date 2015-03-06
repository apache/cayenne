package de.jexp.jequel.execute;

import de.jexp.jequel.jdbc.beanprocessor.BeanRowHandler;
import de.jexp.jequel.jdbc.beanprocessor.BeanRowMapper;
import de.jexp.jequel.jdbc.valuehandler.ValueRowHandler;
import de.jexp.jequel.jdbc.valuehandler.ValueRowMapper;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 19:39:02 (c) 2007 jexp.de
 */
public interface ExecutableStatement {
    int getValue();

    <T> T getValue(Class<T> type);

    <T> Collection<T> mapRows(RowMapper rowMapper);

    void processRows(RowCallbackHandler rowHandler);

    <I, O> Collection<O> mapBeans(final BeanRowMapper<I, O> beanRowMapper);

    <O> Collection<O> mapValues(final ValueRowMapper<O> valueRowMapper);

    <I> void handleBeans(final BeanRowHandler<I> beanRowHandler);

    void handleValues(final ValueRowHandler valueRowHandler);

    DataSource getDataSource();

    ExecutableStatement withParams(ExecutableParams params);

    ExecutableStatement withParams(Object... params);

    ExecutableStatement withParams(Map<String, Object> params);
}
