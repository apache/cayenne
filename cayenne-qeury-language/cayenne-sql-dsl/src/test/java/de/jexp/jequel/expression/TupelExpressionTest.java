package de.jexp.jequel.expression;

import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.table.Table;
import junit.framework.TestCase;

public class TupelExpressionTest extends TestCase {
    public void testRowTupelExpression() {
        assertTrue("table is row tuple", SimpleListExpression.class.isAssignableFrom(Table.class));
        assertTrue("sql is row tuple", SimpleListExpression.class.isAssignableFrom(Sql.class));
    }
}
