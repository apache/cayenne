package de.jexp.jequel;

import static de.jexp.jequel.sql.Sql.*;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

public class JoinTest extends TestCase {
    private static final Sql92Format SQL_92_FORMAT = new Sql92Format();

    public void testJoinSql() {
        SqlString sql = Select(ARTICLE.OID).from(ARTICLE.join(ARTICLE_COLOR));

        assertEquals("select ARTICLE.OID from (ARTICLE join ARTICLE_COLOR" +
                " on (ARTICLE.OID = ARTICLE_COLOR.ARTICLE_OID))",
                sql.accept(SQL_92_FORMAT));
    }

    public void testAliasJoinSql() {
        ARTICLE ARTICLE2 = ARTICLE.as("ARTICLE2");
        SqlString sql = Select(ARTICLE2.OID).from(ARTICLE2.join(ARTICLE_COLOR));

        assertEquals("select ARTICLE2.OID from (ARTICLE as ARTICLE2 join ARTICLE_COLOR" +
                " on (ARTICLE2.OID = ARTICLE_COLOR.ARTICLE_OID))",
                sql.accept(SQL_92_FORMAT));
    }

    public void testDoubleJoinSql() {
        ARTICLE ARTICLE2 = ARTICLE.as("ARTICLE2");
        SqlString sql = Select(ARTICLE.OID).from(ARTICLE.join(ARTICLE_COLOR).join(ARTICLE2)
                .on(ARTICLE2.OID.eq(ARTICLE_COLOR.ARTICLE_OID)));

        assertEquals("select ARTICLE.OID from ((ARTICLE join ARTICLE_COLOR" +
                " on (ARTICLE.OID = ARTICLE_COLOR.ARTICLE_OID))" +
                " join ARTICLE as ARTICLE2 on (ARTICLE2.OID = ARTICLE_COLOR.ARTICLE_OID))",
                sql.accept(SQL_92_FORMAT));
    }

    public void testJoinSqlExpression() {
        SqlString sql = Select(ARTICLE.OID).from(ARTICLE.join(ARTICLE_COLOR)
                .on(ARTICLE.OID.eq(ARTICLE_COLOR.ARTICLE_OID)));

        assertEquals("select ARTICLE.OID from (ARTICLE join ARTICLE_COLOR" +
                " on (ARTICLE.OID = ARTICLE_COLOR.ARTICLE_OID))",
                sql.accept(SQL_92_FORMAT));
    }
}