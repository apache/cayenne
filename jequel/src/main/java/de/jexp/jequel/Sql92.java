package de.jexp.jequel;

import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ExpressionFormat;
import de.jexp.jequel.sql.Sql;

public class Sql92 extends Sql {
    private static final ExpressionFormat SQL_92_FORMAT = new Sql92Format();

    protected Sql92(Expression... selectFields) {
        super(selectFields);
    }

    public String toString() {
        return SQL_92_FORMAT.visit(this);
    }
}
