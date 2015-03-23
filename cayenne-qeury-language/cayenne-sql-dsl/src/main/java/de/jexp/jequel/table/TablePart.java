package de.jexp.jequel.table;

import de.jexp.jequel.expression.DefaultExpressionsFactory;
import de.jexp.jequel.expression.ExpressionsFactory;
import de.jexp.jequel.expression.PathExpression;

public interface TablePart {

    ExpressionsFactory EXPRESSIONS_FACTORY = new DefaultExpressionsFactory();

    String getName();

    String getTableName();
}
