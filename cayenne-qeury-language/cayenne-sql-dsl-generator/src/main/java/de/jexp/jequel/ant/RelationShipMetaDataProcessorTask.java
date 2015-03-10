package de.jexp.jequel.ant;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.processor.TableRelationshipSchemaMetaDataProcessor;

public class RelationShipMetaDataProcessorTask extends SchemaMetaDataProcessorTask<TableRelationshipSchemaMetaDataProcessor> {
    private String primaryKeyPattern;
    private String foreignKeyPattern;

    public RelationShipMetaDataProcessorTask() {
        setMetaDataProcessorClass(TableRelationshipSchemaMetaDataProcessor.class);
    }

    protected TableRelationshipSchemaMetaDataProcessor createProcessor(Class<TableRelationshipSchemaMetaDataProcessor> metaDataProcessorClass, SchemaMetaData schemaMetaData) {
        TableRelationshipSchemaMetaDataProcessor tableRelationshipSchemaMetaDataProcessor = super.createProcessor(metaDataProcessorClass, schemaMetaData);
        String primaryKeyPattern = getPrimaryKeyPattern();
        if (primaryKeyPattern != null) {
            tableRelationshipSchemaMetaDataProcessor.setPrimaryKeyColumnPattern(primaryKeyPattern);
        }

        String foreignKeyPattern = getForeignKeyPattern();
        if (foreignKeyPattern != null) {
            tableRelationshipSchemaMetaDataProcessor.setForeignKeyColumnPattern(foreignKeyPattern);
        }
        return tableRelationshipSchemaMetaDataProcessor;
    }

    public String getPrimaryKeyPattern() {
        return primaryKeyPattern;
    }

    public String getForeignKeyPattern() {
        return foreignKeyPattern;
    }

    public void setPrimaryKeyPattern(String primaryKeyPattern) {
        this.primaryKeyPattern = primaryKeyPattern;
    }

    public void setForeignKeyPattern(String foreignKeyPattern) {
        this.foreignKeyPattern = foreignKeyPattern;
    }
}