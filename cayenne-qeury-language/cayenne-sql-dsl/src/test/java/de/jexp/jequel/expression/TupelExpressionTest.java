package de.jexp.jequel.expression;

import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.table.BaseTable;
import junit.framework.TestCase;

public class TupelExpressionTest extends TestCase {
    public void testRowTupelExpression() {
        assertTrue("table is row tuple", RowListExpression.class.isAssignableFrom(BaseTable.class));
        assertTrue("sql is row tuple", RowListExpression.class.isAssignableFrom(Sql.class));
    }
}
