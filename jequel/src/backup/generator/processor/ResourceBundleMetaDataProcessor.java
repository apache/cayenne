package de.jexp.jequel.generator.processor;

import de.jexp.jequel.generator.data.*;
import de.jexp.jequel.util.CollectionUtils;

import java.util.Collection;
import java.util.ResourceBundle;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 20.10.2007 01:40:33
 */
public class ResourceBundleMetaDataProcessor extends SchemaMetaDataProcessor {
    private ResourceBundle resourceBundle;
    private String localeString;
    private Collection<String> resourceBundleKeys;

    public ResourceBundleMetaDataProcessor(final SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public void setResourceBundle(final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public void processMetaData() {
        localeString = " (" + resourceBundle.getLocale().toString() + ")";
        resourceBundleKeys = CollectionUtils.asCollection(resourceBundle.getKeys());
        schemaMetaData.iterateAllColumns(new TableMetaDataIteratorCallback() {
            public void startTable(final TableMetaData table) {
                addRemarkIfFound(table.getName(), table);
            }

            public void forColumn(final TableMetaData table, final TableMetaDataColumn column) {
                addRemarkIfFound(column.getName(), column);
            }
        });
    }

    protected void addRemarkIfFound(final String columnName, final MetaDataElement element) {
        if (resourceBundleKeys.contains(columnName)) {
            element.addRemark(resourceBundle.getString(columnName) + localeString);
        }
    }
}
