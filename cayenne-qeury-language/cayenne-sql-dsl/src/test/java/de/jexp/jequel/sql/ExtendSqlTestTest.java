package de.jexp.jequel.sql;

import static de.jexp.jequel.sql.Expressions.*;
import static de.jexp.jequel.sql.Sql.*;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

public class ExtendSqlTestTest extends TestCase {
    public void testExtendSqlTest() {
        final Sql sql = (Sql) Select(ARTICLE.OID); // empty select

        // fill dynamically, use control logic to select

        sql.select(ARTICLE.ARTICLE_NO);

        sql.from(ARTICLE);

        sql.where(ARTICLE.OID.ne(NULL));

        sql.groupBy(ARTICLE.ARTICLE_NO);

        sql.having(ARTICLE.ARTICLE_NO.ge(1000));

        sql.orderBy(ARTICLE.ARTICLE_NO);

        assertEquals("select ARTICLE.OID, ARTICLE.ARTICLE_NO from ARTICLE" +
                " where ARTICLE.OID is not NULL" +
                " group by ARTICLE.ARTICLE_NO" +
                " having ARTICLE.ARTICLE_NO >= 1000" +
                " order by ARTICLE.ARTICLE_NO",
                sql.toString());
    }
}
