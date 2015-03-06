package de.jexp.jequel.expression;

/**
 * @author mh14 @ jexp.de
 * @since 05.11.2007 01:33:27 (c) 2007 jexp.de
 */
public class DelegatingExpressionFormat extends DelegatingFormat<ExpressionFormat> implements ExpressionFormat {
    public DelegatingExpressionFormat() {
    }

    public DelegatingExpressionFormat(String formatClassName) {
        super(formatClassName);
    }

    public <V> String visit(ConstantExpression<V> expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(NumericExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public String visit(BooleanConstantExpression expression) {
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

    public String visit(MutableBooleanExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    public <E extends Expression> String visit(ExpressionAlias<E> expression) {
        return formatAround(getFormat().visit(expression), expression);
    }
}
