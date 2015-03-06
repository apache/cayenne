package de.jexp.jequel.sql;

import de.jexp.jequel.execute.ExecutableStatement;
import de.jexp.jequel.util.ClassUtils;

import javax.sql.DataSource;

public class DelegatingExecutableStatementFactory implements ExecutableStatementFactory {

    private static final String DEFAULT_STATEMENT_FACTORY = "de.jexp.jequel.execute.spring.SpringExecutableStatementFactory";

    private final ExecutableStatementFactory executableStatementFactory;

    public DelegatingExecutableStatementFactory() {
        this(DEFAULT_STATEMENT_FACTORY);
    }

    public DelegatingExecutableStatementFactory(String statementFactory) {
        this.executableStatementFactory = ClassUtils.newInstance(statementFactory);
    }

    public ExecutableStatementFactory getExecutableStatementFactory() {
        return executableStatementFactory;
    }

    public ExecutableStatement createExecutableStatement(DataSource dataSource, Sql sql) {
        return getExecutableStatementFactory().createExecutableStatement(dataSource, sql);
    }
}
