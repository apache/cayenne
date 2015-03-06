package de.jexp.jequel.generator.data;

/**
 * Created: mhu@salt-solutions.de 19.10.2007 16:05:42
 * (c) Salt Solutions GmbH 2006
 */
public class TableMetaDataColumn extends MetaDataElement {
    private final int jdbcType;
    private final TableMetaData table;
    private TableMetaData referencedTable;
    private TableMetaDataColumn referencedColumn;

    private boolean primaryKey;
    private String typeClassName;
    private Class<?> javaClass;

    // TODO getJavaType
    public TableMetaDataColumn(final String columnName, final int jdbcType, final TableMetaData table) {
        super(columnName);
        this.jdbcType = jdbcType;
        this.table = table;
    }

    public TableMetaData getTable() {
        return table;
    }

    public int getJdbcType() {
        return jdbcType;
    }

    public void setReferencedTable(final TableMetaData referencedTable) {
        this.referencedTable = referencedTable;
    }

    public TableMetaData getReferencedTable() {
        return referencedTable;
    }

    public boolean isForeignKey() {
        return referencedTable != null;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey() {
        this.primaryKey = true;
    }

    public String toString() {
        return getTable().getName() + "." + name + (isPrimaryKey() ? " pk " : "") + (isForeignKey() ? " fk " : "");
    }


    public void setReferencedColumn(final TableMetaDataColumn referencedColumn) {
        this.referencedColumn = referencedColumn;
        this.referencedTable = referencedColumn.getTable();
    }

    public TableMetaDataColumn getReferencedColumn() {
        return referencedColumn;
    }

    public void setJavaType(final String typeClassName) {
        this.typeClassName = typeClassName;
    }

    public String getTypeClassName() {
        return typeClassName;
    }

    public void setJavaClass(final String javaClassName) {
        try {
            this.javaClass = Class.forName(javaClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Class<?> getJavaClass() {
        return javaClass;
    }
}