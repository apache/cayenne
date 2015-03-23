package de.jexp.jequel.expression;

public interface ExpressionVisitable {

    <R> R accept(ExpressionVisitor<R> visitor);

}
