package de.jexp.jequel.sql;

import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.expression.logical.BooleanExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.literals.SelectKeyword;

public class Sql extends RowListExpression implements SqlDsl.Select, SqlDsl.From, SqlDsl.Where, SqlDsl.OrderBy, SqlDsl.GroupBy, SqlDsl.Having {

    private final SqlModel.Select select = new SqlModel.Select();
    private final SqlModel.SelectPartColumnListExpression from = new SqlModel.SelectPartColumnListExpression(SelectKeyword.FROM);
    private final SqlModel.Where where = new SqlModel.Where();
    private final SqlModel.SelectPartColumnListExpression groupBy = new SqlModel.SelectPartColumnListExpression(SelectKeyword.GROUP_BY);
    private final SqlModel.Having having = new SqlModel.Having();
    private final SqlModel.SelectPartColumnListExpression orderBy = new SqlModel.SelectPartColumnListExpression(SelectKeyword.ORDER_BY);

    protected Sql(Expression... selectFields) {
        super(Delimeter.SPACE);

        this.select.append(selectFields);
        super.append(select, from, where, groupBy, having, orderBy);
    }

    public Sql toSql() {
        return this;
    }

    public static SqlDsl.Select Select(Expression... fields) {
        return new Sql(fields);
    }

    public SqlDsl.From select(Expression... fields) {
        this.select.append(fields);
        return this;
    }

    public static SqlDsl.Select subSelect(Expression... fields) {
        return new Sql(fields);
    }

    public SqlDsl.From from(RowListExpression... tables) {
        this.from.append(tables);
        return this;
    }

    public SqlDsl.Where where(BooleanExpression where) {
        this.where.and(where);
        return this;
    }

    public SqlDsl.OrderBy orderBy(Expression... orderBy) {
        this.orderBy.append(orderBy);
        return this;
    }

    public SqlDsl.GroupBy groupBy(Expression... groupBy) {
        this.groupBy.append(groupBy);
        return this;
    }

    public SqlDsl.Having having(BooleanExpression having) {
        this.having.and(having);
        return this;
    }

    public SqlModel.Where where() {
        return where;
    }

    public Sql append(SqlModel.Where where) {
        this.where.and(where.getBooleanExpression());

        return this;
    }

    public SqlModel.Having having() {
        return having;
    }

    public Sql append(SqlModel.Having having) {
        this.having.and(having.getBooleanExpression());

        return this;
    }

    public Sql append(Sql sql) {
        select.append(sql.select.getExpressions());
        from.append(sql.from.getExpressions());
        where.and(sql.where.getBooleanExpression());
        groupBy.append(sql.groupBy.getExpressions());
        having.and(sql.having.getBooleanExpression());
        orderBy.append(sql.orderBy.getExpressions());
        return this;
    }

    public SqlModel.Select getSelect() {
        return select;
    }

    public SqlModel.SelectPartColumnListExpression getFrom() {
        return from;
    }

    public SqlModel.Where getWhere() {
        return where;
    }

    public SqlModel.SelectPartColumnListExpression getGroupBy() {
        return groupBy;
    }

    public SqlModel.Having getHaving() {
        return having;
    }

    public SqlModel.SelectPartColumnListExpression getOrderBy() {
        return orderBy;
    }

}

