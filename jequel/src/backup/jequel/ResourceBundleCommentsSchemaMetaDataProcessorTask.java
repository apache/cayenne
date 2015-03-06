package org.apache.tools.ant.taskdefs.optional.jequel;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.processor.ResourceBundleMetaDataProcessor;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author mh14 @ jexp.de
 * @since 22.10.2007 21:51:35 (c) 2007 jexp.de
 */
public class ResourceBundleCommentsSchemaMetaDataProcessorTask extends SchemaMetaDataProcessorTask<ResourceBundleMetaDataProcessor> {
    private Locale locale = Locale.getDefault();
    private String resourceBundleBaseName;

    public ResourceBundleCommentsSchemaMetaDataProcessorTask() {
        setMetaDataProcessorClass(ResourceBundleMetaDataProcessor.class);
    }

    protected ResourceBundleMetaDataProcessor createProcessor(final Class<ResourceBundleMetaDataProcessor> metaDataProcessorClass, final SchemaMetaData schemaMetaData) {
        final ResourceBundleMetaDataProcessor resourceBundleMetaDataProcessor = super.createProcessor(metaDataProcessorClass, schemaMetaData);
        final ResourceBundle resourceBundle = ResourceBundle.getBundle(getResourceBundleBaseName(), getLocale());
        resourceBundleMetaDataProcessor.setResourceBundle(resourceBundle);
        return resourceBundleMetaDataProcessor;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getResourceBundleBaseName() {
        return resourceBundleBaseName;
    }

    public void setLocale(final String localeString) {
        this.locale = new Locale(localeString);
    }

    public void setResourceBundleBaseName(final String resourceBundleBaseName) {
        this.resourceBundleBaseName = resourceBundleBaseName;
    }
}