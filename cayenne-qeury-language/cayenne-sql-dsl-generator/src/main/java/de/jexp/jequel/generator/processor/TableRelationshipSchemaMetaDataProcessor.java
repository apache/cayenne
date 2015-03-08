package de.jexp.jequel.generator.processor;

import de.jexp.jequel.generator.data.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves foreign keys of tables
 */
public class TableRelationshipSchemaMetaDataProcessor extends SchemaMetaDataProcessor {
    private Pattern primaryKeyColumnPattern = Pattern.compile("^OID$");
    private Pattern foreignKeyColumnPattern = Pattern.compile("^(.+)_(OID)$");

    public TableRelationshipSchemaMetaDataProcessor(SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public void processMetaData() {
        getSchemaMetaData().iterateAllColumns(new TableMetaDataIteratorCallback() {

            @Override
            public void startTable(TableMetaData table) {

            }

            @Override
            public void forColumn(TableMetaData table, TableMetaDataColumn column) {
                String columnName = column.getName();
                if (primaryKeyColumnPattern.matcher(columnName).matches()) {
                    column.setPrimaryKey();
                }
                Matcher matcher = foreignKeyColumnPattern.matcher(columnName);
                if (matcher.matches()) {
                    String foreignTableName = matcher.group(1);
                    String foreignPkColumnName = matcher.group(2);
                    TableMetaData referencedTable = getSchemaMetaData().getTable(foreignTableName);
                    if (referencedTable != null) {
                        column.setReferencedTable(referencedTable);
                        referencedTable.addReference(column);
                        TableMetaDataColumn pkColumn = referencedTable.getColumn(foreignPkColumnName);
                        if (pkColumn != null) {
                            column.setReferencedColumn(pkColumn);
                        }
                    }
                }
            }

            @Override
            public void endTable(TableMetaData table) {

            }
        });
    }

    public String getPrimaryKeyColumnPattern() {
        return primaryKeyColumnPattern.pattern();
    }

    public void setPrimaryKeyColumnPattern(String primaryKeyColumnPattern) {
        this.primaryKeyColumnPattern = Pattern.compile(primaryKeyColumnPattern);
    }

    public String getForeignKeyColumnPattern() {
        return foreignKeyColumnPattern.pattern();
    }

    public void setForeignKeyColumnPattern(String foreignKeyColumnPattern) {
        this.foreignKeyColumnPattern = Pattern.compile(foreignKeyColumnPattern);
    }
}
