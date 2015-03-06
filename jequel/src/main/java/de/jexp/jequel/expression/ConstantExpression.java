package de.jexp.jequel.expression;

import de.jexp.jequel.Valueable;

public class ConstantExpression<V> extends AbstractExpression implements Valueable<V> {
    private final V value;
    private final String literal;

    public ConstantExpression(String literal, V value) {
        this.value = value;
        this.literal = literal;
    }

    public ConstantExpression(String literal) {
        this(literal, null);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public String getLiteral() {
        return literal;
    }

    public V getValue() {
        return value;
    }

    public boolean isAtomic() {
        return true;
    }
}
