package de.jexp.jequel.expression;

import de.jexp.jequel.processor.DebugExpressionProcessor;
import de.jexp.jequel.sql.Sql;
import static de.jexp.jequel.sql.Sql.*;
import de.jexp.jequel.tables.TEST_TABLES;
import junit.framework.TestCase;

public class DebugProcessorTest extends TestCase {
    private static final String EXPECTED_DEBUG_INFO =
            "select ARTICLE.OID from ARTICLE where ARTICLE_COLOR.OID is NULL {Sql}\n" +
                    "select ARTICLE.OID {Select}\n" +
                    "ARTICLE.OID {INTEGER}\n" +
                    "from ARTICLE {SelectPartColumnListExpression}\n" +
                    "ARTICLE {ARTICLE}\n" +
                    "ARTICLE.OID {INTEGER}\n" +
                    "ARTICLE.NAME {TableField}\n" +
                    "ARTICLE.ARTICLE_NO {INTEGER}\n" +
                    "ARTICLE_COLOR.OID is NULL {Where}\n" +
                    "ARTICLE_COLOR.OID is NULL {BinaryExpression}\n" +
                    "ARTICLE_COLOR.OID {INTEGER}\n" +
                    "NULL {}\n" +
                    " {SelectPartColumnListExpression}\n" +
                    "NULL {Having}\n" +
                    " {SelectPartColumnListExpression}\n";

    private DebugExpressionProcessor expressionProcessor;

    public void testParamExpressionProcessor() {
//        Sql sql = Select(TEST_TABLES.ARTICLE.OID).from(TEST_TABLES.ARTICLE).where(TEST_TABLES.ARTICLE_COLOR.OID.isNull()).toSql();
//        expressionProcessor.process(sql);
//        assertEquals(EXPECTED_DEBUG_INFO, expressionProcessor.getResult());
    }

    protected void setUp() throws Exception {
        super.setUp();
        expressionProcessor = new DebugExpressionProcessor();
    }
}