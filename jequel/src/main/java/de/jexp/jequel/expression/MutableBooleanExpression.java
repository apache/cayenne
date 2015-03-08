package de.jexp.jequel.expression;

public class MutableBooleanExpression extends AbstractBooleanExpression {
    private BooleanExpression expr;

    public MutableBooleanExpression() {
        this.expr = null;
    }

    public MutableBooleanExpression(BooleanExpression expr) {
        this.expr = expr;
    }

    public BooleanExpression and(BooleanExpression expression) {
        this.expr = hasValue() ? expr.and(expression) : expression;

        return this.expr;
    }

    public BooleanExpression or(BooleanExpression expression) {
        this.expr = hasValue() ? expr.or(expression) : expression;

        return this.expr;
    }

    public BooleanExpression getBooleanExpression() {
        return expr;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public boolean hasValue() {
        return expr != null;
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        if (hasValue()) {
            getBooleanExpression().process(expressionProcessor);
        }
    }

    public boolean isAtomic() {
        return false;
    }
}
