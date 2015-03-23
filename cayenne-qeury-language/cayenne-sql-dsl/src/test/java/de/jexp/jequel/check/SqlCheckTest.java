package de.jexp.jequel.check;

import static de.jexp.jequel.sql.Expressions.*;
import de.jexp.jequel.sql.Sql;
import static de.jexp.jequel.sql.Sql.*;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import de.jexp.jequel.test.Assert;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SqlCheckTest extends TestCase {
/*    public void testFromSymbols() {
        Sql sql = Select().from(ARTICLE).where(ARTICLE.OID.isNull()).toSql();
        Collection<String> fromSymbols = new SqlChecker(sql).getFromSymbols();
        assertEquals(Arrays.asList(ARTICLE.getName()), fromSymbols);
    }

    public void testFromSymbolsAlias() {
        ARTICLE_COLOR ARTICLE_COLOR2 = ARTICLE_COLOR.as("article_color_alias");
        Sql sql = Select().from(ARTICLE, ARTICLE_COLOR2).where(ARTICLE.OID.isNull()).toSql();
        Collection<String> fromSymbols = new SqlChecker(sql).getFromSymbols();
        assertEquals(Arrays.asList(ARTICLE.getName(), "article_color_alias"), fromSymbols);
    }

    public void testFromSymbolsExpression() {
        Sql sql = Select().from(ARTICLE, subSelect(ARTICLE.OID).toSql().as("sql_alias")).where(ARTICLE.OID.isNull()).toSql();
        assertEquals("select * from ARTICLE, (select ARTICLE.OID) as sql_alias where ARTICLE.OID is NULL", sql.toString());
        Collection<String> fromSymbols = new SqlChecker(sql).getFromSymbols();
        assertEquals(Arrays.asList(ARTICLE.getName(), "sql_alias"), fromSymbols);
    }

    public void testUsedTables() {
        Sql sql = Select(ARTICLE.OID, ARTICLE_COLOR.ARTICLE_OID).from(ARTICLE, subSelect(ARTICLE.OID).toSql().as("sql_alias")).where(ARTICLE.OID.isNull()).toSql();
        Collection<String> usedTables = new SqlChecker(sql).getUsedTables();
        List<String> expected = Arrays.asList(ARTICLE.getName(), ARTICLE_COLOR.getName());
        assertEquals(expected.size(), usedTables.size());
        assertTrue(expected.containsAll(usedTables));
    }

    public void testCheckUsedTables() {
        Sql sql = Select(ARTICLE.OID, ARTICLE_COLOR.ARTICLE_OID).from(ARTICLE, subSelect(ARTICLE.OID).toSql().as("sql_alias")).where(ARTICLE.OID.isNull()).toSql();
        SqlChecker checker = new SqlChecker(sql);
        TableUsageCheckResult checkResult = checker.checkUsedTables();
        assertFalse(checkResult.isValid());
        Assert.assertEquals("unused tables", Arrays.asList("sql_alias"), checkResult.getUnusedTables());
        Assert.assertEquals("missing tables", Arrays.asList(ARTICLE_COLOR.getName()), checkResult.getMissingTables());
        List<String> expected = Arrays.asList(ARTICLE.getName());
        Assert.assertEquals("used tables", expected, checkResult.getUsedTables());
    }*/

    public void testCheckGroupBy() {
        Sql sql = Select(ARTICLE.OID, ARTICLE_COLOR.ARTICLE_OID).from().groupBy(ARTICLE.OID).toSql();
        SqlChecker checker = new SqlChecker(sql);
        Assert.assertEquals("group by expressions", Arrays.asList(ARTICLE.OID.toString()), checker.getGroupByExpressions());
        Collection<String> wrongSelectColumns = checker.checkGroupBy();
        Assert.assertEquals("wrong group by select", Arrays.asList(ARTICLE_COLOR.ARTICLE_OID.toString()), wrongSelectColumns);
    }

    public void testCheckGroupBySelectMin() {
        Sql sql = Select(ARTICLE.OID, min(ARTICLE_COLOR.ARTICLE_OID)).from().groupBy(ARTICLE.OID).toSql();
        SqlChecker checker = new SqlChecker(sql);
        Assert.assertEquals("group by expressions", Arrays.asList(ARTICLE.OID.toString()), checker.getGroupByExpressions());
        Collection<String> wrongSelectColumns = checker.checkGroupBy();
        Assert.assertEquals("wrong group by select ", Collections.emptyList(), wrongSelectColumns);
    }

    public void testCheckGroupByMin() {
        Sql sql = Select(min(ARTICLE.OID), ARTICLE_COLOR.ARTICLE_OID).from().groupBy(min(ARTICLE.OID)).toSql();
        SqlChecker checker = new SqlChecker(sql);
        Assert.assertEquals("group by expressions", Arrays.asList(min(ARTICLE.OID).toString()), checker.getGroupByExpressions());
        Collection<String> wrongSelectColumns = checker.checkGroupBy();
        Assert.assertEquals("wrong group by select ", Arrays.asList(ARTICLE_COLOR.ARTICLE_OID.toString()), wrongSelectColumns);
    }

    public void testCheckGroupByMin2() {
        Sql sql = Select(min(ARTICLE.OID), min(ARTICLE_COLOR.ARTICLE_OID)).from().groupBy(min(ARTICLE.OID)).toSql();
        SqlChecker checker = new SqlChecker(sql);
        Assert.assertEquals("group by expressions", Arrays.asList(min(ARTICLE.OID).toString()), checker.getGroupByExpressions());
        Collection<String> wrongSelectColumns = checker.checkGroupBy();
        Assert.assertEquals("wrong group by select ", Collections.emptyList(), wrongSelectColumns);
    }
}
