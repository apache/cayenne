package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public class StringPathExpression extends StringAbstractExpression implements PathExpression {
    private final String path;

    protected StringPathExpression(String path) {
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
