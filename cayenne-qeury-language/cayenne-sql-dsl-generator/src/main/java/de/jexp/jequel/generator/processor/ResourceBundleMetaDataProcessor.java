package de.jexp.jequel.generator.processor;

import de.jexp.jequel.generator.data.*;
import de.jexp.jequel.util.CollectionUtils;

import java.util.Collection;
import java.util.ResourceBundle;

import static java.util.Collections.list;

public class ResourceBundleMetaDataProcessor extends SchemaMetaDataProcessor {
    private ResourceBundle resourceBundle;
    private String localeString;
    private Collection<String> resourceBundleKeys;

    public ResourceBundleMetaDataProcessor(SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    @Override
    public void processMetaData() {
        localeString = " (" + resourceBundle.getLocale() + ")";
        resourceBundleKeys = list(resourceBundle.getKeys());
        getSchemaMetaData().iterateAllColumns(new TableMetaDataIteratorCallback() {
            @Override
            public void startTable(TableMetaData table) {
                addRemarkIfFound(table.getName(), table);
            }

            @Override
            public void forColumn(TableMetaData table, TableMetaDataColumn column) {
                addRemarkIfFound(column.getName(), column);
            }

            @Override
            public void endTable(TableMetaData table) {

            }
        });
    }

    protected void addRemarkIfFound(String columnName, MetaDataElement element) {
        if (resourceBundleKeys.contains(columnName)) {
            element.addRemark(resourceBundle.getString(columnName) + localeString);
        }
    }
}
