package de.jexp.jequel.check;

import static de.jexp.jequel.expression.Expressions.*;
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
    public void testFromSymbols() {
        final Sql sql = Select().from(ARTICLE).where(ARTICLE.OID.is(NULL)).toSql();
        final Collection<String> fromSymbols = new SqlChecker(sql).getFromSymbols();
        assertEquals(Arrays.asList(ARTICLE.getName()), fromSymbols);
    }

    public void testFromSymbolsAlias() {
        final ARTICLE_COLOR ARTICLE_COLOR2 = ARTICLE_COLOR.as("article_color_alias");
        final Sql sql = Select().from(ARTICLE, ARTICLE_COLOR2).where(ARTICLE.OID.is(NULL)).toSql();
        final Collection<String> fromSymbols = new SqlChecker(sql).getFromSymbols();
        assertEquals(Arrays.asList(ARTICLE.getName(), "article_color_alias"), fromSymbols);
    }

    public void testFromSymbolsExpression() {
        final Sql sql = Select().from(ARTICLE, subSelect(ARTICLE.OID).toSql().as("sql_alias")).where(ARTICLE.OID.is(NULL)).toSql();
        assertEquals("from ARTICLE, (select ARTICLE.OID) as sql_alias where ARTICLE.OID is NULL", sql.toString());
        final Collection<String> fromSymbols = new SqlChecker(sql).getFromSymbols();
        assertEquals(Arrays.asList(ARTICLE.getName(), "sql_alias"), fromSymbols);
    }

    public void testUsedTables() {
        final Sql sql = Select(ARTICLE.OID, ARTICLE_COLOR.ARTICLE_OID).from(ARTICLE, subSelect(ARTICLE.OID).toSql().as("sql_alias")).where(ARTICLE.OID.is(NULL)).toSql();
        final Collection<String> usedTables = new SqlChecker(sql).getUsedTables();
        final List<String> expected = Arrays.asList(ARTICLE.getName(), ARTICLE_COLOR.getName());
        assertEquals(expected.size(), usedTables.size());
        assertTrue(expected.containsAll(usedTables));
    }

    public void testCheckUsedTables() {
        final Sql sql = Select(ARTICLE.OID, ARTICLE_COLOR.ARTICLE_OID).from(ARTICLE, subSelect(ARTICLE.OID).toSql().as("sql_alias")).where(ARTICLE.OID.is(NULL)).toSql();
        final SqlChecker checker = new SqlChecker(sql);
        final TableUsageCheckResult checkResult = checker.checkUsedTables();
        assertFalse(checkResult.isValid());
        Assert.assertEquals("unused tables", Arrays.asList("sql_alias"), checkResult.getUnusedTables());
        Assert.assertEquals("missing tables", Arrays.asList(ARTICLE_COLOR.getName()), checkResult.getMissingTables());
        final List<String> expected = Arrays.asList(ARTICLE.getName());
        Assert.assertEquals("used tables", expected, checkResult.getUsedTables());
    }

    public void testCheckGroupBy() {
        final Sql sql = Select(ARTICLE.OID, ARTICLE_COLOR.ARTICLE_OID).from().groupBy(ARTICLE.OID).toSql();
        final SqlChecker checker = new SqlChecker(sql);
        Assert.assertEquals("group by expressions", Arrays.asList(ARTICLE.OID.toString()), checker.getGroupByExpressions());
        final Collection<String> wrongSelectColumns = checker.checkGroupBy();
        Assert.assertEquals("wrong group by select", Arrays.asList(ARTICLE_COLOR.ARTICLE_OID.toString()), wrongSelectColumns);
    }

    public void testCheckGroupBySelectMin() {
        final Sql sql = Select(ARTICLE.OID, min(ARTICLE_COLOR.ARTICLE_OID)).from().groupBy(ARTICLE.OID).toSql();
        final SqlChecker checker = new SqlChecker(sql);
        Assert.assertEquals("group by expressions", Arrays.asList(ARTICLE.OID.toString()), checker.getGroupByExpressions());
        final Collection<String> wrongSelectColumns = checker.checkGroupBy();
        Assert.assertEquals("wrong group by select ", Collections.emptyList(), wrongSelectColumns);
    }

    public void testCheckGroupByMin() {
        final Sql sql = Select(min(ARTICLE.OID), ARTICLE_COLOR.ARTICLE_OID).from().groupBy(min(ARTICLE.OID)).toSql();
        final SqlChecker checker = new SqlChecker(sql);
        Assert.assertEquals("group by expressions", Arrays.asList(min(ARTICLE.OID).toString()), checker.getGroupByExpressions());
        final Collection<String> wrongSelectColumns = checker.checkGroupBy();
        Assert.assertEquals("wrong group by select ", Arrays.asList(ARTICLE_COLOR.ARTICLE_OID.toString()), wrongSelectColumns);
    }

    public void testCheckGroupByMin2() {
        final Sql sql = Select(min(ARTICLE.OID), min(ARTICLE_COLOR.ARTICLE_OID)).from().groupBy(min(ARTICLE.OID)).toSql();
        final SqlChecker checker = new SqlChecker(sql);
        Assert.assertEquals("group by expressions", Arrays.asList(min(ARTICLE.OID).toString()), checker.getGroupByExpressions());
        final Collection<String> wrongSelectColumns = checker.checkGroupBy();
        Assert.assertEquals("wrong group by select ", Collections.emptyList(), wrongSelectColumns);
    }
}
