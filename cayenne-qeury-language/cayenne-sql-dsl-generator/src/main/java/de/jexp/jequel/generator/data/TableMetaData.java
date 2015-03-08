package de.jexp.jequel.generator.data;

import java.util.*;

public class TableMetaData extends MetaDataElement {
    private final Map<String, TableMetaDataColumn> columns = new LinkedHashMap<String, TableMetaDataColumn>();
    private final SchemaMetaData schema;

    private List<TableMetaDataColumn> foreignKeys = Collections.emptyList();

    public TableMetaData(String tableName, SchemaMetaData schema) {
        super(tableName);
        this.schema = schema;
    }

    public SchemaMetaData getSchema() {
        return schema;
    }

    public TableMetaDataColumn addColumn(String name, int jdbcType) {
        TableMetaDataColumn column = new TableMetaDataColumn(name, jdbcType, this);
        columns.put(name, column);
        return column;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public Collection<String> getColumnNames() {
        return columns.keySet();
    }

    public int getColumnType(String columnName) {
        return getColumn(columnName).getJdbcType();
    }

    public boolean hasColumn(String columnName) {
        return columns.containsKey(columnName);
    }

    public Collection<TableMetaDataColumn> getColumns() {
        return columns.values();
    }

    public TableMetaDataColumn getColumn(String columnName) {
        return columns.get(columnName);
    }

    public List<TableMetaDataColumn> getPrimaryKeys() {
        List<TableMetaDataColumn> result = new ArrayList<TableMetaDataColumn>(3);
        for (TableMetaDataColumn column : columns.values()) {
            if (column.isPrimaryKey()) {
                result.add(column);
            }
        }
        return result;
    }

    public void addReference(TableMetaDataColumn foreignKeyColumn) {
        if (foreignKeys.isEmpty()) {
            foreignKeys = new LinkedList<TableMetaDataColumn>();
        }
        foreignKeys.add(foreignKeyColumn);
    }

    public List<TableMetaDataColumn> getForeignKeys() {
        return foreignKeys;
    }

    public String toString() {
        return getName();
    }

    public TableMetaDataColumn getPrimaryKey() {
        List<TableMetaDataColumn> list = getPrimaryKeys();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
}