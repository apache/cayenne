package de.jexp.jequel.sql;

import de.jexp.jequel.execute.ExecutableStatement;

import javax.sql.DataSource;

public class DelegatingExecutableStatementFactory implements ExecutableStatementFactory {

    private final ExecutableStatementFactory executableStatementFactory;

    public DelegatingExecutableStatementFactory(ExecutableStatementFactory statementFactory) {
        this.executableStatementFactory = statementFactory;
    }

    public ExecutableStatementFactory getExecutableStatementFactory() {
        return executableStatementFactory;
    }

    public ExecutableStatement createExecutableStatement(DataSource dataSource, Sql sql) {
        return getExecutableStatementFactory().createExecutableStatement(dataSource, sql);
    }
}
