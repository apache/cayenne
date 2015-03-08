package de.jexp.jequel.check;

import de.jexp.jequel.expression.Aliased;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ExpressionProcessor;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.table.Field;
import de.jexp.jequel.table.Table;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Check compliance of the sql by application of rules
 */
public class RuleBasedSqlChecker {
    public class TableIsUsedByOneFieldSpec extends CheckSpec {
        private final String relationName;

        public TableIsUsedByOneFieldSpec(String relationName) {
            this.relationName = relationName;
        }

        public void check(Expression expression) {
            if (expression instanceof Field) {
                if (((Field) expression).getTableName().equals(relationName)) {
                    setSatisfied();
                }
            }
        }

    }

    private final Sql sql;

    public RuleBasedSqlChecker(Sql sql) {
        this.sql = sql;
    }

    public TableUsageCheckResult checkTableUses() {
        Collection<String> relationNames = getRelationNames();
        TableUsageCheckResult tableUsageCheckResult = new TableUsageCheckResult(relationNames);
        for (String relationName : relationNames) {
            TableIsUsedByOneFieldSpec isUsedByOneFieldSpec = new TableIsUsedByOneFieldSpec(relationName);
            SpecProcessor<TableIsUsedByOneFieldSpec> specProcessor = new SpecProcessor<TableIsUsedByOneFieldSpec>(isUsedByOneFieldSpec);
            specProcessor.process(sql);
            if (isUsedByOneFieldSpec.isSatisfied()) {
                tableUsageCheckResult.addUsedTable(relationName);
            }
        }
        return null;
    }

    protected Collection<String> getRelationNames() {
        Collection<String> relationNames = new ArrayList<String>(10);
        for (Expression relation : sql.getFrom().getExpressions()) {
            relationNames.add(getRelationName(relation));
        }
        return relationNames;
    }

    protected String getRelationName(Expression relation) {
        String relationName = null;
        if (relation instanceof Aliased) {
            Aliased aliased = (Aliased) relation;
            if (aliased.getAlias() != null) {
                relationName = aliased.getAlias();
            } else if (aliased instanceof Table) {
                relationName = ((Table) aliased).getName();
            }
        }
        return relationName;
    }

    class SpecProcessor<T extends CheckSpec> implements ExpressionProcessor<T> {
        private final T checkSpec;

        SpecProcessor(T checkSpec) {
            this.checkSpec = checkSpec;
        }

        public void process(Expression expression) {
            checkSpec.check(expression);
            if (!checkSpec.isSatisfied() && !checkSpec.isViolated()) {
                expression.process(this);
            }
        }

        public T getResult() {
            return checkSpec;
        }
    }
}
