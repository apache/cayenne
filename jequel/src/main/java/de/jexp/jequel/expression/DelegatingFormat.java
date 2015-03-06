package de.jexp.jequel.expression;

import de.jexp.jequel.util.ClassUtils;

public class DelegatingFormat<T extends Format> implements Format {
    public static final String DEFAULT_SQL_FORMAT = "de.jexp.jequel.format.Sql92Format";

    private final T format;

    public DelegatingFormat() {
        this(DEFAULT_SQL_FORMAT);
    }

    public DelegatingFormat(String formatClassName) {
        format = ClassUtils.newInstance(formatClassName);
    }

    public T getFormat() {
        return format;
    }

    @Override
    public String formatAround(String expressionString, Expression expression) {
        return getFormat().formatAround(expressionString, expression);
    }
}