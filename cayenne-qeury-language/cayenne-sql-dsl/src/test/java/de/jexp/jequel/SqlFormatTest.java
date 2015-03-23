package de.jexp.jequel;

import de.jexp.jequel.sql.Sql;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import static org.junit.Assert.assertEquals;

import de.jexp.jequel.sql.SqlDsl;
import junit.framework.TestCase;
import org.junit.Test;

public class SqlFormatTest {
    private final ARTICLE ARTICLE2 = ARTICLE.as("ARTICLE2");
    private final Sql sql = Sql.Select(ARTICLE2.OID).from(ARTICLE2).where(ARTICLE2.OID.isNotNull()).toSql();

    @Test
    public void testSql92() {
        String sql92String = new Sql92().append(sql).accept((SqlDsl.SqlVisitor<? extends String>) new Sql92Format());
        assertEquals("select ARTICLE2.OID from ARTICLE as ARTICLE2 where ARTICLE2.OID is not NULL", sql92String);
    }

    @Test
    public void testOracleSql() {
        String oracleString = new OracleSql().append(sql).accept((SqlDsl.SqlVisitor<? extends String>) new Sql92Format());
        assertEquals("select ARTICLE2.OID from ARTICLE ARTICLE2 where ARTICLE2.OID is not NULL", oracleString);
    }
}
