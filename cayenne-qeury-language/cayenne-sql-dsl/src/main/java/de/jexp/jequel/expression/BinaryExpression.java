package de.jexp.jequel.expression;

import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.literals.SqlKeyword;

import java.util.Arrays;
import java.util.Collection;

public class BinaryExpression extends AbstractExpression implements CompoundExpression {
    private final Expression first;
    private final Operator operator;
    private final Expression second;

    public BinaryExpression(Expression first, Operator operator, Expression second) {
        this.first = first;
        this.operator = operator;
        this.second = second;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        expressionProcessor.process(first);
        expressionProcessor.process(second);
    }

    public Expression getFirst() {
        return first;
    }

    public Expression getSecond() {
        return second;
    }

    public Operator getOperator() {
        return operator;
    }

    public boolean oneIsNull() {
        return first == Expressions.NULL || second == Expressions.NULL;
    }

    public boolean isAtomic() {
        return false;
    }

    public Collection<? extends Expression> getExpressions() {
        return Arrays.asList(first, second);
    }

    public boolean hasValues() {
        return true;
    }

    public SqlKeyword getDelimeter() {
        return operator;
    }
}