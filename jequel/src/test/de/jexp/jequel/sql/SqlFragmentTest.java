package de.jexp.jequel.sql;

import static de.jexp.jequel.expression.Expressions.*;
import static de.jexp.jequel.sql.Sql.*;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

public class SqlFragmentTest extends TestCase {
    public void testSqlFragment() {
        final Sql NOT_NULL_FRAGMENT = (Sql) Select().from(ARTICLE).where(ARTICLE.OID.isNot(NULL));

        final Sql ARTICLE_COLORS = (Sql) Select(ARTICLE.ARTICLE_NO, ARTICLE_COLOR.ARTICLE_OID).from(ARTICLE_COLOR)
                .where(ARTICLE_COLOR.OID.eq(ARTICLE.OID));

        ARTICLE_COLORS.append(NOT_NULL_FRAGMENT);

        assertEquals("select ARTICLE.ARTICLE_NO, ARTICLE_COLOR.ARTICLE_OID from ARTICLE_COLOR, ARTICLE where" +
                " ARTICLE_COLOR.OID = ARTICLE.OID and ARTICLE.OID is not NULL"
                , ARTICLE_COLORS.toString());
    }

    public void testFragmentPart() {
        final Sql NOT_NULL_FRAGMENT = (Sql) Select().from().where(ARTICLE.OID.isNot(NULL));
        final Sql ARTICLE_COLORS = (Sql) Select(ARTICLE.ARTICLE_NO, ARTICLE_COLOR.ARTICLE_OID).from(ARTICLE_COLOR)
                .where(ARTICLE_COLOR.OID.eq(ARTICLE.OID));
        ARTICLE_COLORS.append(NOT_NULL_FRAGMENT.where());

        assertEquals("select ARTICLE.ARTICLE_NO, ARTICLE_COLOR.ARTICLE_OID from ARTICLE_COLOR where" +
                " ARTICLE_COLOR.OID = ARTICLE.OID and ARTICLE.OID is not NULL"
                , ARTICLE_COLORS.toString());
        assertEquals("where ARTICLE.OID is not NULL", NOT_NULL_FRAGMENT.toString());

        ARTICLE_COLORS.where().and(TRUE.eq(FALSE));

        assertEquals("where ARTICLE.OID is not NULL", NOT_NULL_FRAGMENT.toString());
        assertEquals("select ARTICLE.ARTICLE_NO, ARTICLE_COLOR.ARTICLE_OID from ARTICLE_COLOR where" +
                " ARTICLE_COLOR.OID = ARTICLE.OID and ARTICLE.OID is not NULL and TRUE = FALSE",
                ARTICLE_COLORS.toString());
    }
}
