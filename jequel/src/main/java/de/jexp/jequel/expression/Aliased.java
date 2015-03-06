package de.jexp.jequel.expression;

public interface Aliased<T> {
    T getAliased();

    String getAlias();
}