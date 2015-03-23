package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public class VariableNumericLiteralExpression extends NumericAbstractExpression implements VariableExpression {
    private final String path;

    protected VariableNumericLiteralExpression(String path) {
        this.path = path;
    }

    @Override
    public String getValue() {
        return path;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
