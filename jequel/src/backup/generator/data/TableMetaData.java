package de.jexp.jequel.generator.data;

import java.util.*;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 19.10.2007 01:20:41
 */
public class TableMetaData extends MetaDataElement {
    private final Map<String, TableMetaDataColumn> columns = new LinkedHashMap<String, TableMetaDataColumn>();
    private final SchemaMetaData schema;

    private List<TableMetaDataColumn> foreignKeys = Collections.emptyList();

    public TableMetaData(final String tableName, final SchemaMetaData schema) {
        super(tableName);
        this.schema = schema;
    }

    public SchemaMetaData getSchema() {
        return schema;
    }

    public TableMetaDataColumn addColumn(final String name, final int jdbcType) {
        final TableMetaDataColumn column = new TableMetaDataColumn(name, jdbcType, this);
        columns.put(name, column);
        return column;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public Collection<String> getColumnNames() {
        return columns.keySet();
    }

    public int getColumnType(final String columnName) {
        return getColumn(columnName).getJdbcType();
    }

    public boolean hasColumn(final String columnName) {
        return columns.containsKey(columnName);
    }

    public Collection<TableMetaDataColumn> getColumns() {
        return columns.values();
    }

    public TableMetaDataColumn getColumn(final String columnName) {
        return columns.get(columnName);
    }

    public List<TableMetaDataColumn> getPrimaryKeys() {
        final List<TableMetaDataColumn> result = new ArrayList<TableMetaDataColumn>(3);
        for (final TableMetaDataColumn column : columns.values()) {
            if (column.isPrimaryKey()) {
                result.add(column);
            }
        }
        return result;
    }

    public void addReference(final TableMetaDataColumn foreignKeyColumn) {
        if (foreignKeys == Collections.EMPTY_LIST) {
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
        final List<TableMetaDataColumn> list = getPrimaryKeys();
        if (list.isEmpty()) return null;
        return list.get(0);
    }
}