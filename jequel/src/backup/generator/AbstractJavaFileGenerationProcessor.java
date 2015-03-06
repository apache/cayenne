package de.jexp.jequel.generator;

import de.jexp.jequel.generator.data.*;
import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * @author mh14 @ jexp.de
 * @since 27.10.2007 23:04:48 (c) 2007 jexp.de
 */
public abstract class AbstractJavaFileGenerationProcessor extends SchemaMetaDataProcessor {
    private static final String HEADER =
            "package %1$s;\n" +
                    "\n" +
                    "import " + Field.class.getName() + ";\n" +
                    "import " + BaseTable.class.getName() + ";\n" +
                    "import " + BigDecimal.class.getName() + ";\n" +
                    "import " + Date.class.getName() + ";\n" +
                    "import " + Timestamp.class.getName() + ";\n" +
                    "\n" +
                    "/**\n" +
                    " * @author %2$s\n" +
                    " * @since %4$tc\n" +
                    " * %3$s \n" +
                    " */\n\n";
    protected static final String FOOTER = "}\n";
    private String javaClassName;
    private String basePath;
    private String javaPackage;

    protected AbstractJavaFileGenerationProcessor(final SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public String getHeader(final String javaPackage, final String comment, final String author) {
        return String.format(HEADER, javaPackage, author, comment, new java.util.Date());
    }

    protected String createTableInstanceVariable(final TableMetaData table) {
        return String.format("%2$s\n" +
                "public final static %1$s %1$s = new %1$s();\n", table.getName(), createComment(table));
    }

    public String createColumnsSource(final TableMetaData table) {
        final StringBuilder columnsSource = new StringBuilder();
        for (final TableMetaDataColumn column : table.getColumns()) {
            if (!validColumn(column)) continue;
            columnsSource.append(createComment(column));
            columnsSource.append(createColumnSource(column));
        }
        return columnsSource.toString();
    }

    private boolean validColumn(final TableMetaDataColumn column) {
        return column.getName().matches("[A-Z_]+");
    }

    public String createComment(final MetaDataElement element) {
        if (!element.hasRemark()) return "";
        String remark = element.getRemark();
        remark = remark.replaceFirst("deprecated", "@deprecated");
        if (remark.trim().length() == 0) return "";
        return String.format("/** %s */\n", remark);
    }

    private String createComment(final TableMetaDataColumn column) {
        String remark = column.hasRemark() ? column.getRemark() : "";
        if (column.isPrimaryKey()) {
            remark += addTableRemark("PK: ", column.getTable());
        }
        if (column.isForeignKey()) {
            remark += addTableRemark("FK: ", column.getReferencedTable());
        }
        if (remark.trim().length() == 0) return "";
        remark = remark.replaceFirst("deprecated", "@deprecated");
        return String.format("/** %s */\n", remark);
    }

    protected String addTableRemark(final String prefix, final TableMetaData table) {
        final String remark = prefix + table.getName();
        if (table.hasRemark()) {
            return remark + "; " + table.getRemark();
        } else {
            return remark;
        }
    }

    public String createColumnSource(final TableMetaDataColumn column) {
        return String.format(
                "    public final Field %s = %s;\n",
                column.getName(), getColumnCreationMethod(column)
        );
    }

    public String getColumnCreationMethod(final TableMetaDataColumn column) {

        String columnCreationMethod = getColumnCreationMethodByType(column);
        if (column.isForeignKey()) {
            final TableMetaData referencedTable = column.getReferencedTable();
            if (referencedTable != null) {
                final TableMetaDataColumn pkColumn;
                if (column.getReferencedColumn() != null) pkColumn = column.getReferencedColumn();
                else pkColumn = referencedTable.getPrimaryKey();
                columnCreationMethod = createForeignKeyColumn(referencedTable, pkColumn);
            }
        }
        if (column.isPrimaryKey()) columnCreationMethod += ".primaryKey()";
        return columnCreationMethod;
    }

    protected String createForeignKeyColumn(final TableMetaData referencedTable, final TableMetaDataColumn pkColumn) {
        return "foreignKey(" + javaClassName + "." + referencedTable.getName() + ".class,\"" + pkColumn.getName() + "\")";
    }

    protected String getColumnCreationMethodByType(final TableMetaDataColumn column) {
        final Class columnClass = column.getJavaClass() != null ? column.getJavaClass() : ColumnTypeHandler.getJavaType(column.getJdbcType());
        if (columnClass == String.class) return "string()";
        if (columnClass == Integer.class) return "integer()";
        if (columnClass == BigDecimal.class) return "numeric()";
        if (columnClass == Boolean.class) return "bool()";
        if (columnClass == Date.class) return "date()";
        if (columnClass == Timestamp.class) return "timestamp()";
        return String.format("field(%s.class)", columnClass.getSimpleName());
    }

    public void setBasePath(final String basePath) {
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setJavaPackage(final String javaPackage) {
        this.javaPackage = javaPackage;
    }

    public String getJavaPackage() {
        return javaPackage;
    }

    public void setJavaClassName(final String javaClassName) {
        this.javaClassName = javaClassName;
    }

    public String getJavaClassName() {
        return javaClassName != null ? javaClassName : schemaMetaData.getSchema();
    }

    protected String getSchemaComment() {
        if (schemaMetaData.hasRemark()) return "Generated from: " + schemaMetaData.getRemark();
        else return "Generated from: " + schemaMetaData.getSchema();
    }

    protected String getAuthor() {
        return getClass().getName();
    }
}
