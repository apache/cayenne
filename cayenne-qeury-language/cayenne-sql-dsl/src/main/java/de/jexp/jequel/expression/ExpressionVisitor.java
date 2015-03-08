package de.jexp.jequel.expression;

public interface ExpressionVisitor<R> {
    <V> R visit(ConstantExpression<V> constantExpression);

    R visit(NumericExpression numericExpression);

    R visit(BooleanConstantExpression booleanConstantExpression);

    R visit(StringExpression stringExpression);

    R visit(UnaryExpression unaryExpression);

    R visit(BooleanUnaryExpression booleanUnaryExpression);

    R visit(NumericUnaryExpression numericUnaryExpression);

    R visit(BinaryExpression binaryExpression);

    R visit(BooleanBinaryExpression binaryExpression);

    R visit(NumericBinaryExpression binaryExpression);

    R visit(CompoundExpression listExpression);

    R visit(RowListExpression rowTupleExpression);

    <T> R visit(ParamExpression<T> paramExpression);

    R visit(MutableBooleanExpression mutableBooleanExpression);

    <E extends Expression> R visit(ExpressionAlias<E> expression);
}
