package de.jexp.jequel.sql;

import de.jexp.jequel.expression.DelegatingFormat;

public class SqlExpressionFormat extends DelegatingFormat<SqlFormat> implements SqlFormat {
    public SqlExpressionFormat() {
    }

    public SqlExpressionFormat(String formatClassName) {
        super(formatClassName);
    }

    public String visit(SelectPartColumnListExpression sqlPartColumnTupleExpression) {
        return formatAround(getFormat().visit(sqlPartColumnTupleExpression), sqlPartColumnTupleExpression);
    }

    public String visit(SelectPartMutableBooleanExpression selectPartMutableBooleanExpression) {
        return formatAround(getFormat().visit(selectPartMutableBooleanExpression), selectPartMutableBooleanExpression);
    }
}