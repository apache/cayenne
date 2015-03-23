package de.jexp.jequel.expression;

import de.jexp.jequel.Sql92ExpressionVisitor;

public interface Expression extends ExpressionVisitable {

    Sql92ExpressionVisitor EXPRESSION_FORMAT = new Sql92ExpressionVisitor();

    BooleanExpression eq(Object expression);

    BooleanExpression ge(Object expression);

    BooleanExpression gt(Object expression);

    BooleanExpression lt(Object expression);

    BooleanExpression le(Object expression);

    BooleanExpression ne(Object expression);

    <E> BooleanExpression between(E start, E end);

    BooleanExpression isNull();

    BooleanExpression isNotNull();

    BooleanExpression in(Expression subQuery);

    BooleanExpression in(Object... expressions);

    /* TODO going to remove this method, visitor functionality should be enough */
    <K> void process(ExpressionProcessor<K> expressionProcessor);

    ExpressionsFactory factory();

}
