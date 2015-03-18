package de.jexp.jequel.expression;

import static de.jexp.jequel.expression.Expressions.*;
import de.jexp.jequel.processor.DebugExpressionProcessor;
import de.jexp.jequel.sql.Sql;
import static de.jexp.jequel.sql.Sql.*;
import de.jexp.jequel.tables.TEST_TABLES;
import junit.framework.TestCase;

public class DebugProcessorTest extends TestCase {
    private static final String EXPECTED_DEBUG_INFO =
            "select ARTICLE.OID from ARTICLE where ARTICLE_COLOR.OID is NULL {Sql}\n" +
                    "select ARTICLE.OID {SelectPartColumnTupleExpression}\n" +
                    "ARTICLE.OID {TableField}\n" +
                    "from ARTICLE {SelectPartColumnTupleExpression}\n" +
                    "ARTICLE {ARTICLE}\n" +
                    "ARTICLE.OID {TableField}\n" +
                    "ARTICLE.NAME {TableField}\n" +
                    "ARTICLE.ARTICLE_NO {TableField}\n" +
                    "where ARTICLE_COLOR.OID is NULL {SelectPartMutableBooleanExpression}\n" +
                    "ARTICLE_COLOR.OID is NULL {BinaryExpression}\n" +
                    "ARTICLE_COLOR.OID {TableField}\n" +
                    "NULL {ConstantExpression}\n" +
                    " {SelectPartColumnTupleExpression}\n" +
                    " {SelectPartMutableBooleanExpression}\n" +
                    " {SelectPartColumnTupleExpression}\n";

    private DebugExpressionProcessor expressionProcessor;

    public void testParamExpressionProcessor() {
        Sql sql = Select(TEST_TABLES.ARTICLE.OID).from(TEST_TABLES.ARTICLE).where(TEST_TABLES.ARTICLE_COLOR.OID.isNull()).toSql();
        expressionProcessor.process(sql);
        assertEquals(EXPECTED_DEBUG_INFO, expressionProcessor.getResult());
    }

    protected void setUp() throws Exception {
        super.setUp();
        expressionProcessor = new DebugExpressionProcessor();
    }
}