package de.jexp.jequel.expression;

import static de.jexp.jequel.sql.Expressions.*;

import de.jexp.jequel.Sql92Format;
import de.jexp.jequel.processor.ParameterCollectorProcessor;
import de.jexp.jequel.sql.Sql;

import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ParamExpressionProcessorTest extends TestCase {

    public void testParamExpressionProcessor() {
        Sql sql = Sql.Select(ARTICLE.OID)
                       .from(ARTICLE)
                      .where(ARTICLE.ARTICLE_NO.gt(param(1))
                              .and(ARTICLE_COLOR.OID.eq(named("article")))
                              .or(ARTICLE.OID.in(named("article_oid", Arrays.asList(1, 2, 3))))).toSql();

        assertEquals("select ARTICLE.OID from ARTICLE" +
                " where (ARTICLE.ARTICLE_NO > ?" + // TODO brackets shouldn't be here
                " and ARTICLE_COLOR.OID = :article)" +
                " or ARTICLE.OID in (:article_oid)",
                sql.accept(new Sql92Format()));

        ParameterCollectorProcessor paramsCollector = new ParameterCollectorProcessor();
        paramsCollector.process(sql.getWhere().getBooleanExpression());
        List<ParamExpression> params = paramsCollector.getResult();
        assertEquals(3, params.size());
        ParamExpression<Number> param0 = params.get(0);
        assertEquals(1, param0.getValue().intValue());
        assertFalse(param0.isNamedExpression());

        ParamExpression param1 = params.get(1);
        assertNull(param1.getValue());
        assertEquals("article", param1.getLiteral());

        ParamExpression param2 = params.get(2);
        assertEquals(3, ((Collection) param2.getValue()).size());
        assertEquals("article_oid", param2.getLiteral());

    }
}
