package de.jexp.jequel.generator.tables_db.multi;

import static de.jexp.jequel.expression.Expressions.*;
import de.jexp.jequel.generator.GeneratorTestUtils;
import static de.jexp.jequel.generator.tables_db.multi.GEN_TEST_TABLES.ARTICLE;
import static de.jexp.jequel.sql.Sql.*;
import junit.framework.TestCase;

import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 19.10.2007 03:03:45
 */
public class MultiFileGeneratedTableSqlTest extends TestCase {
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
        GeneratorTestUtils.createTestMetaDataSourceFile("de.jexp.jequel.generator.tables_db.multi", GeneratorTestUtils.TEST_CLASS, GeneratorTestUtils.MULTI_FILE);
    }

    protected void tearDown() throws Exception {
        GeneratorTestUtils.closeDatabase();
    }
}
