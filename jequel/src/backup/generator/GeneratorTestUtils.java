package de.jexp.jequel.generator;

import de.jexp.jequel.generator.data.SchemaMetaData;
import de.jexp.jequel.generator.data.TableMetaData;
import de.jexp.jequel.generator.data.TableMetaDataColumn;
import de.jexp.jequel.util.FileUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Types;
import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 19.10.2007 03:12:28
 */
public class GeneratorTestUtils {
    static DataSource dataSource;
    public static JdbcTemplate jdbcTemplate;
    static final char SEP = File.separatorChar;
    public static final String ARTICLE = "ARTICLE";
    static final String OID = "OID";
    static final String NAME = "NAME";
    static final String ACTIVE = "ACTIVE";
    public static final String ARTICLE_NO = "ARTICLE_NO";
    static final String DEPRECATED = "deprecated";
    static final String ARTICLE_OID = "ARTICLE_OID";
    static final String ARTICLE_COLOR = "ARTICLE_COLOR";
    static final String TABLE_TEST_REMARK = "ARTICLE_TEST";

    public static final String CREATE_TABLE_ARTICLE =
            "create memory table article (" +
                    "oid numeric(10), " +
                    "name varchar(50), " +
                    "active date, " +
                    "article_no integer," +
                    "constraint pk_article primary key (oid)" +
                    ")";
    private static final String INSERT_TEST_ARTICLE_SQL = "insert into article values(10,'Foobar',now(),12345)";
    static final String TEST_PATH = "unit/test";
    public static final String TEST_CLASS = "GEN_TEST_TABLES";
    public static final boolean SINGLE_FILE = false;
    public static final boolean MULTI_FILE = true;
    static final String ARTICLE_COLOR_NO = "ARTICLE_COLOR_NO";
    private static final String CREATE_TABLE_ARTICLE_COLOR = "create table ARTICLE_COLOR(OID numeric(10),  ARTICLE_COLOR_NO varchar(20), ARTICLE_OID numeric(10), constraint pk_article_color primary key (OID), constraint fk_article_color_article foreign key (ARTICLE_OID) references ARTICLE(OID))";

    public static DataSource createAndSetupHsqlDatasource() {
        if (dataSource != null) return dataSource;
        dataSource = new DriverManagerDataSource("org.hsqldb.jdbcDriver", "jdbc:hsqldb:data/test", "sa", "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(CREATE_TABLE_ARTICLE);
        jdbcTemplate.execute(CREATE_TABLE_ARTICLE_COLOR);
//        jdbcTemplate.execute("COMMENT ON COLUMN article.active IS '@deprecated'");
        jdbcTemplate.execute(INSERT_TEST_ARTICLE_SQL);
        return dataSource;
    }

    public static void closeDatabase() {
        if (dataSource != null) {
            jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute("drop table article_color");
            jdbcTemplate.execute("drop table article");
            jdbcTemplate.execute("shutdown");
            dataSource = null;
        }
    }

    static String readTestFile(final String testPath, final String testPackage, final String testClass) throws IOException {
        final String path = testPath + SEP + testPackage.replace('.', SEP) + SEP + testClass + ".java";
        return FileUtils.readFileToString(path);
    }

    public static File getSerializedFile(final String path, final String packageName, final String file) {
        final File fileForPackage = FileUtils.getFileForPackage(path, packageName);
        return new File(fileForPackage, file);
    }

    public static Map<String, TableMetaData> readSerializedTableMetaData(final File file) throws IOException, ClassNotFoundException {
        return readObject(file);
    }

    public static SchemaMetaData readSerializedSchemaMetaData(final File file) throws IOException, ClassNotFoundException {
        return readObject(file);
    }

    public static <T> T readObject(final File file) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(file));
            return (T) ois.readObject();
        } finally {
            if (ois != null) {
                ois.close();
            }
        }
    }

    public static void writeSerializedTableMetaData(final File file, final Object data) throws IOException {
        final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(data);
        oos.close();
    }

    public static SchemaMetaData createTestSchemaMetaData() {
        final SchemaMetaData schemaMetaData = new SchemaMetaData("TEST");
        final TableMetaData articleData = new TableMetaData(ARTICLE, schemaMetaData);
        final TableMetaDataColumn articleOidColumn = articleData.addColumn(OID, Types.NUMERIC);
        articleOidColumn.setPrimaryKey();
        articleData.addColumn(NAME, java.sql.Types.VARCHAR);
        articleData.addColumn(ACTIVE, java.sql.Types.DATE);
        articleData.addColumn(ARTICLE_NO, java.sql.Types.INTEGER);
        schemaMetaData.addTable(articleData);

        addTestRemarks(schemaMetaData);
        final TableMetaData articleColorData = new TableMetaData(ARTICLE_COLOR, schemaMetaData);
        articleColorData.addColumn(OID, java.sql.Types.NUMERIC).setPrimaryKey();
        articleColorData.addColumn(ARTICLE_COLOR_NO, java.sql.Types.VARCHAR);
        final TableMetaDataColumn articleOidRefColumn = articleColorData.addColumn(ARTICLE_OID, Types.NUMERIC);
        articleOidRefColumn.setReferencedColumn(articleOidColumn);
        schemaMetaData.addTable(articleColorData);
        return schemaMetaData;
    }

    static SchemaMetaData setUpLoaderWithDatabase() {
        final SchemaCrawlerLoadSchemaMetaDataProcessor loadSchemaMetaDataProcessor = new SchemaCrawlerLoadSchemaMetaDataProcessor();
        loadSchemaMetaDataProcessor.setHandleForeignKeys(true);
        loadSchemaMetaDataProcessor.setDataSource(createAndSetupHsqlDatasource());
        loadSchemaMetaDataProcessor.loadMetaData();
        // cheat, comment not possible with hsql
        final SchemaMetaData schemaMetaData = loadSchemaMetaDataProcessor.getSchemaMetaData();
        addTestRemarks(schemaMetaData);
        return schemaMetaData;
    }

    protected static void addTestRemarks(final SchemaMetaData schemaMetaData) {
        final TableMetaData articleTable = schemaMetaData.getTable(ARTICLE);
        articleTable.setRemark(TABLE_TEST_REMARK);
        articleTable.getColumn(ACTIVE).setRemark(DEPRECATED);
    }

    static SchemaMetaData createSchemaDataWithRelationship() {
        final SchemaMetaData schemaMetaData = createTestSchemaMetaData();
        final TableMetaData articleColor = schemaMetaData.addTable(ARTICLE_COLOR);
        articleColor.addColumn(OID, Types.NUMERIC);
        articleColor.addColumn(ARTICLE_OID, Types.NUMERIC);
        return schemaMetaData;
    }

    public static void createTestMetaDataSourceFile(final String testPackage, final String className, final boolean multiFile) {
        createTestMetaDataSourceFile(testPackage, className, multiFile, setUpLoaderWithDatabase());
    }

    public static void createTestMetaDataSourceFile(final String testPackage, final String className, final boolean multiFile, final SchemaMetaData schemaMetaData) {
        final JavaFileGenerationProcessor fileGenerationProcessor = new JavaFileGenerationProcessor(schemaMetaData);
        fileGenerationProcessor.setBasePath(TEST_PATH);
        fileGenerationProcessor.setJavaPackage(testPackage);
        fileGenerationProcessor.setJavaClassName(className);
        fileGenerationProcessor.setMultiFile(multiFile);
        fileGenerationProcessor.processMetaData();
    }

}
