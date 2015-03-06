package org.apache.tools.ant.taskdefs.optional.jequel;

import de.jexp.jequel.generator.processor.OracleCommentsSchemaMetaDataProcessor;

/**
 * @author mh14 @ jexp.de
 * @since 22.10.2007 21:40:04 (c) 2007 jexp.de
 *        loads the schema metadata from database
 */
public class OracleUserCommentsSchemaMetaDataTask extends SchemaMetaDataProcessorDataSourceTask<OracleCommentsSchemaMetaDataProcessor> {
    public OracleUserCommentsSchemaMetaDataTask() {
        setMetaDataProcessorClass(OracleCommentsSchemaMetaDataProcessor.class);
    }
}