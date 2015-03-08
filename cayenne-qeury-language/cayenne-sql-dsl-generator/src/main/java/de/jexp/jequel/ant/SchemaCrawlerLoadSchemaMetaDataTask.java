package de.jexp.jequel.ant;

import de.jexp.jequel.generator.SchemaCrawlerLoadSchemaMetaDataProcessor;
import de.jexp.jequel.generator.data.SchemaMetaData;

/**
 * Loads the schema metadata from database
 */
public class SchemaCrawlerLoadSchemaMetaDataTask extends SchemaMetaDataProcessorDataSourceTask<SchemaCrawlerLoadSchemaMetaDataProcessor> {
    public SchemaCrawlerLoadSchemaMetaDataTask() {
        setMetaDataProcessorClass(SchemaCrawlerLoadSchemaMetaDataProcessor.class);
    }

    protected SchemaCrawlerLoadSchemaMetaDataProcessor createProcessor(final Class<SchemaCrawlerLoadSchemaMetaDataProcessor> metaDataProcessorClass, final SchemaMetaData schemaMetaData) {
        final SchemaCrawlerLoadSchemaMetaDataProcessor loadSchemaMetaDataProcessor = super.createProcessor(metaDataProcessorClass, schemaMetaData);
        loadSchemaMetaDataProcessor.setHandleForeignKeys(isForeignKeys());
        loadSchemaMetaDataProcessor.setDataSource(getDataSource());
        return loadSchemaMetaDataProcessor;
    }

    private boolean foreignKeys;

    public boolean isForeignKeys() {
        return foreignKeys;
    }

    public void setForeignKeys(final boolean foreignKeys) {
        this.foreignKeys = foreignKeys;
    }
}