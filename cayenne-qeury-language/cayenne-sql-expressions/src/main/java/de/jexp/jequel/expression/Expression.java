package de.jexp.jequel.expression;

import de.jexp.jequel.Sql92ExpressionFormatter;
import de.jexp.jequel.expression.visitor.DelegatingExpressionFormat;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public interface Expression {

    /* this expression format are used in expressins in toString method */
    DelegatingExpressionFormat EXPRESSION_FORMAT = new DelegatingExpressionFormat(new Sql92ExpressionFormatter());

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

    <R> R accept(ExpressionVisitor<R> visitor);

    ExpressionsFactory factory();

}
