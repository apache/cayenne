package de.jexp.jequel;

import static de.jexp.jequel.expression.Expressions.*;
import static de.jexp.jequel.sql.Sql.*;
import de.jexp.jequel.tables.TEST_TABLES;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

public class SubSelectTest extends TestCase {
    public void testSubSelect() {
        ARTICLE ARTICLE2 = ARTICLE.as("ARTICLE2");
        SqlString sqlString = Select(ARTICLE.OID)
                .from(ARTICLE)
                .where(not(exits(
                        subSelect(e(1)).
                                from(ARTICLE2)
                                .where(ARTICLE2.OID.eq(ARTICLE.OID)
                                .and(ARTICLE2.ARTICLE_NO.isNull()))
                )));

        assertEquals("sub select",
                "select ARTICLE.OID from ARTICLE where not(exists(" +
                        "(select 1 from ARTICLE as ARTICLE2 where ARTICLE2.OID = ARTICLE.OID and ARTICLE2.ARTICLE_NO is NULL)" +
                        "))",
                sqlString.toString());

    }

    public void testSubSelect2() {
        TEST_TABLES.ARTICLE_EAN ARTICLE_EAN2 = ARTICLE_EAN.as("ARTICLE_EAN2");
        SqlString sqlString = Select(ARTICLE_EAN.EAN)
                .from(ARTICLE_EAN)
                .where(ARTICLE_EAN.ARTICLE_OID.in(
                        Select(ARTICLE_EAN2.ARTICLE_OID)
                                .from(ARTICLE_EAN2)
                                .where(ARTICLE_EAN2.EAN.eq(e("1234567890123"))
                                .and(ARTICLE_EAN.ARTICLE_OID.eq(ARTICLE_EAN2.ARTICLE_OID)))));
        assertEquals("sub select 2",
                "select ARTICLE_EAN.EAN " +
                        "from ARTICLE_EAN " +
                        "where ARTICLE_EAN.ARTICLE_OID in " +
                        "(select ARTICLE_EAN2.ARTICLE_OID " +
                        "from ARTICLE_EAN as ARTICLE_EAN2 " +
                        "where ARTICLE_EAN2.EAN = '1234567890123' " +
                        "and ARTICLE_EAN.ARTICLE_OID = ARTICLE_EAN2.ARTICLE_OID)"
                , sqlString.toString());
    }
}
