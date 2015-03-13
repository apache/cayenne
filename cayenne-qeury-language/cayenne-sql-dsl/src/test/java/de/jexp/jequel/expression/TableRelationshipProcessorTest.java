package de.jexp.jequel.expression;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.TableMetaData;
import de.jexp.jequel.generator.data.TableMetaDataColumn;
import de.jexp.jequel.generator.processor.TableRelationshipSchemaMetaDataProcessor;
import junit.framework.TestCase;

import java.util.List;

public class TableRelationshipProcessorTest extends TestCase {

    public void testTableRelationshipProcessorPrimaryKeys() {
        final SchemaMetaData schema = GeneratorTestUtils.createSchemaDataWithRelationship();
        new TableRelationshipSchemaMetaDataProcessor(schema).processMetaData();
        checkPrimaryKeys(schema.getTable(GeneratorTestUtils.ARTICLE), GeneratorTestUtils.OID);
        checkPrimaryKeys(schema.getTable(GeneratorTestUtils.ARTICLE_COLOR), GeneratorTestUtils.OID);
    }

    public void testTableRelationshipProcessorForeignKeys() {
        final SchemaMetaData schema = GeneratorTestUtils.createSchemaDataWithRelationship();
        new TableRelationshipSchemaMetaDataProcessor(schema).processMetaData();
        final TableMetaData article = schema.getTable(GeneratorTestUtils.ARTICLE);
        final TableMetaData articleColor = schema.getTable(GeneratorTestUtils.ARTICLE_COLOR);
        final List<TableMetaDataColumn> foreignKeys = article.getForeignKeys();
        assertEquals(1, foreignKeys.size());
        final TableMetaDataColumn foreignKey = foreignKeys.get(0);
        assertSame(articleColor, foreignKey.getTable());
        assertSame(articleColor.getColumn(GeneratorTestUtils.ARTICLE_OID), foreignKey);
        assertSame(article.getColumn(GeneratorTestUtils.OID), foreignKey.getReferencedColumn());
    }

    private void checkPrimaryKeys(final TableMetaData table, final String expectedPrimaryKeyColumnName) {
        final List<TableMetaDataColumn> primaryKeys = table.getPrimaryKeys();
        assertEquals(primaryKeys.toString(), 1, primaryKeys.size());
        final TableMetaDataColumn primaryKey = primaryKeys.get(0);
        assertSame(table.getPrimaryKey(), primaryKey);
        assertEquals(expectedPrimaryKeyColumnName, primaryKey.getName());
        assertTrue(primaryKey.isPrimaryKey());

    }

    public void testTableRelationshipProcessorPrimaryKeyPatterns() {
        final SchemaMetaData schema = GeneratorTestUtils.createTestSchemaMetaData();
        final TableRelationshipSchemaMetaDataProcessor processor = new TableRelationshipSchemaMetaDataProcessor(schema);
        processor.setPrimaryKeyColumnPattern("^ARTICLE_.*");
        processor.processMetaData();
        final TableMetaData article = schema.getTable(GeneratorTestUtils.ARTICLE);
        final List<TableMetaDataColumn> primaryKeys = article.getPrimaryKeys();
        assertEquals(primaryKeys.toString(), 2, primaryKeys.size());
        final TableMetaDataColumn primaryKey = primaryKeys.get(0);
        assertSame(article.getPrimaryKey(), primaryKey);
        assertTrue(primaryKeys.contains(article.getColumn(GeneratorTestUtils.ARTICLE_NO)));
        assertTrue(primaryKey.isPrimaryKey());
        assertTrue(primaryKeys.get(1).isPrimaryKey());

    }
}
