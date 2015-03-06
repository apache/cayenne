package de.jexp.jequel.sql;

import de.jexp.jequel.Delimeter;
import de.jexp.jequel.execute.ExecutableStatement;
import de.jexp.jequel.expression.BooleanExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.literals.SelectKeyword;
import de.jexp.jequel.sql.dsl.DSL;

import static de.jexp.jequel.expression.Expressions.*;

import javax.sql.DataSource;
import java.util.Collection;

public class Sql extends RowListExpression implements DSL.From, Where, DSL.OrderBy, DSL.Having {

    private static final ExecutableStatementFactory EXECUTABLE_STATEMENT_FACTORY = new DelegatingExecutableStatementFactory();

    private final SelectPartColumnListExpression select = new SelectPartColumnListExpression(SelectKeyword.SELECT);
    private final SelectPartColumnListExpression from = new SelectPartColumnListExpression(SelectKeyword.FROM);
    private final SelectPartMutableBooleanExpression where = new SelectPartMutableBooleanExpression(SelectKeyword.WHERE);
    private final SelectPartColumnListExpression groupBy = new SelectPartColumnListExpression(SelectKeyword.GROUP_BY);
    private final SelectPartMutableBooleanExpression having = new SelectPartMutableBooleanExpression(SelectKeyword.HAVING);
    private final SelectPartColumnListExpression orderBy = new SelectPartColumnListExpression(SelectKeyword.ORDER_BY);

    protected Sql(Expression... selectFields) {
        super(Delimeter.SPACE);

        this.select.append(selectFields);
        super.append(toCollection(select, from, where, groupBy, having, orderBy));
    }

    public Sql toSql() {
        return this;
    }

    public ExecutableStatement executeOn(DataSource dataSource) {
        return EXECUTABLE_STATEMENT_FACTORY.createExecutableStatement(dataSource, this);
    }

    public static DSL.From Select(Expression... fields) {
        return new Sql(fields);
    }

    public DSL.From select(Expression... fields) {
        this.select.append(fields);
        return this;
    }

    public static DSL.From subSelect(Expression... fields) {
        return new Sql(fields) {
            public boolean isParenthesed() {
                return true;
            }
        };
    }

    public Where from(RowListExpression... tables) {
        this.from.append(tables);
        return this;
    }


    public DSL.OrderBy where(BooleanExpression where) {
        this.where.and(where);
        return this;
    }

    public DSL.GroupBy orderBy(Expression... orderBy) {
        this.orderBy.append(orderBy);
        return this;
    }

    public DSL.Having groupBy(Expression... groupBy) {
        this.groupBy.append(groupBy);
        return this;
    }

    public Expression having(BooleanExpression having) {
        this.having.and(having);
        return this;
    }

    public boolean isParenthesed() {
        return false;
    }

    public SelectPartMutableBooleanExpression where() {
        return where;
    }

    public SelectPartMutableBooleanExpression having() {
        return having;
    }

    // Allow Append of Expressions

    public Sql append(Sql sql) {
        select.append(sql.select.getExpressions());
        from.append(sql.from.getExpressions());
        where.and(sql.where.getBooleanExpression());
        groupBy.append(sql.groupBy.getExpressions());
        having.and(sql.having.getBooleanExpression());
        orderBy.append(sql.orderBy.getExpressions());
        return this;
    }

    public void append(Expression... expressions) {
        for (Expression expression : expressions) {
            appendExpression(expression);
        }
    }

    public void append(Collection<? extends Expression> expressions) {
        for (Expression expression : expressions) {
            appendExpression(expression);
        }
    }

    private void appendExpression(Expression expression) {
        if (expression instanceof SelectPartColumnListExpression) {
            SelectPartColumnListExpression selectPartExpression = (SelectPartColumnListExpression) expression;
            Collection<Expression> expressions = selectPartExpression.getExpressions();
            switch (selectPartExpression.getSelectKeyword()) {
                case SELECT:
                    this.select.append(expressions);
                    break;
                case FROM:
                    this.from.append(expressions);
                    break;
                case GROUP_BY:
                    this.groupBy.append(expressions);
                    break;
                case ORDER_BY:
                    this.orderBy.append(expressions);
                    break;
            }
        } else if (expression instanceof SelectPartMutableBooleanExpression) {
            SelectPartMutableBooleanExpression selectPartExpression = (SelectPartMutableBooleanExpression) expression;
            BooleanExpression booleanExpression = selectPartExpression.getBooleanExpression();

            switch (selectPartExpression.getSelectKeyword()) {
                case WHERE:
                    this.where.and(booleanExpression);
                    break;
                case HAVING:
                    this.having.and(booleanExpression);
                    break;
            }
        }
    }

    public SelectPartColumnListExpression getSelect() {
        return select;
    }

    public SelectPartColumnListExpression getFrom() {
        return from;
    }

    public SelectPartMutableBooleanExpression getWhere() {
        return where;
    }

    public SelectPartColumnListExpression getGroupBy() {
        return groupBy;
    }

    public SelectPartMutableBooleanExpression getHaving() {
        return having;
    }

    public SelectPartColumnListExpression getOrderBy() {
        return orderBy;
    }
}

