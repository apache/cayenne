package de.jexp.jequel.expression;

public class NumericPathExpression extends NumericAbstractExpression implements PathExpression {
    private final String path;

    protected NumericPathExpression(String path) {
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
