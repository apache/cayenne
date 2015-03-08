package de.jexp.jequel.sql;

import de.jexp.jequel.execute.ExecutableStatement;

import javax.sql.DataSource;

public interface ExecutableStatementFactory {
    ExecutableStatement createExecutableStatement(DataSource dataSource, Sql sql);
}
