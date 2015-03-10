package de.jexp.jequel.expression.visitor;

import de.jexp.jequel.expression.BinaryExpression;
import de.jexp.jequel.expression.logical.BooleanBinaryExpression;
import de.jexp.jequel.expression.logical.BooleanLiteral;
import de.jexp.jequel.expression.logical.BooleanUnaryExpression;
import de.jexp.jequel.expression.CompoundExpression;
import de.jexp.jequel.expression.ConstantExpression;
import de.jexp.jequel.expression.DelegatingFormat;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ExpressionAlias;
import de.jexp.jequel.expression.SearchCondition;
import de.jexp.jequel.expression.ParamExpression;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.expression.StringExpression;
import de.jexp.jequel.expression.UnaryExpression;
import de.jexp.jequel.expression.numeric.NumericBinaryExpression;
import de.jexp.jequel.expression.numeric.NumericLiteral;
import de.jexp.jequel.expression.numeric.NumericUnaryExpression;

public class DelegatingExpressionFormat extends DelegatingFormat<ExpressionFormat> implements ExpressionFormat {

    public DelegatingExpressionFormat(ExpressionFormat format) {
        super(format);
    }

    public <V> String visit(ConstantExpression<V> expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(NumericLiteral expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(BooleanLiteral expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(StringExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(UnaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(BooleanUnaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(NumericUnaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(BinaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(BooleanBinaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(NumericBinaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(CompoundExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(RowListExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public <T> String visit(ParamExpression<T> expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public <E extends Expression> String visit(ExpressionAlias<E> expression) {
        return formatAround(getFormat().visit(expression), expression);
    }
}
