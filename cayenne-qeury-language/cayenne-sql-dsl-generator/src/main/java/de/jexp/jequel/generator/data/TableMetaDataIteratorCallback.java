package de.jexp.jequel.generator.data;

/**
 * Internal Iterator for TableModel/Column Metadata
 */
public interface TableMetaDataIteratorCallback {
    void startTable(TableMetaData table);

    void forColumn(TableMetaData table, TableMetaDataColumn column);

    void endTable(TableMetaData table);
}
