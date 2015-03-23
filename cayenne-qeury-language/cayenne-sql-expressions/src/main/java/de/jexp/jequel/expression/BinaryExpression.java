package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.ExpressionVisitor;
import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.literals.SqlKeyword;

import java.util.Arrays;
import java.util.Collection;

public class BinaryExpression<E extends Expression> extends AbstractExpression {
    private final E first;
    private final Operator operator;
    private final E second;

    protected BinaryExpression(E first, Operator operator, E second) {
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

    public E getFirst() {
        return first;
    }

    public E getSecond() {
        return second;
    }

    public Operator getOperator() {
        return operator;
    }

    public boolean oneIsNull() {
        return first == factory().boolNull() || second == factory().boolNull();
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