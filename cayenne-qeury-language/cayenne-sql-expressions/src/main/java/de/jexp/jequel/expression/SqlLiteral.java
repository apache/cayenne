package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public class SqlLiteral extends AbstractExpression implements LiteralExpression<String> {

    private final String sql;

    protected SqlLiteral(String sql) {
        this.sql = sql;
    }

    @Override
    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    @Override
    public String getValue() {
        return sql;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
