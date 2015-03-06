package de.jexp.jequel.generator.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @since 21.10.2007 01:10:18 (c) 2007 jexp.de
 *        encapsulates the table metadata read from a database schema
 */
public class SchemaMetaData extends MetaDataElement {
    final static long serialVersionUID = -1136113165893009076L;
    private final Map<String, TableMetaData> schemaMetaData = new HashMap<String, TableMetaData>();

    public SchemaMetaData(final Map<String, TableMetaData> schemaMetaData) {
        this();
        this.schemaMetaData.putAll(schemaMetaData);
    }

    public SchemaMetaData() {
        this((String) null);
    }

    public SchemaMetaData(final String schema) {
        super(schema);
    }

    public TableMetaData getTable(final String tableName) {
        return schemaMetaData.get(tableName.toUpperCase());
    }

    public Collection<String> getTableNames() {
        return schemaMetaData.keySet();
    }

    public TableMetaData addTable(final TableMetaData tableMetaData) {
        schemaMetaData.put(tableMetaData.getName(), tableMetaData);
        return tableMetaData;
    }

    public String getSchema() {
        return getName();
    }

    public Collection<TableMetaData> getTables() {
        return schemaMetaData.values();
    }

    public void iterateAllColumns(final TableMetaDataIteratorCallback tableColumnCallback) {
        for (final TableMetaData table : getTables()) {
            tableColumnCallback.startTable(table);
            for (final TableMetaDataColumn column : table.getColumns()) {
                tableColumnCallback.forColumn(table, column);
            }
            tableColumnCallback.endTable(table);
        }
    }

    public TableMetaData addTable(final String tableName) {
        return addTable(new TableMetaData(tableName, this));
    }
}
