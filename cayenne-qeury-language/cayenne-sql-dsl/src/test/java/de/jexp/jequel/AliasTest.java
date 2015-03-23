package de.jexp.jequel;

import de.jexp.jequel.table.IColumn;
import org.junit.Test;

import static de.jexp.jequel.sql.Sql.Select;
import static de.jexp.jequel.tables.TEST_TABLES.ARTICLE;
import static de.jexp.jequel.tables.TEST_TABLES.ARTICLE_COLOR;
import static org.junit.Assert.assertEquals;

public class AliasTest {
    private static final Sql92Format SQL_92_FORMAT = new Sql92Format();

    @Test
    public void testAliasSql() {
        ARTICLE ARTICLE2 = ARTICLE.as("ARTICLE2");
        IColumn ARTICLE_COLOR_OID = ARTICLE_COLOR.OID.as("ARTICLE_COLOR_OID");

        SqlString sql = Select(ARTICLE2.OID, ARTICLE_COLOR_OID)
                .from(ARTICLE2, ARTICLE_COLOR)
                .where(ARTICLE2.OID.eq(ARTICLE_COLOR.ARTICLE_OID));

        assertEquals("select ARTICLE2.OID, ARTICLE_COLOR.OID as ARTICLE_COLOR_OID" +
                " from ARTICLE as ARTICLE2, ARTICLE_COLOR" +
                " where ARTICLE2.OID = ARTICLE_COLOR.ARTICLE_OID", sql.accept(SQL_92_FORMAT));
    }

    @Test
    public void testAliasSql2() {
        ARTICLE ARTICLE2 = ARTICLE.as("article2");
        SqlString sql = Select(ARTICLE2.OID,
                ARTICLE_COLOR.OID.as("ARTICLE_COLOR_OID"))
                .from(ARTICLE2, ARTICLE_COLOR)
                .where(ARTICLE2.OID.eq(ARTICLE_COLOR.ARTICLE_OID));

        assertEquals("select article2.OID, ARTICLE_COLOR.OID as ARTICLE_COLOR_OID" +
                " from ARTICLE as article2, ARTICLE_COLOR" +
                " where article2.OID = ARTICLE_COLOR.ARTICLE_OID", sql.accept(SQL_92_FORMAT));
    }
}