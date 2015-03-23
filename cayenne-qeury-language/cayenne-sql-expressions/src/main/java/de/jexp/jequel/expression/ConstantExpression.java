package de.jexp.jequel.expression;

public abstract class ConstantExpression<V> extends AbstractExpression implements LiteralExpression<V> {
    private final V value;
    private final String literal;

    protected ConstantExpression(String literal, V value) {
        this.value = value;
        this.literal = literal;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public String getLiteral() {
        return literal;
    }

    @Override
    public V getValue() {
        return value;
    }
}
