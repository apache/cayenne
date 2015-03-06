/**
 * START_TAG_PLACEHOLDER FOREGROUND_PROCESSING RUNS_AS_JAVA_ON_CLIENT FINISH_TAG_PLACEHOLDER
 */
package de.jexp.jequel.expression;

public class StringExpression extends ConstantExpression<String> {
    public StringExpression(String value) {
        super(null, value);
    }

    public String toString() {
        return EXPRESSION_FORMAT.visit(this);
    }

}
