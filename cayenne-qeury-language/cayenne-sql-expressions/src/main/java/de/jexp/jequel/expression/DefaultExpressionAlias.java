package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public class DefaultExpressionAlias<E extends Expression> implements ExpressionAlias<E> {
    private final E aliased;
    private final String alias;

    protected DefaultExpressionAlias(E aliased, String alias) {
        this.aliased = aliased;
        this.alias = alias;
    }

    public E getAliased() {
        return aliased;
    }

    public String getAlias() {
        return alias;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    @Override
    public ExpressionsFactory factory() {
        return aliased.factory();
    }

    @Override
    public BooleanExpression isNull() {
        return aliased.isNull();
    }

    @Override
    public BooleanExpression isNotNull() {
        return aliased.isNotNull();
    }

    @Override
    public BooleanExpression in(Expression subQuery) {
        return aliased.in(subQuery);
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        aliased.process(expressionProcessor);
    }

    public BooleanExpression eq(Object expression) {
        return aliased.eq(expression);
    }

    public BooleanExpression ge(Object expression) {
        return aliased.ge(expression);
    }

    public BooleanExpression gt(Object expression) {
        return aliased.gt(expression);
    }

    public BooleanExpression lt(Object expression) {
        return aliased.lt(expression);
    }

    public BooleanExpression le(Object expression) {
        return aliased.le(expression);
    }

    public BooleanExpression ne(Object expression) {
        return aliased.ne(expression);
    }

    public <E> BooleanExpression between(E start, E end) {
        return aliased.between(start, end);
    }

    public BooleanExpression in(Object... expressions) {
        return aliased.in(expressions);
    }

}
