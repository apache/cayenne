package de.jexp.jequel.sql;

import de.jexp.jequel.Sql92Format;
import de.jexp.jequel.expression.AbstractExpression;
import de.jexp.jequel.expression.BooleanExpression;
import de.jexp.jequel.expression.DefaultExpressionsFactory;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;
import de.jexp.jequel.literals.SelectKeyword;
import de.jexp.jequel.sql.SqlDsl.GroupBy;
import de.jexp.jequel.sql.SqlDsl.OrderBy;
import de.jexp.jequel.sql.SqlModel.Having;
import de.jexp.jequel.sql.SqlModel.Select;
import de.jexp.jequel.sql.SqlModel.SelectPartColumnListExpression;
import de.jexp.jequel.sql.SqlModel.Where;
import de.jexp.jequel.table.Table;

import static java.util.Arrays.asList;

public class Sql extends AbstractExpression implements SqlDsl.Select, SqlDsl.From, SqlDsl.Where, OrderBy, GroupBy, SqlDsl.Having, Expression {

    public static final DefaultExpressionsFactory EXPRESSIONS_FACTORY = new DefaultExpressionsFactory();

    private final Select select = new Select();
    private final SqlModel.From from = new SqlModel.From();
    private final Where where = new Where();
    private final SelectPartColumnListExpression groupBy = new SelectPartColumnListExpression(SelectKeyword.GROUP_BY);
    private final Having having = new Having();
    private final SelectPartColumnListExpression orderBy = new SelectPartColumnListExpression(SelectKeyword.ORDER_BY);

    protected Sql(Expression... selectFields) {
        this.select.append(selectFields);
    }

    public Sql toSql() {
        return this;
    }

    public static SqlDsl.From Select(Table table) {
        Sql sql = new Sql();
        sql.factory(EXPRESSIONS_FACTORY);
        return sql.from(table);
    }

    public static SqlDsl.Select Select(Expression... fields) {
        Sql sql = new Sql(fields);
        sql.factory(EXPRESSIONS_FACTORY);

        return sql;
    }

    public static SqlDsl.Select subSelect(Expression... fields) {
        Sql sql = new Sql(fields);
        sql.factory(EXPRESSIONS_FACTORY);
        return sql;
    }

    public SqlDsl.From select(Expression... fields) {
        this.select.append(fields);
        return this;
    }

    @Override
    public SqlDsl.From from(SqlModel.FromSource... tables) {
        this.from.append(asList(tables));
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
        from.append(sql.from.getSources());
        where.and(sql.where.getBooleanExpression());
        groupBy.append(sql.groupBy.getExpressions());
        having.and(sql.having.getBooleanExpression());
        orderBy.append(sql.orderBy.getExpressions());
        return this;
    }

    public Select getSelect() {
        return select;
    }

    public SqlModel.From getFrom() {
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

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(factory().sql(this.accept(new Sql92Format()))); // TODO cyclic dependency!!!
    }
}

