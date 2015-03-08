package de.jexp.jequel.expression;

public class BooleanConstantExpression extends AbstractBooleanExpression {
    private final String literal;
    private final Boolean value;

    public BooleanConstantExpression() {
        this(null, true);
    }

    public BooleanConstantExpression(String literal, Boolean value) {
        this.literal = literal;
        this.value = value;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public Boolean getValue() {
        return value;
    }

    public String getLiteral() {
        return literal;
    }

    public boolean isAtomic() {
        return true;
    }
}