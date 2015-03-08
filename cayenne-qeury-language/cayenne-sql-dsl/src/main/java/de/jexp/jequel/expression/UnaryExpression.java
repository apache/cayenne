package de.jexp.jequel.expression;

import de.jexp.jequel.literals.UnaryOperator;

public class UnaryExpression extends AbstractExpression {
    private final UnaryOperator operator;
    private final Expression first;

    public UnaryExpression(UnaryOperator operator, Expression first) {
        this.first = first;
        this.operator = operator;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        expressionProcessor.process(first);
    }

    public UnaryOperator getOperator() {
        return operator;
    }

    public Expression getFirst() {
        return first;
    }

    public boolean isAtomic() {
        return false;
    }
}
