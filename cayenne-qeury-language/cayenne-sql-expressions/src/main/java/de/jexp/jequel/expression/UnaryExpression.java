package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.ExpressionVisitor;
import de.jexp.jequel.literals.UnaryOperator;

public class UnaryExpression<E extends Expression> extends AbstractExpression {
    private final UnaryOperator operator;
    private final E exp;

    public UnaryExpression(UnaryOperator operator, E exp) {
        this.exp = exp;
        this.operator = operator;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        expressionProcessor.process(exp);
    }

    public UnaryOperator getOperator() {
        return operator;
    }

    public E getExpression() {
        return exp;
    }
}
