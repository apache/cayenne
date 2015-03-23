package de.jexp.jequel;

import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.sql.Sql;
import static de.jexp.jequel.sql.Sql.*;

import de.jexp.jequel.sql.SqlDsl;
import de.jexp.jequel.table.FieldAlias;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import junit.framework.TestCase;
import org.junit.Test;

public class AliasTest {
    private static final Sql92Format SQL_92_FORMAT = new Sql92Format();

    @Test
    public void testAliasSql() {
        ARTICLE ARTICLE2 = ARTICLE.as("ARTICLE2");
        FieldAlias ARTICLE_COLOR_OID = ARTICLE_COLOR.OID.as("ARTICLE_COLOR_OID");

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

    @Test
    public void testSqlAlias() {
        Sql sql = Select(ARTICLE.ARTICLE_NO).toSql();
        RowListExpression sqlAlias = sql.as("sql_alias");
        assertSame(sql, sqlAlias.getAliased());
        assertSame("sql_alias", sqlAlias.getAlias());
        assertEquals("select ARTICLE.ARTICLE_NO as sql_alias", sqlAlias.accept(SQL_92_FORMAT));
    }
}