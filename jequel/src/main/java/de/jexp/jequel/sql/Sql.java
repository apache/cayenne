package de.jexp.jequel.sql;

import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.expression.BooleanExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.literals.SelectKeyword;

public class Sql extends RowListExpression implements Dsl.Select, Dsl.From, Dsl.Where, Dsl.OrderBy, Dsl.GroupBy, Dsl.Having {

    private final DslSqlModel.SelectPartColumnListExpression select = new DslSqlModel.SelectPartColumnListExpression(SelectKeyword.SELECT);
    private final DslSqlModel.SelectPartColumnListExpression from = new DslSqlModel.SelectPartColumnListExpression(SelectKeyword.FROM);
    private final DslSqlModel.Where where = new DslSqlModel.Where();
    private final DslSqlModel.SelectPartColumnListExpression groupBy = new DslSqlModel.SelectPartColumnListExpression(SelectKeyword.GROUP_BY);
    private final DslSqlModel.Having having = new DslSqlModel.Having();
    private final DslSqlModel.SelectPartColumnListExpression orderBy = new DslSqlModel.SelectPartColumnListExpression(SelectKeyword.ORDER_BY);

    protected Sql(Expression... selectFields) {
        super(Delimeter.SPACE);

        this.select.append(selectFields);
        super.append(select, from, where, groupBy, having, orderBy);
    }

    public Sql toSql() {
        return this;
    }

    public static Dsl.Select Select(Expression... fields) {
        return new Sql(fields);
    }

    public Dsl.From select(Expression... fields) {
        this.select.append(fields);
        return this;
    }

    public static Dsl.Select subSelect(Expression... fields) {
        return new Sql(fields) {
            public boolean isParenthesed() {
                return true;
            }
        };
    }

    public Dsl.From from(RowListExpression... tables) {
        this.from.append(tables);
        return this;
    }

    public Dsl.Where where(BooleanExpression where) {
        this.where.and(where);
        return this;
    }

    public Dsl.OrderBy orderBy(Expression... orderBy) {
        this.orderBy.append(orderBy);
        return this;
    }

    public Dsl.GroupBy groupBy(Expression... groupBy) {
        this.groupBy.append(groupBy);
        return this;
    }

    public Dsl.Having having(BooleanExpression having) {
        this.having.and(having);
        return this;
    }

    public boolean isParenthesed() {
        return false;
    }

    public DslSqlModel.Where where() {
        return where;
    }

    public Sql append(DslSqlModel.Where where) {
        this.where.and(where.getBooleanExpression());

        return this;
    }

    public DslSqlModel.Having having() {
        return having;
    }

    public Sql append(DslSqlModel.Having having) {
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

    public DslSqlModel.SelectPartColumnListExpression getSelect() {
        return select;
    }

    public DslSqlModel.SelectPartColumnListExpression getFrom() {
        return from;
    }

    public DslSqlModel.Where getWhere() {
        return where;
    }

    public DslSqlModel.SelectPartColumnListExpression getGroupBy() {
        return groupBy;
    }

    public DslSqlModel.Having getHaving() {
        return having;
    }

    public DslSqlModel.SelectPartColumnListExpression getOrderBy() {
        return orderBy;
    }

}

