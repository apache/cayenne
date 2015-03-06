package de.jexp.jequel.defaults;

import static de.jexp.jequel.expression.Expressions.*;
import de.jexp.jequel.sql.Sql;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

public class SqlFormatTest extends TestCase {
    private final ARTICLE ARTICLE2 = ARTICLE.AS("article2");
    private final Sql sql = Sql.Select(ARTICLE2.OID).from(ARTICLE2).where(ARTICLE2.OID.isNot(NULL)).toSql();

    public void testSql92() {
        final String sql92String = new Sql92().append(sql).toString();
        assertEquals("select ARTICLE2.OID from ARTICLE as ARTICLE2 where ARTICLE2.OID is not NULL", sql92String);
    }

    public void testOracleSql() {
        final String oracleString = new OracleSql().append(sql).toString();
        assertEquals("select ARTICLE2.OID from ARTICLE ARTICLE2 where ARTICLE2.OID is not NULL", oracleString);
    }

    public void testTestSql() {
        assertEquals(" test   test   test   test   test   test ", new TestFormat().visit(sql));
    }
}
