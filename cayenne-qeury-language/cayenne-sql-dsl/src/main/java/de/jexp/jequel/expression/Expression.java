package de.jexp.jequel.expression;

import de.jexp.jequel.SqlString;
import de.jexp.jequel.Sql92Format;

public interface Expression extends SqlString, NumericOperations, ListOperations, StringOperations, IsOperations {

    /* this expression format are used in expressins in toString method */
    DelegatingExpressionFormat EXPRESSION_FORMAT = new DelegatingExpressionFormat(new Sql92Format());

    <K> void process(ExpressionProcessor<K> expressionProcessor);

    boolean isParenthesed();

    boolean isAtomic();

    <R> R accept(ExpressionVisitor<R> expressionVisitor);

}
