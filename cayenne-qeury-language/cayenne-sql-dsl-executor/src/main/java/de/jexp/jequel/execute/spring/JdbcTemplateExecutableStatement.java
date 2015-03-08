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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.Collection;

@SuppressWarnings({"unchecked"})
public class JdbcTemplateExecutableStatement extends AbstractExecutableStatement {
    private final JdbcTemplate template;

    public JdbcTemplateExecutableStatement(DataSource dataSource, Sql sql) {
        super(dataSource, sql);
        template = new JdbcTemplate(getDataSource());
    }

    public int getValue() {
        return template.queryForInt(getSqlString());
    }

    public <T> T getValue(Class<T> type) {
        return template.queryForObject(getSqlString(), type);
    }

    public <T> Collection<T> mapRows(RowMapper rowMapper) {
        return template.query(getSqlString(), rowMapper);
    }

    public void processRows(RowCallbackHandler rowHandler) {
        template.query(getSqlString(), rowHandler);
    }

    public <I, O> Collection<O> mapBeans(BeanRowMapper<I, O> beanRowMapper) {
        return template.query(getSqlString(), new ResultSetBeanRowMapper<I, O>(beanRowMapper));
    }

    public <O> Collection<O> mapValues(ValueRowMapper<O> valueRowMapper) {
        return template.query(getSqlString(), new ResultSetValueRowMapper<O>(valueRowMapper));
    }

    public <I> void handleBeans(BeanRowHandler<I> beanRowHandler) {
        template.query(getSqlString(), new ResultSetBeanRowHandler<I>(beanRowHandler));
    }

    public void handleValues(ValueRowHandler valueRowHandler) {
        template.query(getSqlString(), new ResultSetValueRowHandler(valueRowHandler));
    }

    public JdbcTemplate getTemplate() {
        return template;
    }
}
