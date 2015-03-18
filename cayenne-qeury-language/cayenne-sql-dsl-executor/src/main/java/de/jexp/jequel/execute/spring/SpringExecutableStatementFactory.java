package de.jexp.jequel.execute.spring;

import de.jexp.jequel.execute.ExecutableStatement;
import de.jexp.jequel.execute.core.DefaultExecutableParams;
import de.jexp.jequel.sql.ExecutableStatementFactory;
import de.jexp.jequel.sql.Sql;

import javax.sql.DataSource;

public class SpringExecutableStatementFactory implements ExecutableStatementFactory {

    public ExecutableStatement createExecutableStatement(DataSource dataSource, Sql sql) {
        DefaultExecutableParams executableParams = DefaultExecutableParams.extractParams(sql);
        if (!executableParams.hasParams()) {
            return new JdbcTemplateExecutableStatement(dataSource, sql);
        }

        if (executableParams.hasOnlyNamed()) {
            return new NamedParameterJdbcTemplateExecutableStatement(dataSource, sql).withParams(executableParams);
        }

        return new ParametrizedJdbcTemplateExecutableStatement(dataSource, sql).withParams(executableParams);
    }
}
