package de.jexp.jequel.defaults;

import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ExpressionFormat;
import de.jexp.jequel.format.OracleSqlFormat;
import de.jexp.jequel.sql.Sql;

public class OracleSql extends Sql {
    private static final ExpressionFormat ORACLE_SQL_FORMAT = new OracleSqlFormat();

    protected OracleSql(Expression... selectFields) {
        super(selectFields);
    }

    public String toString() {
        return ORACLE_SQL_FORMAT.visit(this);
    }
}