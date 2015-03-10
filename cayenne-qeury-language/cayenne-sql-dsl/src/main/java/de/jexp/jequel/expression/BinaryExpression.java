package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.ExpressionVisitor;
import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.literals.SqlKeyword;

import java.util.Arrays;
import java.util.Collection;

public class BinaryExpression<T extends Expression> extends AbstractExpression {
    private final T first;
    private final Operator operator;
    private final T second;

    public BinaryExpression(T first, Operator operator, T second) {
        this.first = first;
        this.operator = operator;
        this.second = second;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> visitor) {
        first.accept(visitor);
        second.accept(visitor);

        return visitor.visit(this);
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        expressionProcessor.process(first);
        expressionProcessor.process(second);
    }

    public T getFirst() {
        return first;
    }

    public T getSecond() {
        return second;
    }

    public Operator getOperator() {
        return operator;
    }

    public boolean oneIsNull() {
        return first == Expressions.NULL || second == Expressions.NULL;
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