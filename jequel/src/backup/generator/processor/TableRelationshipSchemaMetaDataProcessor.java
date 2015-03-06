package de.jexp.jequel.generator.processor;

import de.jexp.jequel.generator.data.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mh14 @ jexp.de
 * @since 23.10.2007 22:17:50 (c) 2007 jexp.de
 *        resolves foreign keys of tables
 */
public class TableRelationshipSchemaMetaDataProcessor extends SchemaMetaDataProcessor {
    private Pattern primaryKeyColumnPattern = Pattern.compile("^OID$");
    private Pattern foreignKeyColumnPattern = Pattern.compile("^(.+)_(OID)$");

    public TableRelationshipSchemaMetaDataProcessor(final SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public void processMetaData() {
        getSchemaMetaData().iterateAllColumns(new TableMetaDataIteratorCallback() {
            public void forColumn(final TableMetaData table, final TableMetaDataColumn column) {
                final String columnName = column.getName();
                if (primaryKeyColumnPattern.matcher(columnName).matches()) {
                    column.setPrimaryKey();
                }
                final Matcher matcher = foreignKeyColumnPattern.matcher(columnName);
                if (matcher.matches()) {
                    final String foreignTableName = matcher.group(1);
                    final String foreignPkColumnName = matcher.group(2);
                    final TableMetaData referencedTable = schemaMetaData.getTable(foreignTableName);
                    if (referencedTable != null) {
                        column.setReferencedTable(referencedTable);
                        referencedTable.addReference(column);
                        final TableMetaDataColumn pkColumn = referencedTable.getColumn(foreignPkColumnName);
                        if (pkColumn != null) {
                            column.setReferencedColumn(pkColumn);
                        }
                    }
                }
            }
        });
    }

    public String getPrimaryKeyColumnPattern() {
        return primaryKeyColumnPattern.pattern();
    }

    public void setPrimaryKeyColumnPattern(final String primaryKeyColumnPattern) {
        this.primaryKeyColumnPattern = Pattern.compile(primaryKeyColumnPattern);
    }

    public String getForeignKeyColumnPattern() {
        return foreignKeyColumnPattern.pattern();
    }

    public void setForeignKeyColumnPattern(final String foreignKeyColumnPattern) {
        this.foreignKeyColumnPattern = Pattern.compile(foreignKeyColumnPattern);
    }
}
