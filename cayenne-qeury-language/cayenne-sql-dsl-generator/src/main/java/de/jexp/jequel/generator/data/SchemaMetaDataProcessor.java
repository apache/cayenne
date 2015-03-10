package de.jexp.jequel.generator.data;

public abstract class SchemaMetaDataProcessor {
    private final SchemaMetaData schemaMetaData;

    protected SchemaMetaDataProcessor(SchemaMetaData schemaMetaData) {
        this.schemaMetaData = schemaMetaData;
    }

    public SchemaMetaData getSchemaMetaData() {
        return schemaMetaData;
    }

    public abstract void processMetaData();

}
