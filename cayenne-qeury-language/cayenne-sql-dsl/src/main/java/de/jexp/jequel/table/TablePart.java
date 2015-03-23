package de.jexp.jequel.table;

import de.jexp.jequel.expression.DefaultExpressionsFactory;
import de.jexp.jequel.expression.ExpressionsFactory;
import de.jexp.jequel.expression.VariableExpression;

public interface TablePart extends VariableExpression {

    ExpressionsFactory EXPRESSIONS_FACTORY = new DefaultExpressionsFactory();

    String getName();

    String getTableName();
}
