package de.jexp.jequel.processor;

import de.jexp.jequel.expression.Expression;

public class DebugExpressionProcessor extends AbstractExpressionProcessor<String> {
    private final StringBuilder sb = new StringBuilder();

    public String getResult() {
        return sb.toString();
    }

    protected void doProcess(Expression<?> expression) {
        sb.append(String.format("%s {%s}\n", expression, expression.getClass().getSimpleName()));
    }
}