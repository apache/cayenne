package de.jexp.jequel.generator.data;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * Internal Iterator for TableModel/Column Metadata
 * @since 20.10.2007 19:09:33
 */
public abstract class TableMetaDataIteratorCallback {
    public void startTable(final TableMetaData table) {
    }

    public void forColumn(final TableMetaData table, final TableMetaDataColumn column) {
    }

    public void endTable(final TableMetaData table) {
    }
}
