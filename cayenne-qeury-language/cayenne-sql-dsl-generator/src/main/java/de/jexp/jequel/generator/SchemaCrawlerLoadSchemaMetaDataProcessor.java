package de.jexp.jequel.generator;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.SchemaMetaDataProcessor;
import de.jexp.jequel.generator.data.TableMetaData;
import de.jexp.jequel.generator.data.TableMetaDataColumn;
import schemacrawler.crawl.*;
import schemacrawler.schema.*;

import javax.sql.DataSource;

public class SchemaCrawlerLoadSchemaMetaDataProcessor extends SchemaMetaDataProcessor {
    private DataSource dataSource;
    private boolean handleForeignKeys;
    private String includeTablesPattern = "^[A-Z_]+$";
    private String excludesTablesPattern = "^(_|SYS|EX|PM|TMP|(CDI|MGMT|DBA|USER|CI|SDD|ALL|RM|TMP|TEST|DWH|SAV|SAVE)_).+$";

    public SchemaCrawlerLoadSchemaMetaDataProcessor() {
        this((String) null);
    }

    public SchemaCrawlerLoadSchemaMetaDataProcessor(final String schema) {
        super(new SchemaMetaData(schema));
    }

    public SchemaCrawlerLoadSchemaMetaDataProcessor(final SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public void loadMetaData() {
        processMetaData();
    }

    public void processMetaData() {
        final Schema schema = retrieveSchema(dataSource);
        loadSchemaMetaData(schema);
        if (isHandleForeignKeys())
            assignForeignKeys(schema);
    }

    protected Schema retrieveSchema(final DataSource dataSource) {
        try {
            final SchemaCrawlerOptions schemaCrawlerOptions = new SchemaCrawlerOptions();
            schemaCrawlerOptions.setSchemaPattern(getSchemaMetaData().getName());
            schemaCrawlerOptions.setTableInclusionRule(new InclusionRule(includeTablesPattern, excludesTablesPattern));
            final SchemaInfoLevel schemaInfoLevel = new SchemaInfoLevel(SchemaInfoLevel.basic);
            if (isHandleForeignKeys())
                schemaInfoLevel.setRetrieveForeignKeys(true);
            return SchemaCrawler.getSchema(dataSource, schemaInfoLevel, schemaCrawlerOptions);
        } catch (SchemaCrawlerException e) {
            throw new RuntimeException("Error retrieving schema information", e);
        }
    }

    protected Schema loadSchemaMetaData(final Schema schema) {
        schemaMetaData.setRemark(schema.getDatabaseInfo().toString() + schema.getRemarks());
        for (final Table table : schema.getTables()) {
            final TableMetaData tableMetaData = new TableMetaData(table.getName(), schemaMetaData);
            tableMetaData.setRemark(table.getRemarks());
            schemaMetaData.addTable(tableMetaData);
            for (final Column column : table.getColumns()) {
                final ColumnDataType columnDataType = column.getType();
                final TableMetaDataColumn metaDataColumn = tableMetaData.addColumn(column.getName(), columnDataType.getType());
                final String typeClassName = column.getType().getTypeClassName();
                if (typeClassName != null)
                    metaDataColumn.setJavaClass(typeClassName);
                metaDataColumn.setRemark(column.getRemarks());
                if (column.isPartOfPrimaryKey())
                    metaDataColumn.setPrimaryKey();
                metaDataColumn.setJavaType(columnDataType.getTypeClassName());
            }
        }
        return schema;
    }

    protected void assignForeignKeys(final Schema schema) {
        for (final Table table : schema.getTables()) {
            for (final Column column : table.getColumns()) {
                if (column.isPartOfForeignKey()) {
                    final Column primaryKeyColumn = column.getReferencedColumn();
                    final NamedObject primaryKeyTable = primaryKeyColumn.getParent();
                    final TableMetaData primaryKeyTableMetaData = schemaMetaData.getTable(primaryKeyTable.getName());
                    final TableMetaDataColumn primaryKeyColumnMetaData = primaryKeyTableMetaData.getColumn(primaryKeyColumn.getName());
                    final TableMetaData foreignKeyTableMetaData = schemaMetaData.getTable(table.getName());
                    final TableMetaDataColumn foreignKeyColumnMetaData = foreignKeyTableMetaData.getColumn(column.getName());
                    foreignKeyColumnMetaData.setReferencedColumn(primaryKeyColumnMetaData);
                    primaryKeyTableMetaData.addReference(foreignKeyColumnMetaData);
                }
            }
        }
    }

    public void setDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isHandleForeignKeys() {
        return handleForeignKeys;
    }

    public void setHandleForeignKeys(final boolean handleForeignKeys) {
        this.handleForeignKeys = handleForeignKeys;
    }

    public String getIncludeTablesPattern() {
        return includeTablesPattern;
    }

    public void setIncludeTablesPattern(final String includeTablesPattern) {
        this.includeTablesPattern = includeTablesPattern;
    }

    public String getExcludesTablesPattern() {
        return excludesTablesPattern;
    }

    public void setExcludesTablesPattern(final String excludesTablesPattern) {
        this.excludesTablesPattern = excludesTablesPattern;
    }
}