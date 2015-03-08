package de.jexp.jequel;

import static de.jexp.jequel.expression.Expressions.*;
import static de.jexp.jequel.sql.Sql.*;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

public class BasicSqlTest extends TestCase {

    public void testBasicSql() {
        final SqlString sql = Select(ARTICLE.OID, ARTICLE.ARTICLE_NO).from(ARTICLE);

        assertEquals("select ARTICLE.OID, ARTICLE.ARTICLE_NO from ARTICLE", sql.toString());
    }

    public void testOrderBySql() {
        final SqlString sql = Select(ARTICLE.OID)
                .from(ARTICLE).orderBy(ARTICLE.OID);

        assertEquals("select ARTICLE.OID from ARTICLE order by ARTICLE.OID", sql.toString());
    }

    public void testGroupBySql() {
        final SqlString sql = Select(ARTICLE.OID).from(ARTICLE)
                .groupBy(ARTICLE.OID);

        assertEquals("select ARTICLE.OID from ARTICLE group by ARTICLE.OID", sql.toString());
    }

    public void testHavingSql() {
        final SqlString sql = Select(ARTICLE.OID).from(ARTICLE)
                .groupBy(ARTICLE.OID).having(ARTICLE.OID.eq(ARTICLE.OID));

        assertEquals("select ARTICLE.OID from ARTICLE" +
                " group by ARTICLE.OID having ARTICLE.OID = ARTICLE.OID", sql.toString());
    }

    public void testWhereSql() {
        final SqlString sql =
                Select(ARTICLE.OID)
                        .from(ARTICLE, ARTICLE_COLOR)
                        .where(ARTICLE.OID.eq(ARTICLE_COLOR.ARTICLE_OID)
                                .and(ARTICLE.ARTICLE_NO.isNot(NULL)));

        assertEquals("select ARTICLE.OID" +
                " from ARTICLE, ARTICLE_COLOR" +
                " where ARTICLE.OID = ARTICLE_COLOR.ARTICLE_OID" +
                " and ARTICLE.ARTICLE_NO is not NULL", sql.toString());
    }
}
