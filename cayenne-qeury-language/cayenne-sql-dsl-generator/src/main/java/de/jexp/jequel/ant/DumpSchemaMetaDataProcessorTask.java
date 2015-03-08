package de.jexp.jequel.ant;

import de.jexp.jequel.generator.processor.DumpSchemaMetaDataProcessor;

public class DumpSchemaMetaDataProcessorTask extends SchemaMetaDataProcessorTask<DumpSchemaMetaDataProcessor> {
    public DumpSchemaMetaDataProcessorTask() {
        setMetaDataProcessorClass(DumpSchemaMetaDataProcessor.class);
    }
}
