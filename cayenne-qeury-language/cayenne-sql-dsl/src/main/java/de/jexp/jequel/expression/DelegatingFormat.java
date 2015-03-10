package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.Format;

public class DelegatingFormat<T extends Format> implements Format {
    private final T format;

    public DelegatingFormat(T format) {
        this.format = format;
    }

    public T getFormat() {
        return format;
    }

    @Override
    public String formatAround(String expressionString, Expression expression) {
        return getFormat().formatAround(expressionString, expression);
    }
}