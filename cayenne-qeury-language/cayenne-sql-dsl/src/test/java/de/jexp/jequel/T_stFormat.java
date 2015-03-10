package de.jexp.jequel;

import de.jexp.jequel.expression.BinaryExpression;
import de.jexp.jequel.expression.logical.BooleanBinaryExpression;
import de.jexp.jequel.expression.logical.BooleanLiteral;
import de.jexp.jequel.expression.logical.BooleanUnaryExpression;
import de.jexp.jequel.expression.CompoundExpression;
import de.jexp.jequel.expression.ConstantExpression;
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
import de.jexp.jequel.sql.SqlModel;
import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;
import de.jexp.jequel.table.JoinTable;

public class T_stFormat extends Sql92Format {
    private final String testString = " test ";

    public <V> String visit(ConstantExpression<V> constantExpression) {
        return testString;
    }

    public String visit(NumericLiteral numericLiteral) {
        return testString;
    }

    public String visit(BooleanLiteral bool) {
        return testString;
    }

    public String visit(StringExpression stringExpression) {
        return testString;
    }

    public String visit(UnaryExpression unaryExpression) {
        return testString;
    }

    public String visit(BooleanUnaryExpression booleanUnaryExpression) {
        return testString;
    }

    public String visit(NumericUnaryExpression numericUnaryExpression) {
        return testString;
    }

    public String visit(BinaryExpression binaryExpression) {
        return testString;
    }

    public String visit(BooleanBinaryExpression binaryExpression) {
        return testString;
    }

    public String visit(NumericBinaryExpression binaryExpression) {
        return testString;
    }

    public String visit(CompoundExpression listExpression) {
        return super.visit(listExpression);
    }

    public String visit(RowListExpression rowTupleExpression) {
        return super.visit(rowTupleExpression);
    }

    public <T> String visit(ParamExpression<T> paramExpression) {
        return testString;
    }

    public <E extends Expression> String visit(ExpressionAlias<E> expression) {
        return testString;
    }

    public String formatAround(String expressionString, Expression expression) {
        return testString;
    }

    public String visit(SqlModel.SelectPartColumnListExpression sqlPartColumnTupleExpression) {
        return testString;
    }

    public String visit(SqlModel.Where where) {
        return testString;
    }

    public String visit(SqlModel.Having having) {
        return testString;
    }

    public <T> String visit(Field<T> field) {
        return testString;
    }

    public String visit(JoinTable joinTable) {
        return testString;
    }

    public String visit(BaseTable table) {
        return testString;
    }
}
