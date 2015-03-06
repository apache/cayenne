package de.jexp.jequel.generator.data;

/**
 * @author mh14 @ jexp.de
 * @since 21.10.2007 17:24:42 (c) 2007 jexp.de
 */
public abstract class SchemaMetaDataProcessor implements MetaDataProcessor {
    protected final SchemaMetaData schemaMetaData;

    public SchemaMetaDataProcessor(final SchemaMetaData schemaMetaData) {
        this.schemaMetaData = schemaMetaData;
    }

    public SchemaMetaData getSchemaMetaData() {
        return schemaMetaData;
    }

}
