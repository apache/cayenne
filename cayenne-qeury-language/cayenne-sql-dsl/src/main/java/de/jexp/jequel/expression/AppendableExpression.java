package de.jexp.jequel.expression;

public interface AppendableExpression<T extends Expression> {

    void append(T... expressions);
}
