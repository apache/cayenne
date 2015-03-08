package de.jexp.jequel.expression;

import de.jexp.jequel.literals.Operator;

public class NumericBinaryExpression extends NumericExpression {
    private final BinaryExpression binaryExpression;

    public NumericBinaryExpression(Expression first, Operator operator, Expression second) {
        this.binaryExpression = new BinaryExpression(first, operator, second);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public BinaryExpression getBinaryExpression() {
        return binaryExpression;
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        expressionProcessor.process(getBinaryExpression());
    }
}