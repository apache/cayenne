package de.jexp.jequel.expression.visitor;

import de.jexp.jequel.expression.Expression;

public interface Format {
    String formatAround(String expressionString, Expression expression);
}
