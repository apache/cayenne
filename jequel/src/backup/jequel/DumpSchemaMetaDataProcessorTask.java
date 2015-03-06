package org.apache.tools.ant.taskdefs.optional.jequel;

import de.jexp.jequel.generator.processor.DumpSchemaMetaDataProcessor;

/**
 * @author mh14 @ jexp.de
 * @since 22.10.2007 22:40:21 (c) 2007 jexp.de
 */
public class DumpSchemaMetaDataProcessorTask extends SchemaMetaDataProcessorTask<DumpSchemaMetaDataProcessor> {
    public DumpSchemaMetaDataProcessorTask() {
        setMetaDataProcessorClass(DumpSchemaMetaDataProcessor.class);
    }
}
