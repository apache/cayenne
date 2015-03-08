package de.jexp.jequel.execute.core;

import de.jexp.jequel.SqlString;
import de.jexp.jequel.execute.ExecutableParams;
import de.jexp.jequel.execute.ExecutableStatement;
import de.jexp.jequel.processor.ParameterCollectorProcessor;
import de.jexp.jequel.sql.Sql;

import javax.sql.DataSource;
import java.util.Map;

public abstract class AbstractExecutableStatement implements ExecutableStatement, SqlString {
    private final Sql sql;

    private DataSource dataSource;
    private ExecutableParams executableParams;

    protected AbstractExecutableStatement(DataSource dataSource, Sql sql) {
        this.dataSource = dataSource;
        this.sql = sql;
    }

    public String getSqlString() {
        return sql.toString();
    }

    public Sql getSql() {
        return sql;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public ExecutableParams getExecutableParams() {
        return executableParams;
    }

    public ExecutableStatement clearParams() {
        this.executableParams = DefaultExecutableParams.extractParams(sql);
        return this;
    }

    public ExecutableStatement withParams(ExecutableParams params) {
        if (this.executableParams == null) {
            this.executableParams = params;
        } else {
            this.executableParams.addParams(params);
        }
        return this;
    }

    public ExecutableStatement withParams(Object... params) {
        if (hasNamedParams(sql)) {
            return withParams(DefaultExecutableParams.createNamedParams(params));
        } else {
            return withParams(DefaultExecutableParams.createParams(params));
        }
    }

    // TODO adapt, move responsibility to Expression Params
    private boolean hasNamedParams(Sql sql) {
        ParameterCollectorProcessor parameterCollectorProcessor = new ParameterCollectorProcessor();
        parameterCollectorProcessor.process(sql);
        return !parameterCollectorProcessor.getNamedExpressions().isEmpty();
    }

    public ExecutableStatement withParams(Map<String, Object> params) {
        return withParams(DefaultExecutableParams.createParams(params));
    }

    public String toString() {
        return getSqlString();
    }
}

