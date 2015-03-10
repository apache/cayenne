package de.jexp.jequel.ant;

import de.jexp.jequel.generator.JavaFileGenerationProcessor;
import de.jexp.jequel.generator.data.SchemaMetaData;

public class GeneratorSchemaMetaDataProcessorTask extends SchemaMetaDataProcessorTask<JavaFileGenerationProcessor> {
    private String basePath;
    private String javaPackage;
    private String javaClassName;

    public GeneratorSchemaMetaDataProcessorTask() {
        setMetaDataProcessorClass(JavaFileGenerationProcessor.class);
    }

    protected JavaFileGenerationProcessor createProcessor(Class<JavaFileGenerationProcessor> metaDataProcessorClass, SchemaMetaData schemaMetaData) {
        JavaFileGenerationProcessor fileGenerationProcessor = super.createProcessor(metaDataProcessorClass, schemaMetaData);
        fileGenerationProcessor.setBasePath(getBasePath());
        fileGenerationProcessor.setJavaPackage(getJavaPackage());
        fileGenerationProcessor.setJavaClassName(getJavaClassName());
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

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void setJavaPackage(String javaPackage) {
        this.javaPackage = javaPackage;
    }

    public void setJavaClassName(String javaClassName) {
        this.javaClassName = javaClassName;
    }
}
