package de.jexp.jequel.ant;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.processor.ResourceBundleMetaDataProcessor;

import java.util.Locale;
import java.util.ResourceBundle;

public class ResourceBundleCommentsSchemaMetaDataProcessorTask extends SchemaMetaDataProcessorTask<ResourceBundleMetaDataProcessor> {
    private Locale locale = Locale.getDefault();
    private String resourceBundleBaseName;

    public ResourceBundleCommentsSchemaMetaDataProcessorTask() {
        setMetaDataProcessorClass(ResourceBundleMetaDataProcessor.class);
    }

    protected ResourceBundleMetaDataProcessor createProcessor(Class<ResourceBundleMetaDataProcessor> metaDataProcessorClass, SchemaMetaData schemaMetaData) {
        ResourceBundleMetaDataProcessor resourceBundleMetaDataProcessor = super.createProcessor(metaDataProcessorClass, schemaMetaData);
        ResourceBundle resourceBundle = ResourceBundle.getBundle(getResourceBundleBaseName(), getLocale());
        resourceBundleMetaDataProcessor.setResourceBundle(resourceBundle);
        return resourceBundleMetaDataProcessor;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getResourceBundleBaseName() {
        return resourceBundleBaseName;
    }

    public void setLocale(String localeString) {
        this.locale = new Locale(localeString);
    }

    public void setResourceBundleBaseName(String resourceBundleBaseName) {
        this.resourceBundleBaseName = resourceBundleBaseName;
    }
}