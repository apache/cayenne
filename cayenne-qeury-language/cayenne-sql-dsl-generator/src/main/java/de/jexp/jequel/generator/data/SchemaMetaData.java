package de.jexp.jequel.generator.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * encapsulates the table metadata read from a database schema
 */
public class SchemaMetaData extends MetaDataElement {
    private static final long serialVersionUID = -1136113165893009076L;
    private final Map<String, TableMetaData> schemaMetaData = new HashMap<String, TableMetaData>();

    public SchemaMetaData(Map<String, TableMetaData> schemaMetaData) {
        this();
        this.schemaMetaData.putAll(schemaMetaData);
    }

    public SchemaMetaData() {
        this((String) null);
    }

    public SchemaMetaData(String schema) {
        super(schema);
    }

    public TableMetaData getTable(String tableName) {
        return schemaMetaData.get(tableName.toUpperCase());
    }

    public Collection<String> getTableNames() {
        return schemaMetaData.keySet();
    }

    public TableMetaData addTable(TableMetaData tableMetaData) {
        schemaMetaData.put(tableMetaData.getName(), tableMetaData);
        return tableMetaData;
    }

    public String getSchema() {
        return getName();
    }

    public Collection<TableMetaData> getTables() {
        return schemaMetaData.values();
    }

    public void iterateAllColumns(TableMetaDataIteratorCallback tableColumnCallback) {
        for (TableMetaData table : getTables()) {
            tableColumnCallback.startTable(table);
            for (TableMetaDataColumn column : table.getColumns()) {
                tableColumnCallback.forColumn(table, column);
            }
            tableColumnCallback.endTable(table);
        }
    }

    public TableMetaData addTable(String tableName) {
        return addTable(new TableMetaData(tableName, this));
    }
}
