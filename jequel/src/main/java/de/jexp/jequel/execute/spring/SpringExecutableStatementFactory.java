package de.jexp.jequel.execute.spring;

import de.jexp.jequel.execute.ExecutableStatement;
import de.jexp.jequel.execute.core.DefaultExecutableParams;
import de.jexp.jequel.sql.ExecutableStatementFactory;
import de.jexp.jequel.sql.Sql;

import javax.sql.DataSource;

/**
 * @author mh14 @ jexp.de
 * @since 04.11.2007 14:17:20 (c) 2007 jexp.de
 */
public class SpringExecutableStatementFactory implements ExecutableStatementFactory {
    public SpringExecutableStatementFactory() {
    }

    public ExecutableStatement createExecutableStatement(final DataSource dataSource, final Sql sql) {
        final DefaultExecutableParams executableParams = DefaultExecutableParams.extractParams(sql);
        if (executableParams.hasParams()) {
            if (executableParams.hasOnlyNamed()) {
                return new NamedParameterJdbcTemplateExecutableStatement(dataSource, sql).withParams(executableParams);
            }
            return new ParametrizedJdbcTemplateExecutableStatement(dataSource, sql).withParams(executableParams);
        }
        return new JdbcTemplateExecutableStatement(dataSource, sql);
    }
}
