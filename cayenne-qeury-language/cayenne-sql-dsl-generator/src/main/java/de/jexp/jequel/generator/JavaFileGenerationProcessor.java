package de.jexp.jequel.generator;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.TableMetaData;
import de.jexp.jequel.generator.data.TableMetaDataColumn;
import de.jexp.jequel.util.FileUtils;

import java.io.File;

public class JavaFileGenerationProcessor extends AbstractJavaFileGenerationProcessor {

    public static final String TABLE_INITIALIZER =
            "    { initFields(); }\n";

    private boolean multiFile = false;

    public JavaFileGenerationProcessor(SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }


    public String createTableClassSource(String tableName, Boolean multiFile) {
        return createTableClassSource(schemaMetaData.getTable(tableName), multiFile);
    }

    public String createTableClassSource(TableMetaData table, boolean multiFile) {
        String accessModifier = multiFile ? "final" : "final static";
        return String.format(
                "public %2$s class %1$s extends BaseTable<%1$s> {\n", table.getName(), accessModifier) +
                createColumnsSource(table) +
                TABLE_INITIALIZER +
                FOOTER;
    }

    public String createSchemaClassSource(String javaClassName, SchemaMetaData schemaMetaData, boolean multiFile) {
        return String.format(
                "public abstract class %1$s {\n", javaClassName) +
                createTableInstancesSource(schemaMetaData, multiFile) +
                FOOTER;
    }

    public String createTableInstancesSource(SchemaMetaData schemaMetaData, boolean multiFile) {
        StringBuilder sb = new StringBuilder();
        for (TableMetaData tableMetaData : schemaMetaData.getTables()) {
            if (!multiFile) {
                sb.append(createTableClassSource(tableMetaData, false));
            }
            sb.append(createTableInstanceVariable(tableMetaData));
        }
        return sb.toString();
    }

    public void createAllTablesSourceFiles(String basePath, String javaPackage, boolean multiFile) {
        for (TableMetaData tableMetaData : schemaMetaData.getTables()) {
            createTableSourceFile(basePath, javaPackage, tableMetaData, multiFile);
        }
    }

    public void createTableSourceFile(String basePath, String javaPackage, TableMetaData tableMetaData, boolean multiFile) {
        File file = FileUtils.assertWriteJavaFile(basePath, javaPackage, tableMetaData.getName());
        FileUtils.writeFile(file, createTableFileSource(javaPackage, tableMetaData, multiFile));
    }

    protected String createTableFileSource(String javaPackage, TableMetaData tableMetaData, boolean multiFile) {
        return getHeader(javaPackage, tableMetaData.getRemark(), getAuthor())
                + createTableClassSource(tableMetaData, multiFile);
    }

    public void createSchemaSourceFile(String basePath, String javaPackage, String javaClassName, boolean multiFile) {
        File file = FileUtils.assertWriteJavaFile(basePath, javaPackage, javaClassName);
        FileUtils.writeFile(file, createSchemaFileSource(javaPackage, javaClassName, multiFile));
    }

    public String createSchemaFileSource(String javaPackage, String javaClassName, boolean multiFile) {
        return getHeader(javaPackage, getSchemaComment(), getAuthor())
                + createSchemaClassSource(javaClassName, schemaMetaData, multiFile);
    }

    public void processMetaData() {
        if (isMultiFile()) {
            createAllTablesSourceFiles(getBasePath(), getJavaPackage(), isMultiFile());
        }
        createSchemaSourceFile(getBasePath(), getJavaPackage(), getJavaClassName(), isMultiFile());
    }

    public boolean isMultiFile() {
        return multiFile;
    }

    public void setMultiFile(boolean multiFile) {
        this.multiFile = multiFile;
    }

    protected String createForeignKeyColumn(TableMetaData referencedTable, TableMetaDataColumn pkColumn) {
        String javaClassName = getJavaClassName();
        if (isMultiFile()) {
            return "foreignKey(" + javaClassName + ".class," + referencedTable.getName() + ".class,\"" + pkColumn.getName() + "\")";
        }
        return "foreignKey(" + javaClassName + "." + referencedTable.getName() + ".class,\"" + pkColumn.getName() + "\")";
    }

}