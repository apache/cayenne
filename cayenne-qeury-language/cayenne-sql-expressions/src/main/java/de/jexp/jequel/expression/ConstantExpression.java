package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public class ConstantExpression<V> extends AbstractExpression implements LiteralExpression<V> {
    private final V value;
    private final String literal;

    protected ConstantExpression(String literal, V value) {
        this.value = value;
        this.literal = literal;
    }

    protected ConstantExpression(String literal) {
        this(literal, null);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public String getLiteral() {
        return literal;
    }

    @Override
    public V getValue() {
        return value;
    }
}
