package de.jexp.jequel.generator.data;

public class TableMetaDataColumn extends MetaDataElement {
    private final int jdbcType;
    private final TableMetaData table;
    private TableMetaData referencedTable;
    private TableMetaDataColumn referencedColumn;

    private boolean primaryKey;
    private String typeClassName;
    private Class<?> javaClass;

    // TODO getJavaType
    public TableMetaDataColumn(String columnName, int jdbcType, TableMetaData table) {
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

    public void setReferencedTable(TableMetaData referencedTable) {
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
        return getTable().getName() + "." + getName() + (isPrimaryKey() ? " pk " : "") + (isForeignKey() ? " fk " : "");
    }


    public void setReferencedColumn(TableMetaDataColumn referencedColumn) {
        this.referencedColumn = referencedColumn;
        this.referencedTable = referencedColumn.getTable();
    }

    public TableMetaDataColumn getReferencedColumn() {
        return referencedColumn;
    }

    public void setJavaType(String typeClassName) {
        this.typeClassName = typeClassName;
    }

    public String getTypeClassName() {
        return typeClassName;
    }

    public void setJavaClass(String javaClassName) {
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