package de.jexp.jequel.expression;

import static de.jexp.jequel.expression.Expressions.*;
import static de.jexp.jequel.tables.TEST_TABLES.*;

import de.jexp.jequel.sql.Sql;
import junit.framework.TestCase;

public class BinaryBooleanExpressionTest extends TestCase {
    public void testAndSql() {
        assertEquals("ARTICLE.OID is NULL or ARTICLE.OID = ARTICLE.OID",
                ARTICLE.OID.eq(NULL).or(ARTICLE.OID.eq(ARTICLE.OID)).toString());
    }

    public void testOrSql() {
        assertEquals("TRUE or FALSE",
                TRUE.or(FALSE).toString());
    }

    public void testAndORSql() {
        assertEquals("TRUE and (TRUE or FALSE)",
                TRUE.and(TRUE.or(FALSE)).toString());
    }

    public void testAndOrSql() {
        assertEquals("(TRUE and TRUE) or FALSE",
                TRUE.and(TRUE).or(FALSE).toString());
    }

    public void testOrAndSql() {
        assertEquals("FALSE or (TRUE and TRUE)",
                FALSE.or(TRUE.and(TRUE)).toString());
    }

    public void testIn() {
        assertEquals("ARTICLE.OID in (1, 2, 3, 5)",
                ARTICLE.OID.in(1, 2, 3, 5).toString());
    }

    public void testInSubSelect() {
        assertEquals("ARTICLE.OID in (select ARTICLE.OID from ARTICLE)",
                ARTICLE.OID.in(Sql.Select(ARTICLE.OID).from(ARTICLE)).toString());
    }
}
