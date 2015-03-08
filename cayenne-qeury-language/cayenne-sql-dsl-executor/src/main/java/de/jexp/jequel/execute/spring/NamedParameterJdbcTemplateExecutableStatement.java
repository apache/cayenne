package de.jexp.jequel.execute.spring;

import de.jexp.jequel.execute.core.AbstractExecutableStatement;
import de.jexp.jequel.jdbc.beanprocessor.BeanRowHandler;
import de.jexp.jequel.jdbc.beanprocessor.BeanRowMapper;
import de.jexp.jequel.jdbc.beanprocessor.ResultSetBeanRowHandler;
import de.jexp.jequel.jdbc.beanprocessor.ResultSetBeanRowMapper;
import de.jexp.jequel.jdbc.valuehandler.ResultSetValueRowHandler;
import de.jexp.jequel.jdbc.valuehandler.ResultSetValueRowMapper;
import de.jexp.jequel.jdbc.valuehandler.ValueRowHandler;
import de.jexp.jequel.jdbc.valuehandler.ValueRowMapper;
import de.jexp.jequel.sql.Sql;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @since 02.11.2007 19:45:17 (c) 2007 jexp.de
 */
@SuppressWarnings({"unchecked"})
public class NamedParameterJdbcTemplateExecutableStatement extends AbstractExecutableStatement {
    private final NamedParameterJdbcOperations namedTemplate;

    public NamedParameterJdbcTemplateExecutableStatement(final DataSource dataSource, final Sql sql) {
        super(dataSource, sql);
        namedTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    protected Map<String, Object> getParams() {
        return getExecutableParams().getNamedParams();
    }

    public int getValue() {
        return namedTemplate.queryForInt(getSqlString(), getParams());
    }

    public <T> T getValue(final Class<T> type) {
        return (T) namedTemplate.queryForObject(getSqlString(), getParams(), type);
    }

    public <T> Collection<T> mapRows(final RowMapper rowMapper) {
        return namedTemplate.query(getSqlString(), getParams(), rowMapper);
    }

    public void processRows(final RowCallbackHandler rowHandler) {
        namedTemplate.query(getSqlString(), getParams(), rowHandler);
    }

    public <I, O> Collection<O> mapBeans(final BeanRowMapper<I, O> beanRowMapper) {
        return namedTemplate.query(getSqlString(), getParams(), new ResultSetBeanRowMapper<I, O>(beanRowMapper));
    }

    public <O> Collection<O> mapValues(final ValueRowMapper<O> valueRowMapper) {
        return namedTemplate.query(getSqlString(), getParams(), new ResultSetValueRowMapper<O>(valueRowMapper));
    }

    public <I> void handleBeans(final BeanRowHandler<I> beanRowHandler) {
        namedTemplate.query(getSqlString(), getParams(), new ResultSetBeanRowHandler<I>(beanRowHandler));
    }

    public void handleValues(final ValueRowHandler valueRowHandler) {
        namedTemplate.query(getSqlString(), getParams(), new ResultSetValueRowHandler(valueRowHandler));
    }

    public JdbcOperations getTemplate() {
        return namedTemplate.getJdbcOperations();
    }

    public NamedParameterJdbcOperations getNamedTemplate() {
        return namedTemplate;
    }

}