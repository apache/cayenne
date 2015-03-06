package de.jexp.jequel.expression;

public interface Alias<T extends Expression> {
    T as(String alias);
}
