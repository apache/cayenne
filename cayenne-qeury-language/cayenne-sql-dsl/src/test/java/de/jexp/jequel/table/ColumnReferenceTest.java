package de.jexp.jequel.table;

import de.jexp.jequel.expression.FieldReference;
import de.jexp.jequel.tables.TEST_TABLES;
import junit.framework.TestCase;

public class ColumnReferenceTest extends TestCase {
    public void testFieldReference() {
        final FieldReference fieldReference = new FieldReference(TEST_TABLES.ARTICLE.class, "ARTICLE_NO");
        assertSame(TEST_TABLES.ARTICLE.ARTICLE_NO, fieldReference.resolve());
    }

    public void testFieldReferenceMulti() {
        final FieldReference fieldReference = new FieldReference(TEST_TABLES.class, TEST_TABLES.ARTICLE.class, "ARTICLE_NO");
        assertSame(TEST_TABLES.ARTICLE.ARTICLE_NO, fieldReference.resolve());
    }
}
