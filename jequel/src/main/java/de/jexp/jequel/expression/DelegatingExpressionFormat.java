package de.jexp.jequel.expression;

public class DelegatingExpressionFormat extends DelegatingFormat<ExpressionFormat> implements ExpressionFormat {

    public DelegatingExpressionFormat(ExpressionFormat format) {
        super(format);
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
