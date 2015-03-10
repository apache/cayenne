package de.jexp.jequel.expression;

import de.jexp.jequel.SqlString;
import de.jexp.jequel.Sql92Format;
import de.jexp.jequel.expression.logical.BooleanExpression;
import de.jexp.jequel.expression.logical.BooleanLiteral;
import de.jexp.jequel.expression.visitor.DelegatingExpressionFormat;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;
import de.jexp.jequel.literals.Operator;

public interface Expression extends SqlString, ListOperations, StringOperations, OrderingOperations {

    /* this expression format are used in expressins in toString method */
    DelegatingExpressionFormat EXPRESSION_FORMAT = new DelegatingExpressionFormat(new Sql92Format());

    BooleanExpression isNull();

    BooleanExpression isNotNull();

    <K> void process(ExpressionProcessor<K> expressionProcessor);

    <R> R accept(ExpressionVisitor<R> visitor);

}
