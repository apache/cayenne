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

public interface ExecutableStatement {
    int getValue();

    <T> T getValue(Class<T> type);

    <T> Collection<T> mapRows(RowMapper rowMapper);

    void processRows(RowCallbackHandler rowHandler);

    <I, O> Collection<O> mapBeans(BeanRowMapper<I, O> beanRowMapper);

    <O> Collection<O> mapValues(ValueRowMapper<O> valueRowMapper);

    <I> void handleBeans(BeanRowHandler<I> beanRowHandler);

    void handleValues(ValueRowHandler valueRowHandler);

    DataSource getDataSource();

    ExecutableStatement withParams(ExecutableParams params);

    ExecutableStatement withParams(Object... params);

    ExecutableStatement withParams(Map<String, Object> params);
}
