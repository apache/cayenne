package de.jexp.jequel.processor;

import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ExpressionProcessor;

public abstract class AbstractExpressionProcessor<T> implements ExpressionProcessor<T> {
    public void process(Expression expression) {
        doProcess(expression);
        expression.process(this);
    }

    protected abstract void doProcess(Expression expression);

    public abstract T getResult();
}
