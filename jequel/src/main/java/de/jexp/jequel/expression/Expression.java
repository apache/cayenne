package de.jexp.jequel.expression;

import de.jexp.jequel.SqlString;

public interface Expression extends SqlString, NumericOperations, ListOperations, StringOperations, IsOperations {
    DelegatingExpressionFormat EXPRESSION_FORMAT = new DelegatingExpressionFormat();

    <K> void process(ExpressionProcessor<K> expressionProcessor);

    boolean isParenthesed();

    boolean isAtomic();

    <R> R accept(ExpressionVisitor<R> expressionVisitor);

}
