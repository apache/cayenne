package de.jexp.jequel;

import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.SearchCondition;
import de.jexp.jequel.expression.SimpleListExpression;
import de.jexp.jequel.expression.BooleanLiteral;
import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.SelectKeyword;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.sql.SqlModel.From;
import de.jexp.jequel.sql.SqlModel.Having;
import de.jexp.jequel.sql.SqlModel.Select;
import de.jexp.jequel.sql.SqlModel.SelectPartColumnListExpression;
import de.jexp.jequel.sql.SqlDsl.SqlFormat;
import de.jexp.jequel.sql.SqlModel.Where;
import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;
import de.jexp.jequel.table.JoinTable;

public class Sql92Format extends Sql92ExpressionFormatter implements SqlFormat {

    public String formatAround(String expressionString, Expression expression) {
        return expressionString;
    }

    protected <T> String visit(T constantValue) {
        return formatAround(constantValue.toString(), (Expression) constantValue);
    }

/*    protected String formatConcrete(Object value) {
        if (value instanceof BooleanBinaryExpression) return visit((BooleanBinaryExpression) value);
        if (value instanceof BooleanUnaryExpression) return visit((BooleanUnaryExpression) value);
        if (value instanceof Field) return visit((Field) value);
        if (value instanceof ForeignKey) return visit((ForeignKey) value);
        if (value instanceof BaseTable) return visit((BaseTable) value);
        if (value instanceof NumericLiteral) return visit((NumericLiteral) value);
        if (value instanceof ParamExpression) return visit((ParamExpression) value);
        if (value instanceof SelectPartColumnListExpression) return visit((SelectPartColumnListExpression) value);
        if (value instanceof Select) return visit((Select) value);
        if (value instanceof Where) return visit((Where) value);
        if (value instanceof Having) return visit((Having) value);
        if (value instanceof Sql) return "(" + visit((Sql) value) + ")";
        if (value instanceof StringLiteral) return visit((StringLiteral) value);
        if (value instanceof TableField) return visit((TableField) value);
        if (value instanceof UnaryExpression) return visit((UnaryExpression) value);
        if (value instanceof CompoundExpression) return visit((CompoundExpression) value);
        if (value instanceof ConstantExpression) return visit((ConstantExpression) value);
        return value.toString();
    }*/

    protected String visitIn(Expression first, Expression second) {
        if (second instanceof Sql) {
            return first.accept(this) + " in " + second.accept(this);
        }

        return super.visitIn(first, second);
    }

    public String visit(SelectPartColumnListExpression sqlPartColumnTupleExpression) {
        if (sqlPartColumnTupleExpression.hasValues()) {
            return " " + visit(sqlPartColumnTupleExpression.getSelectKeyword()) + " " + visit((SimpleListExpression) sqlPartColumnTupleExpression);
        }

        return "";
    }

    public String visitSearchCondition(SelectKeyword keyword, SearchCondition searchCondition) {
        if (searchCondition.getBooleanExpression().equals(BooleanLiteral.NULL)) {
            return "";
        }

        return " " + visit(keyword) + " " + searchCondition.getBooleanExpression().accept(this);
    }

    public String visit(Select select) {
        if (!select.hasValues()) {
            return "select *";
        }
        return "select " + visit((SimpleListExpression) select);
    }

    public String visit(Where where) {
        return visitSearchCondition(SelectKeyword.WHERE, where);
    }

    public String visit(Having having) {
        return visitSearchCondition(SelectKeyword.HAVING, having);
    }

    @Override
    public String visit(From from) {
        StringBuilder res = new StringBuilder("from ");
        for (Expression expression : from.getExpressions()) {
            res.append(expression.accept(this));
        }
        return res.toString();
    }

    public String visit(JoinTable joinTable) {
        return parenthese(
                formatAround(visitBaseTable(joinTable.getFirst()), joinTable.getFirst())
                        + " join " +
                        formatAround(visitBaseTable(joinTable.getSecond()), joinTable.getSecond())
                        + " on " +
                        parenthese(visit(joinTable.getJoinExpression())));
    }

    public String visitBaseTable(BaseTable table) {
        if (table instanceof JoinTable) {
            return visit((JoinTable) table);
        }

        return table.getName();
    }

    public <T> String visit(Field<T> field) {
        return field.getTableName() + Delimeter.POINT + field.getName();
    }

    @Override
    public String visit(Sql sql) {
        String res = "";
        res += visit(sql.getSelect());
        res += visit(sql.getFrom());
        res += visit(sql.getWhere());
        res += visit(sql.getGroupBy());
        res += visit(sql.getHaving());
        res += visit(sql.getOrderBy());
        return res;
    }
}
