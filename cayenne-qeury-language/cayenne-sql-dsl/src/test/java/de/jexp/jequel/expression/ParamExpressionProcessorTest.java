package de.jexp.jequel.expression;

import static de.jexp.jequel.expression.Expressions.*;
import de.jexp.jequel.processor.ParameterCollectorProcessor;
import de.jexp.jequel.sql.Sql;
import static de.jexp.jequel.sql.Sql.*;
import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ParamExpressionProcessorTest extends TestCase {

    public void testParamExpressionProcessor() {
        final Sql sql = Sql.Select(TEST_TABLES.ARTICLE.OID).from(TEST_TABLES.ARTICLE)
                .where(TEST_TABLES.ARTICLE.ARTICLE_NO.like(Expressions.param(1))
                        .and(TEST_TABLES.ARTICLE_COLOR.OID.eq(Expressions.named("article")))
                        .or(TEST_TABLES.ARTICLE.OID.in(Expressions.named("article_oid", Arrays.asList(1, 2, 3))))).toSql();

        assertEquals("select ARTICLE.OID from ARTICLE" +
                " where ARTICLE.ARTICLE_NO like ?" +
                " and ARTICLE_COLOR.OID = :article" +
                " or ARTICLE.OID in (:article_oid)",
                sql.toString());

        final ParameterCollectorProcessor parameterCollectorProcessor = new ParameterCollectorProcessor();
        parameterCollectorProcessor.process(sql);
        final List<ParamExpression> params = parameterCollectorProcessor.getResult();
        assertEquals(3, params.size());
        final ParamExpression<Number> param0 = params.get(0);
        assertEquals(1, param0.getValue().intValue());
        assertFalse(param0.isNamedExpression());

        final ParamExpression param1 = params.get(1);
        assertNull(param1.getValue());
        assertEquals("article", param1.getLiteral());

        final ParamExpression param2 = params.get(2);
        assertEquals(3, ((Collection) param2.getValue()).size());
        assertEquals("article_oid", param2.getLiteral());

    }
}
