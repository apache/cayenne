package de.jexp.jequel.sql;

import de.jexp.jequel.expression.AppendableExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.literals.SelectKeyword;

public interface SelectPartExpression<T extends Expression> extends AppendableExpression<T> {
    SqlExpressionFormat SQL_FORMAT = new SqlExpressionFormat();

    SelectKeyword getSelectKeyword();
}
