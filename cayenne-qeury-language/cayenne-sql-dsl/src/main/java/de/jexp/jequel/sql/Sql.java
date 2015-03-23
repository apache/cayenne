package de.jexp.jequel.sql;

import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.expression.BooleanExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.literals.SelectKeyword;
import de.jexp.jequel.sql.SqlDsl.From;
import de.jexp.jequel.sql.SqlDsl.GroupBy;
import de.jexp.jequel.sql.SqlDsl.OrderBy;
import de.jexp.jequel.sql.SqlModel.Having;
import de.jexp.jequel.sql.SqlModel.Select;
import de.jexp.jequel.sql.SqlModel.SelectPartColumnListExpression;
import de.jexp.jequel.sql.SqlModel.Where;
import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Table;

public class Sql extends RowListExpression implements SqlDsl.Select, From, SqlDsl.Where, OrderBy, GroupBy, SqlDsl.Having {

    private final Select select = new Select();
    private final SelectPartColumnListExpression from = new SelectPartColumnListExpression(SelectKeyword.FROM);
    private final Where where = new Where();
    private final SelectPartColumnListExpression groupBy = new SelectPartColumnListExpression(SelectKeyword.GROUP_BY);
    private final Having having = new Having();
    private final SelectPartColumnListExpression orderBy = new SelectPartColumnListExpression(SelectKeyword.ORDER_BY);

    protected Sql(Expression... selectFields) {
        super(Delimeter.SPACE);

        this.select.append(selectFields);
        super.append(select, from, where, groupBy, having, orderBy);
    }

    public Sql toSql() {
        return this;
    }

    public static From Select(RowListExpression table) {
        return new Sql().from(table);
    }

    public static SqlDsl.Select Select(Expression... fields) {
        return new Sql(fields);
    }

    public From select(Expression... fields) {
        this.select.append(fields);
        return this;
    }

    public static SqlDsl.Select subSelect(Expression... fields) {
        return new Sql(fields);
    }

    @Override
    public From from(RowListExpression... tables) {
        this.from.append(tables);
        return this;
    }

    @Override
    public SqlDsl.Where where(BooleanExpression where) {
        this.where.and(where);
        return this;
    }

    public OrderBy orderBy(Expression... orderBy) {
        this.orderBy.append(orderBy);
        return this;
    }

    public GroupBy groupBy(Expression... groupBy) {
        this.groupBy.append(groupBy);
        return this;
    }

    public SqlDsl.Having having(BooleanExpression having) {
        this.having.and(having);
        return this;
    }

    public Where where() {
        return where;
    }

    public Sql append(Where where) {
        this.where.and(where.getBooleanExpression());

        return this;
    }

    public Having having() {
        return having;
    }

    public Sql append(Having having) {
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

    public Select getSelect() {
        return select;
    }

    public SelectPartColumnListExpression getFrom() {
        return from;
    }

    public Where getWhere() {
        return where;
    }

    public SelectPartColumnListExpression getGroupBy() {
        return groupBy;
    }

    public Having getHaving() {
        return having;
    }

    public SelectPartColumnListExpression getOrderBy() {
        return orderBy;
    }

    @Override
    public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
        return sqlVisitor.visit(this);
    }
}

