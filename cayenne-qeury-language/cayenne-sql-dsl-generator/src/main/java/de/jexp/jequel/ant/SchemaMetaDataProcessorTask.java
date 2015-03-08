package de.jexp.jequel.ant;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.SchemaMetaDataProcessor;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.lang.reflect.Constructor;

/**
 * @author mh14 @ jexp.de
 * @since 22.10.2007 21:09:17 (c) 2007 jexp.de
 *        Encapsulates MetaDataProcessor as Ant Task
 */
public class SchemaMetaDataProcessorTask<T extends SchemaMetaDataProcessor> extends Task {
    private Class<T> metaDataProcessorClass;

    private T schemaMetaDataProcessor;

    public void setMetaDataProcessorClass(final Class<T> metaDataProcessorClass) {
        this.metaDataProcessorClass = metaDataProcessorClass;
    }

    protected T createProcessor(final Class<T> metaDataProcessorClass, final SchemaMetaData schemaMetaData) {
        try {
            final Constructor<T> constructor = metaDataProcessorClass.getDeclaredConstructor(SchemaMetaData.class);
            return constructor.newInstance(schemaMetaData);
        } catch (Exception e) {
            throw new RuntimeException("Error creating SchemaMetaDataProcessor " + metaDataProcessorClass, e);
        }
    }

    public void execute() throws BuildException {
        if (metaDataProcessorClass == null) throw new BuildException("No ProcessorClass set");
        if (schemaMetaDataProcessor == null) { // install with default
            schemaMetaDataProcessor = createProcessor(metaDataProcessorClass, new SchemaMetaData());
        }
        schemaMetaDataProcessor.processMetaData();
    }

    void setSchemaMetaData(final SchemaMetaData schemaMetaData) {
        schemaMetaDataProcessor = createProcessor(metaDataProcessorClass, schemaMetaData);
    }

    SchemaMetaData getSchemaMetaData() {
        return schemaMetaDataProcessor.getSchemaMetaData();
    }


}
