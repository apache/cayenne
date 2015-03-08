package de.jexp.jequel.expression;

public class NumericExpression extends AbstractExpression {
    private final Number value;

    public NumericExpression() {
        this(null);
    }

    public NumericExpression(Number value) {
        this.value = value;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public Number getValue() {
        return value;
    }

    public boolean isAtomic() {
        return true;
    }
}