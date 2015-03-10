package de.jexp.jequel;

import static de.jexp.jequel.sql.Sql.*;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

public class JoinTest extends TestCase {
    public void testJoinSql() {
        SqlString sql = Select(ARTICLE.OID).from(ARTICLE.join(ARTICLE_COLOR));

        assertEquals("select ARTICLE.OID from (ARTICLE join ARTICLE_COLOR" +
                " on (ARTICLE.OID = ARTICLE_COLOR.ARTICLE_OID))",
                sql.toString());
    }

    public void testAliasJoinSql() {
        ARTICLE ARTICLE2 = ARTICLE.as("ARTICLE2");
        SqlString sql = Select(ARTICLE2.OID).from(ARTICLE2.join(ARTICLE_COLOR));

        assertEquals("select ARTICLE2.OID from (ARTICLE as ARTICLE2 join ARTICLE_COLOR" +
                " on (ARTICLE2.OID = ARTICLE_COLOR.ARTICLE_OID))",
                sql.toString());
    }

    public void testDoubleJoinSql() {
        ARTICLE ARTICLE2 = ARTICLE.as("ARTICLE2");
        SqlString sql = Select(ARTICLE.OID).from(ARTICLE.join(ARTICLE_COLOR).join(ARTICLE2)
                .on(ARTICLE2.OID.eq(ARTICLE_COLOR.ARTICLE_OID)));

        assertEquals("select ARTICLE.OID from ((ARTICLE join ARTICLE_COLOR" +
                " on (ARTICLE.OID = ARTICLE_COLOR.ARTICLE_OID))" +
                " join ARTICLE as ARTICLE2 on (ARTICLE2.OID = ARTICLE_COLOR.ARTICLE_OID))",
                sql.toString());
    }

    public void testJoinSqlExpression() {
        SqlString sql = Select(ARTICLE.OID).from(ARTICLE.join(ARTICLE_COLOR)
                .on(ARTICLE.OID.eq(ARTICLE_COLOR.ARTICLE_OID)));

        assertEquals("select ARTICLE.OID from (ARTICLE join ARTICLE_COLOR" +
                " on (ARTICLE.OID = ARTICLE_COLOR.ARTICLE_OID))",
                sql.toString());
    }
}