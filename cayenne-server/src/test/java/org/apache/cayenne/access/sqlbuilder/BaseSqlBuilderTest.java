package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;

import static org.junit.Assert.assertEquals;

class BaseSqlBuilderTest {

    void assertSQL(String expected, Node node) {
        assertSQL(expected, node, new StringBuilderAppendable());
    }

    void assertQuotedSQL(String expected, Node node) {
        assertSQL(expected, node, new MockQuotedStringBuilderAppendable());
    }

    void assertSQL(String expected, Node node, QuotingAppendable appendable) {
        SQLGenerationVisitor visitor = new SQLGenerationVisitor(appendable);
        node.visit(visitor);
        assertEquals(expected, visitor.getSQLString());
    }

    static class MockQuotedStringBuilderAppendable extends StringBuilderAppendable {
        @Override
        public QuotingAppendable appendQuoted(CharSequence csq) {
            builder.append('`').append(csq).append('`');
            return this;
        }
    }
}
