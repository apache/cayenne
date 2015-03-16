package de.jexp.jequel.expression;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.TableMetaData;
import de.jexp.jequel.generator.data.TableMetaDataColumn;
import de.jexp.jequel.generator.processor.TableRelationshipSchemaMetaDataProcessor;
import junit.framework.TestCase;

import java.util.List;

public class TableRelationshipProcessorTest extends TestCase {

    public void testTableRelationshipProcessorPrimaryKeys() {
        SchemaMetaData schema = GeneratorTestUtils.createSchemaDataWithRelationship();
        new TableRelationshipSchemaMetaDataProcessor(schema).processMetaData();
        checkPrimaryKeys(schema.getTable(GeneratorTestUtils.ARTICLE), GeneratorTestUtils.OID);
        checkPrimaryKeys(schema.getTable(GeneratorTestUtils.ARTICLE_COLOR), GeneratorTestUtils.OID);
    }

    public void testTableRelationshipProcessorForeignKeys() {
        SchemaMetaData schema = GeneratorTestUtils.createSchemaDataWithRelationship();
        new TableRelationshipSchemaMetaDataProcessor(schema).processMetaData();
        TableMetaData article = schema.getTable(GeneratorTestUtils.ARTICLE);
        TableMetaData articleColor = schema.getTable(GeneratorTestUtils.ARTICLE_COLOR);
        List<TableMetaDataColumn> foreignKeys = article.getForeignKeys();
        assertEquals(1, foreignKeys.size());
        TableMetaDataColumn foreignKey = foreignKeys.get(0);
        assertSame(articleColor, foreignKey.getTable());
        assertSame(articleColor.getColumn(GeneratorTestUtils.ARTICLE_OID), foreignKey);
        assertSame(article.getColumn(GeneratorTestUtils.OID), foreignKey.getReferencedColumn());
    }

    private void checkPrimaryKeys(TableMetaData table, String expectedPrimaryKeyColumnName) {
        List<TableMetaDataColumn> primaryKeys = table.getPrimaryKeys();
        assertEquals(primaryKeys.toString(), 1, primaryKeys.size());
        TableMetaDataColumn primaryKey = primaryKeys.get(0);
        assertSame(table.getPrimaryKey(), primaryKey);
        assertEquals(expectedPrimaryKeyColumnName, primaryKey.getName());
        assertTrue(primaryKey.isPrimaryKey());

    }

    public void testTableRelationshipProcessorPrimaryKeyPatterns() {
        SchemaMetaData schema = GeneratorTestUtils.createTestSchemaMetaData();
        TableRelationshipSchemaMetaDataProcessor processor = new TableRelationshipSchemaMetaDataProcessor(schema);
        processor.setPrimaryKeyColumnPattern("^ARTICLE_.*");
        processor.processMetaData();
        TableMetaData article = schema.getTable(GeneratorTestUtils.ARTICLE);
        List<TableMetaDataColumn> primaryKeys = article.getPrimaryKeys();
        assertEquals(primaryKeys.toString(), 2, primaryKeys.size());
        TableMetaDataColumn primaryKey = primaryKeys.get(0);
        assertSame(article.getPrimaryKey(), primaryKey);
        assertTrue(primaryKeys.contains(article.getColumn(GeneratorTestUtils.ARTICLE_NO)));
        assertTrue(primaryKey.isPrimaryKey());
        assertTrue(primaryKeys.get(1).isPrimaryKey());

    }
}
