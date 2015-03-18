package de.jexp.jequel.expression;


import de.jexp.jequel.expression.logical.BooleanExpression;
import de.jexp.jequel.sql.SqlDsl;

public interface ListOperations {
    BooleanExpression in(SqlDsl.ToSql subQuery);

    BooleanExpression in(Object... expressions);
}
