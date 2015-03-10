package de.jexp.jequel.generator;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.TableMetaData;
import de.jexp.jequel.generator.data.TableMetaDataColumn;
import de.jexp.jequel.util.FileUtils;

import java.io.File;

public class JavaFileGenerationProcessor extends AbstractJavaFileGenerationProcessor {

    public static final String TABLE_INITIALIZER =
            "    { initFields(); }\n";

    public JavaFileGenerationProcessor(SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }

    public String createTableClassSource(String tableName) {
        return createTableClassSource(getSchemaMetaData().getTable(tableName));
    }

    public String createTableClassSource(TableMetaData table) {
        return String.format(
                "final class %1$s extends BaseTable<%1$s> {\n", table.getName()) +
                createColumnsSource(table) +
                TABLE_INITIALIZER +
                FOOTER;
    }

    public String createSchemaClassSource(String javaClassName, SchemaMetaData schemaMetaData) {
        return String.format(
                "public interface %1$s {\n", javaClassName) +
                createTableInstancesSource(schemaMetaData) +
                FOOTER;
    }

    public String createTableInstancesSource(SchemaMetaData schemaMetaData) {
        StringBuilder sb = new StringBuilder();
        for (TableMetaData tableMetaData : schemaMetaData.getTables()) {
            sb.append(createTableInstanceVariable(tableMetaData));
        }
        return sb.toString();
    }

    public void createAllTablesSourceFiles(String basePath, String javaPackage) {
        for (TableMetaData tableMetaData : getSchemaMetaData().getTables()) {
            createTableSourceFile(basePath, javaPackage, tableMetaData);
        }
    }

    public void createTableSourceFile(String basePath, String javaPackage, TableMetaData tableMetaData) {
        File file = FileUtils.assertWriteJavaFile(basePath, javaPackage, tableMetaData.getName());
        FileUtils.writeFile(file, createTableFileSource(javaPackage, tableMetaData));
    }

    protected String createTableFileSource(String javaPackage, TableMetaData tableMetaData) {
        return getHeader(javaPackage, tableMetaData.getRemark(), getAuthor())
                + createTableClassSource(tableMetaData);
    }

    public void createSchemaSourceFile(String basePath, String javaPackage, String javaClassName) {
        File file = FileUtils.assertWriteJavaFile(basePath, javaPackage, javaClassName);
        FileUtils.writeFile(file, createSchemaFileSource(javaPackage, javaClassName));
    }

    public String createSchemaFileSource(String javaPackage, String javaClassName) {
        return getHeader(javaPackage, getSchemaComment(), getAuthor())
                + createSchemaClassSource(javaClassName, getSchemaMetaData());
    }

    public void processMetaData() {
        createSchemaSourceFile(getBasePath(), getJavaPackage(), getJavaClassName());
    }

    protected String createForeignKeyColumn(TableMetaData referencedTable, TableMetaDataColumn pkColumn) {
        return "foreignKey(" + getJavaClassName() + "." + referencedTable.getName() + ".class,\"" + pkColumn.getName() + "\")";
    }

}