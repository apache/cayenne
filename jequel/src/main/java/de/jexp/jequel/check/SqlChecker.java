package de.jexp.jequel.check;

import de.jexp.jequel.expression.Aliased;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ExpressionProcessor;
import de.jexp.jequel.expression.UnaryExpression;
import de.jexp.jequel.processor.AbstractExpressionProcessor;
import de.jexp.jequel.sql.DslSqlModel;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.table.Field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * define Rules as Checks, Rules can be validated
 */
public class SqlChecker {
    private final Sql sql;

    public SqlChecker(Sql sql) {
        this.sql = sql;
    }

    public TableUsageCheckResult checkUsedTables() {
        TableUsageCheckResult checkResult = new TableUsageCheckResult(getFromSymbols());
        checkResult.addUsedTables(getUsedTables());
        return checkResult;
    }

    // group by, check fields in having
    // check usage in select only with NumericUnaryExpressions

    public Collection<String> checkGroupBy() {
        Collection<String> groupByExpressions = getGroupByExpressions();
        Collection<String> ungroupedSelectExpressions = new LinkedList<String>();
        for (Expression anSelectExpression : sql.getSelect().getExpressions()) {
            Object selectExpression = anSelectExpression instanceof Aliased ? ((Aliased) anSelectExpression).getAliased() : anSelectExpression;
            String selectExpressionString = selectExpression.toString();
            if (!groupByExpressions.contains(selectExpressionString)) {
                // either numeric, todo shouldnt contain group by expression
                if (!(selectExpression instanceof UnaryExpression) &&
                        !(selectExpression instanceof Sql)) {
                    ungroupedSelectExpressions.add(selectExpressionString);
                }
            }
        }
        sql.getHaving();
        // TODO Having
        return ungroupedSelectExpressions;
    }

    public Collection<String> getGroupByExpressions() {
        Collection<String> groupByExpressions = new LinkedList<String>();
        for (Expression groupByExpression : sql.getGroupBy().getExpressions()) {
            groupByExpressions.add(groupByExpression.toString());
        }
        return groupByExpressions;
    }

    public Collection<String> getFromSymbols() {
        return getSymbols(sql.getFrom());
    }

    public Collection<String> getUsedTables() {
        TableNameCollector collector = new TableNameCollector();
        collector.process(sql.getSelect());
        collector.process(sql.getWhere());
        collector.process(sql.getOrderBy());
        collector.process(sql.getGroupBy());
        collector.process(sql.getHaving());
        return collector.getResult();
    }

    public static Collection<String> getSymbols(Expression expression) {
        ExpressionProcessor<Collection<String>> symbolCollector = new SymbolCollector();
        symbolCollector.process(expression);
        return symbolCollector.getResult();
    }

    private static class SymbolCollector implements ExpressionProcessor<Collection<String>> {
        Collection<String> symbols = new ArrayList<String>();

        public void process(Expression expression) {
            if (expression instanceof DslSqlModel.SelectPartExpression) {
                expression.process(this);
                return;
            }
            if (expression instanceof Aliased && ((Aliased) expression).getAlias() != null) {
                symbols.add(((Aliased) expression).getAlias());
            } else {
                symbols.add(expression.toString());
            }
        }

        public Collection<String> getResult() {
            return symbols;
        }
    }

    private static class LeafExpressionCollector implements ExpressionProcessor<Collection<Expression>> {
        private final Collection<Expression> expressions = new ArrayList<Expression>();

        public void process(Expression expression) {
            if (expression instanceof DslSqlModel.SelectPartExpression) {
                expression.process(this);
                return;
            }
            if (expression instanceof Aliased) {
                Expression aliased = ((Aliased<Expression>) expression).getAliased();
                expressions.add(aliased);
            } else {
                expressions.add(expression);
            }
        }

        public Collection<Expression> getResult() {
            return expressions;
        }
    }

    private static class TableNameCollector extends AbstractExpressionProcessor<Collection<String>> {
        private final Collection<String> tables = new HashSet<String>();

        public void doProcess(Expression expression) {
            if (expression instanceof Field) {
                tables.add(((Field) expression).getTableName());
            }
        }

        public Collection<String> getResult() {
            return tables;
        }
    }
}
