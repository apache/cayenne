package de.jexp.jequel.generator;

import de.jexp.jequel.generator.data.MetaDataElement;
import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.SchemaMetaDataProcessor;
import de.jexp.jequel.generator.data.TableMetaData;
import de.jexp.jequel.generator.data.TableMetaDataColumn;
import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;

import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class AbstractJavaFileGenerationProcessor extends SchemaMetaDataProcessor {
    private static final String HEADER =
            "package %1$s;\n" +
                    "\n" +
                    "import " + Field.class.getName() + ";\n" +
                    "import " + BaseTable.class.getName() + ";\n" +
                    "import " + BigDecimal.class.getName() + ";\n" +
                    "import " + Date.class.getName() + ";\n" +
                    "import " + Timestamp.class.getName() + ";\n" +
                    "\n\n";

    protected static final String FOOTER = "}\n";

    private String javaClassName;
    private String basePath;
    private String javaPackage;

    protected AbstractJavaFileGenerationProcessor(SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public String getHeader(String javaPackage, String comment, String author) {
        return String.format(HEADER, javaPackage, author, comment, new java.util.Date());
    }

    protected String createTableInstanceVariable(TableMetaData table) {
        return String.format("%2$s\n" +
                "final %1$s %1$s = new %1$s();\n", table.getName(), createComment(table));
    }

    public String createColumnsSource(TableMetaData table) {
        StringBuilder columnsSource = new StringBuilder();
        for (TableMetaDataColumn column : table.getColumns()) {
            if (!isColumnValid(column)) {
                continue;
            }
            columnsSource.append(createComment(column));
            columnsSource.append(createColumnSource(column));
        }
        return columnsSource.toString();
    }

    private static boolean isColumnValid(TableMetaDataColumn column) {
        return column.getName().matches("[A-Z_]+");
    }

    public String createComment(MetaDataElement element) {
        if (!element.hasRemark()) {
            return "";
        }
        String remark = element.getRemark();
        if (isBlank(remark)) {
            return "";
        }
        remark = remark.replaceFirst("deprecated", "@deprecated");
        return String.format("/** %s */\n", remark);
    }

    private String createComment(TableMetaDataColumn column) {
        String remark = column.hasRemark() ? column.getRemark() : "";
        if (column.isPrimaryKey()) {
            remark += addTableRemark("PK: ", column.getTable());
        }
        if (column.isForeignKey()) {
            remark += addTableRemark("FK: ", column.getReferencedTable());
        }
        if (isBlank(remark)) {
            return "";
        }
        remark = remark.replaceFirst("deprecated", "@deprecated");
        return String.format("/** %s */\n", remark);
    }

    protected String addTableRemark(String prefix, TableMetaData table) {
        String remark = prefix + table.getName();
        return table.hasRemark() ? remark + "; " + table.getRemark() : remark;
    }

    public String createColumnSource(TableMetaDataColumn column) {
        return String.format(
                "    public final Field %s = %s;\n",
                column.getName(), getColumnCreationMethod(column)
        );
    }

    public String getColumnCreationMethod(TableMetaDataColumn column) {

        String columnCreationMethod = getColumnCreationMethodByType(column);
        if (column.isForeignKey()) {
            TableMetaData referencedTable = column.getReferencedTable();
            if (referencedTable != null) {
                TableMetaDataColumn pkColumn;
                if (column.getReferencedColumn() != null) {
                    pkColumn = column.getReferencedColumn();
                } else {
                    pkColumn = referencedTable.getPrimaryKey();
                }
                columnCreationMethod = createForeignKeyColumn(referencedTable, pkColumn);
            }
        }
        if (column.isPrimaryKey()) {
            columnCreationMethod += ".primaryKey()";
        }
        return columnCreationMethod;
    }

    protected String createForeignKeyColumn(TableMetaData referencedTable, TableMetaDataColumn pkColumn) {
        return "foreignKey(" + javaClassName + "." + referencedTable.getName() + ".class,\"" + pkColumn.getName() + "\")";
    }

    protected String getColumnCreationMethodByType(TableMetaDataColumn column) {
        Class columnClass = column.getJavaClass() != null ? column.getJavaClass() : getJavaType(column.getJdbcType());
        if (columnClass.equals(String.class)) {
            return "string()";
        }
        if (columnClass.equals(Integer.class)) {
            return "integer()";
        }
        if (columnClass.equals(BigDecimal.class)) {
            return "numeric()";
        }
        if (columnClass.equals(Boolean.class)) {
            return "bool()";
        }
        if (columnClass.equals(Date.class)) {
            return "date()";
        }
        if (columnClass.equals(Timestamp.class)) {
            return "timestamp()";
        }
        return String.format("field(%s.class)", columnClass.getSimpleName());
    }

    public static Class getJavaType(int columnType) {
        switch (columnType) {
            case Types.VARCHAR:
            case Types.CHAR:
                return String.class;
            case Types.SMALLINT:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return BigDecimal.class;
            case Types.INTEGER:
                return Integer.class;
            case Types.DATE:
            case Types.TIME:
                return Date.class;
            case Types.TIMESTAMP:
                return Timestamp.class;
            case Types.BIT:
                return Boolean.class;
            default:
                return Object.class;
        }
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setJavaPackage(String javaPackage) {
        this.javaPackage = javaPackage;
    }

    public String getJavaPackage() {
        return javaPackage;
    }

    public void setJavaClassName(String javaClassName) {
        this.javaClassName = javaClassName;
    }

    public String getJavaClassName() {
        return javaClassName != null ? javaClassName : getSchemaMetaData().getSchema();
    }

    protected String getSchemaComment() {
        return "Generated from: "
                + (getSchemaMetaData().hasRemark() ? getSchemaMetaData().getRemark() : getSchemaMetaData().getSchema());
    }

    protected String getAuthor() {
        return getClass().getName();
    }
}
