package org.apache.cayenne.exp.parser;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ASTCustomAggregateTest {

    @Test
    public void parse() {
        Expression exp = ExpressionFactory.exp("agg('quantile', value, 0.3)");

        assertTrue(exp instanceof ASTCustomAggregate);
        assertEquals("agg(\"quantile\", value, 0.3)", exp.toString());
    }

    @Test
    public void customAggregateAsFunctionArg() {
        Expression exp = ExpressionFactory.exp("fn('format_quantile', agg('quantile', value, 0.3))");

        assertTrue(exp instanceof ASTCustomFunction);
        assertEquals("fn(\"format_quantile\", agg(\"quantile\", value, 0.3))", exp.toString());
    }

    @Test
    public void evaluate() {
        assertThrows(ExpressionException.class, () -> new ASTCustomAggregate("test").evaluate(new Object()));
        assertThrows(UnsupportedOperationException.class, () -> new ASTCustomAggregate("test").evaluateCollection(List.of()));
    }
}