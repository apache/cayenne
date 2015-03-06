package de.jexp.jequel.expression;

public class DefaultExpressionAlias<E extends Expression> implements ExpressionAlias<E> {
    private final E aliased;
    private final String alias;

    public DefaultExpressionAlias(E aliased, String alias) {
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

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        aliased.process(expressionProcessor);
    }

    public boolean isParenthesed() {
        return false;
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

    public BooleanExpression between(Object start, Object end) {
        return aliased.between(start, end);
    }

    public NumericBinaryExpression plus(Object expression) {
        return aliased.plus(expression);
    }

    public NumericBinaryExpression minus(Object expression) {
        return aliased.minus(expression);
    }

    public NumericBinaryExpression times(Object expression) {
        return aliased.times(expression);
    }

    public NumericBinaryExpression by(Object expression) {
        return aliased.by(expression);
    }

    public BooleanExpression in(Object... expressions) {
        return aliased.in(expressions);
    }

    public BooleanExpression like(Object expression) {
        return aliased.like(expression);
    }

    public BooleanExpression is(Object expression) {
        return aliased.is(expression);
    }

    public BooleanExpression isNot(Object expression) {
        return aliased.isNot(expression);
    }

    public static DefaultExpressionAlias<Expression> as(AbstractExpression aliased, String alias) {
        return new DefaultExpressionAlias<Expression>(aliased, alias);
    }

    public static DefaultExpressionAlias<Expression> AS(AbstractExpression aliased, String alias) {
        return as(aliased, alias.toUpperCase());
    }

    public boolean isAtomic() {
        return aliased.isAtomic();
    }
}
