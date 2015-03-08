package de.jexp.jequel.execute.spring;

import de.jexp.jequel.execute.core.DefaultExecutableParams;
import de.jexp.jequel.sql.Sql;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author mh14 @ jexp.de
 * @since 05.11.2007 03:25:55 (c) 2007 jexp.de
 *        supplies named parameters from the statement as paramsource
 */
public class StatementParameterSource implements SqlParameterSource {
    private final MapSqlParameterSource mapSqlParameterSource;

    public StatementParameterSource(final Sql sql) {
        final DefaultExecutableParams executableParams = DefaultExecutableParams.extractParams(sql);
        mapSqlParameterSource = new MapSqlParameterSource(executableParams.getNamedParams());
    }

    public boolean hasValue(final String paramName) {
        return mapSqlParameterSource.hasValue(paramName);
    }

    public Object getValue(final String paramName) throws IllegalArgumentException {
        return mapSqlParameterSource.getValue(paramName);
    }

    public int getSqlType(final String paramName) {
        return mapSqlParameterSource.getSqlType(paramName);
    }

    @Override
    public String getTypeName(String s) {
        throw new NotImplementedException();
    }
}
