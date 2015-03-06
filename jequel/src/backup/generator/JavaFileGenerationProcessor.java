package de.jexp.jequel.generator;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.TableMetaData;
import de.jexp.jequel.generator.data.TableMetaDataColumn;
import de.jexp.jequel.util.FileUtils;

import java.io.File;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 19.10.2007 00:50:24
 */
public class JavaFileGenerationProcessor extends AbstractJavaFileGenerationProcessor {

    public static final String TABLE_INITIALIZER =
            "    { initFields(); }\n";

    private boolean multiFile = false;

    public JavaFileGenerationProcessor(final SchemaMetaData schemaMetaData) {
        super(schemaMetaData);
    }


    public String createTableClassSource(final String tableName, final Boolean multiFile) {
        return createTableClassSource(schemaMetaData.getTable(tableName), multiFile);
    }

    public String createTableClassSource(final TableMetaData table, final boolean multiFile) {
        final String accessModifier = multiFile ? "final" : "final static";
        return String.format(
                "public %2$s class %1$s extends BaseTable<%1$s> {\n", table.getName(), accessModifier) +
                createColumnsSource(table) +
                TABLE_INITIALIZER +
                FOOTER;
    }

    public String createSchemaClassSource(final String javaClassName, final SchemaMetaData schemaMetaData, final boolean multiFile) {
        return String.format(
                "public abstract class %1$s {\n", javaClassName) +
                createTableInstancesSource(schemaMetaData, multiFile) +
                FOOTER;
    }

    public String createTableInstancesSource(final SchemaMetaData schemaMetaData, final boolean multiFile) {
        final StringBuilder sb = new StringBuilder();
        for (final TableMetaData tableMetaData : schemaMetaData.getTables()) {
            if (!multiFile) {
                sb.append(createTableClassSource(tableMetaData, multiFile));
            }
            sb.append(createTableInstanceVariable(tableMetaData));
        }
        return sb.toString();
    }

    public void createAllTablesSourceFiles(final String basePath, final String javaPackage, final boolean multiFile) {
        for (final TableMetaData tableMetaData : schemaMetaData.getTables()) {
            createTableSourceFile(basePath, javaPackage, tableMetaData, multiFile);
        }
    }

    public void createTableSourceFile(final String basePath, final String javaPackage, final TableMetaData tableMetaData, final boolean multiFile) {
        final File file = FileUtils.assertWriteJavaFile(basePath, javaPackage, tableMetaData.getName());
        FileUtils.writeFile(file, createTableFileSource(javaPackage, tableMetaData, multiFile));
    }

    protected String createTableFileSource(final String javaPackage, final TableMetaData tableMetaData, final boolean multiFile) {
        final StringBuilder tableSource = new StringBuilder();
        tableSource.append(getHeader(javaPackage, tableMetaData.getRemark(), getAuthor()));
        tableSource.append(createTableClassSource(tableMetaData, multiFile));
        return tableSource.toString();
    }

    public void createSchemaSourceFile(final String basePath, final String javaPackage, final String javaClassName, final boolean multiFile) {
        final File file = FileUtils.assertWriteJavaFile(basePath, javaPackage, javaClassName);
        FileUtils.writeFile(file, createSchemaFileSource(javaPackage, javaClassName, multiFile));
    }

    public String createSchemaFileSource(final String javaPackage, final String javaClassName, final boolean multiFile) {
        final StringBuilder tableSource = new StringBuilder();
        tableSource.append(getHeader(javaPackage, getSchemaComment(), getAuthor()));
        tableSource.append(createSchemaClassSource(javaClassName, schemaMetaData, multiFile));
        return tableSource.toString();
    }

    public void processMetaData() {
        if (isMultiFile())
            createAllTablesSourceFiles(getBasePath(), getJavaPackage(), isMultiFile());
        createSchemaSourceFile(getBasePath(), getJavaPackage(), getJavaClassName(), isMultiFile());
    }

    public boolean isMultiFile() {
        return multiFile;
    }

    public void setMultiFile(final boolean multiFile) {
        this.multiFile = multiFile;
    }

    protected String createForeignKeyColumn(final TableMetaData referencedTable, final TableMetaDataColumn pkColumn) {
        final String javaClassName = getJavaClassName();
        if (isMultiFile())
            return "foreignKey(" + javaClassName + ".class," + referencedTable.getName() + ".class,\"" + pkColumn.getName() + "\")";
        return "foreignKey(" + javaClassName + "." + referencedTable.getName() + ".class,\"" + pkColumn.getName() + "\")";
    }

}