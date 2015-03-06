package de.jexp.jequel.expression;

public interface ExpressionProcessor<T> {
    void process(Expression expression);

    T getResult();
}
