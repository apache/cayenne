/**
 * START_TAG_PLACEHOLDER FOREGROUND_PROCESSING RUNS_AS_JAVA_ON_CLIENT FINISH_TAG_PLACEHOLDER
 */
package de.jexp.jequel.expression;

public class StringLiteral extends StringAbstractExpression implements LiteralExpression<String> {
    private final String value;

    protected StringLiteral(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public String toString() {
        return EXPRESSION_FORMAT.visit(this);
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
