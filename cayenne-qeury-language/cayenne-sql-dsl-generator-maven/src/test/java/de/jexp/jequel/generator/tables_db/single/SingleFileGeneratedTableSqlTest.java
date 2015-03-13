package de.jexp.jequel.generator.tables_db.single;

import static de.jexp.jequel.expression.Expressions.*;
import de.jexp.jequel.generator.GeneratorTestUtils;
import static de.jexp.jequel.generator.tables_db.single.GEN_TEST_TABLES.*;
import static de.jexp.jequel.sql.Sql.*;
import junit.framework.TestCase;

import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 19.10.2007 03:03:45
 */
public abstract class SingleFileGeneratedTableSqlTest extends TestCase {
    public void testGeneratedSql() {
        final String sql =
                Select(ARTICLE.OID, ARTICLE.ARTICLE_NO)
                        .from(ARTICLE)
                        .where(ARTICLE.OID.is_not(NULL)).toString();
        assertEquals("select ARTICLE.OID, ARTICLE.ARTICLE_NO from ARTICLE where ARTICLE.OID is not NULL", sql);
        final Map result = GeneratorTestUtils.jdbcTemplate.queryForMap(sql);
        assertEquals("oid", 10, ((Number) result.get(ARTICLE.OID.getName())).intValue());
        assertEquals("article_no", 12345, ((Number) result.get(ARTICLE.ARTICLE_NO.getName())).intValue());
    }

    protected void setUp() throws Exception {
        GeneratorTestUtils.createTestMetaDataSourceFile("de.jexp.jequel.generator.tables_db.single", GeneratorTestUtils.TEST_CLASS, GeneratorTestUtils.SINGLE_FILE);
    }

    protected void tearDown() throws Exception {
        GeneratorTestUtils.closeDatabase();
    }
}