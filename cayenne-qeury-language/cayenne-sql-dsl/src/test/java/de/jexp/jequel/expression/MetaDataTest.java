package de.jexp.jequel.expression;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.TableMetaData;
import de.jexp.jequel.generator.data.TableMetaDataColumn;
import junit.framework.TestCase;

public class MetaDataTest extends TestCase {
    public void testMetaDataReferences() {
        final SchemaMetaData schema = GeneratorTestUtils.createTestSchemaMetaData();
        final TableMetaData article = schema.getTable(GeneratorTestUtils.ARTICLE);
        assertSame(schema, article.getSchema());
        final TableMetaDataColumn oidColumn = article.getColumn(GeneratorTestUtils.OID);
        assertSame(article, oidColumn.getTable());
    }
}
