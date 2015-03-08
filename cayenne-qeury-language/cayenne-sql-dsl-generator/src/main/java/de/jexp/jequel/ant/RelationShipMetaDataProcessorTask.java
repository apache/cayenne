package de.jexp.jequel.ant;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.processor.TableRelationshipSchemaMetaDataProcessor;

/**
 * @author mh14 @ jexp.de
 * @since 22.10.2007 22:40:21 (c) 2007 jexp.de
 */
public class RelationShipMetaDataProcessorTask extends SchemaMetaDataProcessorTask<TableRelationshipSchemaMetaDataProcessor> {
    private String primaryKeyPattern;
    private String foreignKeyPattern;

    public RelationShipMetaDataProcessorTask() {
        setMetaDataProcessorClass(TableRelationshipSchemaMetaDataProcessor.class);
    }

    protected TableRelationshipSchemaMetaDataProcessor createProcessor(final Class<TableRelationshipSchemaMetaDataProcessor> metaDataProcessorClass, final SchemaMetaData schemaMetaData) {
        final TableRelationshipSchemaMetaDataProcessor tableRelationshipSchemaMetaDataProcessor = super.createProcessor(metaDataProcessorClass, schemaMetaData);
        final String primaryKeyPattern = getPrimaryKeyPattern();
        if (primaryKeyPattern != null)
            tableRelationshipSchemaMetaDataProcessor.setPrimaryKeyColumnPattern(primaryKeyPattern);
        final String foreignKeyPattern = getForeignKeyPattern();
        if (foreignKeyPattern != null)
            tableRelationshipSchemaMetaDataProcessor.setForeignKeyColumnPattern(foreignKeyPattern);
        return tableRelationshipSchemaMetaDataProcessor;
    }

    public String getPrimaryKeyPattern() {
        return primaryKeyPattern;
    }

    public String getForeignKeyPattern() {
        return foreignKeyPattern;
    }

    public void setPrimaryKeyPattern(final String primaryKeyPattern) {
        this.primaryKeyPattern = primaryKeyPattern;
    }

    public void setForeignKeyPattern(final String foreignKeyPattern) {
        this.foreignKeyPattern = foreignKeyPattern;
    }
}