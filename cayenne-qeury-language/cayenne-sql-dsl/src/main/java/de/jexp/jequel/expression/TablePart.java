package de.jexp.jequel.expression;

import de.jexp.jequel.expression.DefaultExpressionsFactory;
import de.jexp.jequel.expression.ExpressionsFactory;
import de.jexp.jequel.expression.PathExpression;
import de.jexp.jequel.sql.SqlDsl;

public interface TablePart extends SqlDsl.SqlVisitable {

    ExpressionsFactory EXPRESSIONS_FACTORY = new DefaultExpressionsFactory();

    String getName();

    String getTableName();
}
