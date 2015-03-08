package de.jexp.jequel.ant;

import de.jexp.jequel.generator.JavaFileGenerationProcessor;
import de.jexp.jequel.generator.data.SchemaMetaData;

/**
 * @author mh14 @ jexp.de
 * @since 22.10.2007 21:51:35 (c) 2007 jexp.de
 */
public class GeneratorSchemaMetaDataProcessorTask extends SchemaMetaDataProcessorTask<JavaFileGenerationProcessor> {
    private boolean multFile;
    private String basePath;
    private String javaPackage;
    private String javaClassName;

    public GeneratorSchemaMetaDataProcessorTask() {
        setMetaDataProcessorClass(JavaFileGenerationProcessor.class);
    }

    protected JavaFileGenerationProcessor createProcessor(final Class<JavaFileGenerationProcessor> metaDataProcessorClass, final SchemaMetaData schemaMetaData) {
        final JavaFileGenerationProcessor fileGenerationProcessor = super.createProcessor(metaDataProcessorClass, schemaMetaData);
        fileGenerationProcessor.setBasePath(getBasePath());
        fileGenerationProcessor.setJavaPackage(getJavaPackage());
        fileGenerationProcessor.setJavaClassName(getJavaClassName());
        fileGenerationProcessor.setMultiFile(isMultFile());
        return fileGenerationProcessor;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getJavaPackage() {
        return javaPackage;
    }

    public String getJavaClassName() {
        return javaClassName;
    }

    public void setBasePath(final String basePath) {
        this.basePath = basePath;
    }

    public void setJavaPackage(final String javaPackage) {
        this.javaPackage = javaPackage;
    }

    public void setJavaClassName(final String javaClassName) {
        this.javaClassName = javaClassName;
    }

    public boolean isMultFile() {
        return multFile;
    }

    public void setMultFile(final boolean multFile) {
        this.multFile = multFile;
    }
}
