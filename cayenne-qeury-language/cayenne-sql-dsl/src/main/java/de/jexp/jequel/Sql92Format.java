package de.jexp.jequel;

import de.jexp.jequel.expression.CompoundExpression;
import de.jexp.jequel.expression.ConstantExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ParamExpression;
import de.jexp.jequel.expression.SearchCondition;
import de.jexp.jequel.expression.SimpleListExpression;
import de.jexp.jequel.expression.StringExpression;
import de.jexp.jequel.expression.UnaryExpression;
import de.jexp.jequel.expression.logical.BooleanBinaryExpression;
import de.jexp.jequel.expression.logical.BooleanLiteral;
import de.jexp.jequel.expression.logical.BooleanUnaryExpression;
import de.jexp.jequel.expression.numeric.NumericLiteral;
import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.SelectKeyword;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.sql.SqlModel;
import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;
import de.jexp.jequel.table.ForeignKey;
import de.jexp.jequel.table.JoinTable;
import de.jexp.jequel.table.TableField;

public class Sql92Format extends Sql92ExpressionFormatter implements SqlModel.SqlFormat {

    public String formatAround(String expressionString, Expression expression) {
        return expressionString;
    }

    protected <T> String visit(T constantValue) {
        String valueString = formatConcrete(constantValue);
        if (constantValue instanceof Expression) {
            return formatAround(valueString, (Expression) constantValue);
        }
        return valueString;
    }

    protected String formatConcrete(Object value) {
        if (value instanceof BooleanBinaryExpression) return visit((BooleanBinaryExpression) value);
        if (value instanceof BooleanUnaryExpression) return visit((BooleanUnaryExpression) value);
        if (value instanceof Field) return visit((Field) value);
        if (value instanceof ForeignKey) return visit((ForeignKey) value);
        if (value instanceof BaseTable) return visit((BaseTable) value);
        if (value instanceof NumericLiteral) return visit((NumericLiteral) value);
        if (value instanceof ParamExpression) return visit((ParamExpression) value);
        if (value instanceof SqlModel.SelectPartColumnListExpression) return visit((SqlModel.SelectPartColumnListExpression) value);
        if (value instanceof SqlModel.Select) return visit((SqlModel.Select) value);
        if (value instanceof SqlModel.Where) return visit((SqlModel.Where) value);
        if (value instanceof SqlModel.Having) return visit((SqlModel.Having) value);
        if (value instanceof Sql) return "(" + visit((Sql) value) + ")";
        if (value instanceof StringExpression) return visit((StringExpression) value);
        if (value instanceof TableField) return visit((TableField) value);
        if (value instanceof UnaryExpression) return visit((UnaryExpression) value);
        if (value instanceof CompoundExpression) return visit((CompoundExpression) value);
        if (value instanceof ConstantExpression) return visit((ConstantExpression) value);
        return value.toString();
    }

    protected String visitIn(Expression first, Expression second) {
        if (second instanceof Sql) {
            return first.accept(this) + " in " + second.accept(this);
        }

        return super.visitIn(first, second);
    }

    public String visit(SqlModel.SelectPartColumnListExpression sqlPartColumnTupleExpression) {
        if (sqlPartColumnTupleExpression.hasValues()) {
            return visit(sqlPartColumnTupleExpression.getSelectKeyword()) + " " + visit((SimpleListExpression) sqlPartColumnTupleExpression);
        }

        return "";
    }

    public String visitSearchCondition(SelectKeyword keyword, SearchCondition searchCondition) {
        if (!searchCondition.getBooleanExpression().equals(BooleanLiteral.NULL)) {
            return visit(keyword) + " " + searchCondition.getBooleanExpression().accept(this);
        } else {
            return "";
        }
    }

    public String visit(SqlModel.Select select) {
        if (!select.hasValues()) {
            return "select *";
        }
        return "select " + visit((SimpleListExpression) select);
    }

    public String visit(SqlModel.Where where) {
        return visitSearchCondition(SelectKeyword.WHERE, where);
    }

    public String visit(SqlModel.Having having) {
        return visitSearchCondition(SelectKeyword.HAVING, having);
    }

    public String visit(JoinTable joinTable) {
        return parenthese(
                formatAround(visit(joinTable.getFirst()), joinTable.getFirst())
                        + " join " +
                        formatAround(visit(joinTable.getSecond()), joinTable.getSecond())
                        + " on " +
                        parenthese(visit(joinTable.getJoinExpression())));
    }

    public String visit(BaseTable table) {
        if (table instanceof JoinTable) {
            return visit((JoinTable) table);
        }

        return table.getName();
    }

    public <T> String visit(Field<T> field) {
        return field.getTableName() + Delimeter.POINT + field.getName();
    }
}
