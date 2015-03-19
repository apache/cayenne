/**
 * START_TAG_PLACEHOLDER FOREGROUND_PROCESSING RUNS_AS_JAVA_ON_CLIENT FINISH_TAG_PLACEHOLDER
 */
package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.ExpressionVisitor;

/*
* TODO: Alex, split it to 2 classes with named and unnamed param
* */
public class ParamExpression<T> extends ConstantExpression<T> {

    public ParamExpression(String paramName) {
        super(paramName, null);
    }

    public ParamExpression(String paramName, T paramValue) {
        super(paramName, paramValue);
    }

    public ParamExpression(T paramValue) {
        super(null, paramValue);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public boolean isNamedExpression() {
        return getLiteral() != null;
    }
}
