package de.jexp.jequel.expression;

import de.jexp.jequel.literals.UnaryOperator;

public class NumericUnaryExpression extends NumericExpression {
    private final UnaryExpression unaryExpression;

    public NumericUnaryExpression(UnaryOperator operator, Expression first) {
        this.unaryExpression = new UnaryExpression(operator, first);
    }

    public String toString() {
        return EXPRESSION_FORMAT.visit(this);
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        expressionProcessor.process(getUnaryExpression());
    }

    public UnaryExpression getUnaryExpression() {
        return unaryExpression;
    }

    public boolean isAtomic() {
        return false;
    }
}
