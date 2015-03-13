package de.jexp.jequel.expression;

import de.jexp.jequel.SqlString;
import static de.jexp.jequel.expression.Expressions.*;
import static de.jexp.jequel.sql.Sql.*;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 18.10.2007 15:17:02
 */
public class BinaryBooleanExpressionTest extends TestCase {
    public void testAndSql() {
        final String sql = TEST_TABLES.ARTICLE.OID.eq(Expressions.NULL).or(TEST_TABLES.ARTICLE.OID.eq(TEST_TABLES.ARTICLE.OID)).toString();
        assertEquals("ARTICLE.OID is NULL or ARTICLE.OID = ARTICLE.OID", sql);
    }

    public void testOrSql() {
        final String sql = Expressions.TRUE.or(Expressions.FALSE).toString();
        assertEquals("TRUE or FALSE", sql);
    }

    public void testORSql() {
        final String sql = Expressions.TRUE.OR(Expressions.FALSE).toString();
        assertEquals("(TRUE or FALSE)", sql);
    }

    public void testAndORSql() {
        final String sql = Expressions.TRUE.and(Expressions.TRUE.OR(Expressions.FALSE)).toString();
        assertEquals("TRUE and (TRUE or FALSE)", sql);
    }

    public void testAndOrSql() {
        final String sql = Expressions.TRUE.and(Expressions.TRUE.or(Expressions.FALSE)).toString();
        assertEquals("TRUE and TRUE or FALSE", sql);
    }

    public void testIn() {
        assertEquals("ARTICLE.OID in (1, 2, 3, 5)", TEST_TABLES.ARTICLE.OID.in(1, 2, 3, 5).toString());
    }

    public void testInSubSelect() {
        final SqlString sqlString =
                TEST_TABLES.ARTICLE.OID.in(
                        Sql.Select(TEST_TABLES.ARTICLE.OID).from(TEST_TABLES.ARTICLE)
                );

        assertEquals("ARTICLE.OID in (select ARTICLE.OID from ARTICLE)", sqlString.toString());
    }
}
