package de.jexp.jequel;

import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.visitor.ExpressionFormat;
import de.jexp.jequel.sql.Sql;

public class OracleSql extends Sql {
    private static final OracleSqlFormat ORACLE_SQL_FORMAT = new OracleSqlFormat();

    protected OracleSql(Expression... selectFields) {
        super(selectFields);
    }

    public String toString() {
        return ORACLE_SQL_FORMAT.visit(this);
    }
}