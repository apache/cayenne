package de.jexp.jequel.expression;

import de.jexp.jequel.literals.Operator;

public class BooleanBinaryExpression extends AbstractBooleanExpression {
    private final BinaryExpression binaryExpression;

    public BooleanBinaryExpression(Expression first, Operator operator, Expression second) {
        this.binaryExpression = new BinaryExpression(first, operator, second);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public BinaryExpression getBinaryExpression() {
        return binaryExpression;
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        expressionProcessor.process(getBinaryExpression());
    }

    public boolean isAtomic() {
        return false;
    }
}