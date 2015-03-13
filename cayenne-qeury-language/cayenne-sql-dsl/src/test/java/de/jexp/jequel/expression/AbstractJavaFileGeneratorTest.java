package de.jexp.jequel.expression;

import de.jexp.jequel.generator.data.TableMetaData;
import junit.framework.TestCase;

/**
 * @author mh14 @ jexp.de
 * @since 31.10.2007 20:02:46 (c) 2007 jexp.de
 */
public abstract class AbstractJavaFileGeneratorTest extends TestCase {
    public static final String TABLE_LINE_ARTICLE =
            "/** ARTICLE_TEST */\n\n" +
                    "public final static ARTICLE ARTICLE = new ARTICLE();\n";
    public static final String TABLE_LINE_ARTICLE_COLOR =
            "\npublic final static ARTICLE_COLOR ARTICLE_COLOR = new ARTICLE_COLOR();\n";
    protected static final String TEST_CLASS_ARTICLE =
            "public final class ARTICLE extends BaseTable<ARTICLE> {\n" +
                    "/** PK: ARTICLE; ARTICLE_TEST */\n" +
                    "    public final Field OID = numeric().primaryKey();\n" +
                    "    public final Field NAME = string();\n" +
                    "/** @deprecated */\n" +
                    "    public final Field ACTIVE = date();\n" +
                    "    public final Field ARTICLE_NO = integer();\n" +
                    "    { initFields(); }\n" +
                    "}\n";

    protected static final String TEST_CLASS_ARTICLE_COLOR =
            "public final class ARTICLE_COLOR extends BaseTable<ARTICLE_COLOR> {\n" +
                    "/** PK: ARTICLE_COLOR */\n" +
                    "    public final Field OID = numeric().primaryKey();\n" +
                    "    public final Field ARTICLE_COLOR_NO = string();\n" +
                    "/** FK: ARTICLE; ARTICLE_TEST */\n" +
                    "    public final Field ARTICLE_OID = foreignKey(GEN_TEST_TABLES.ARTICLE.class,\"OID\");\n" +
                    "    { initFields(); }\n" +
                    "}\n";

    protected static final String TEST_CLASS_ARTICLE_COLOR_MULTI =
            TEST_CLASS_ARTICLE_COLOR.replace("GEN_TEST_TABLES.", "GEN_TEST_TABLES.class,");

    protected JavaFileGenerationProcessor fileGenerationProcessor;
    protected static final String TEST_PACKAGE_MULTI = "de.jexp.jequel.generator.tables.multi";
    protected static final String TEST_PACKAGE_MULTI_DB = "de.jexp.jequel.generator.tables_db.multi";
    protected static final String TEST_PACKAGE_SINGLE = "de.jexp.jequel.generator.tables.single";
    protected static final String TEST_PACKAGE_SINGLE_DB = "de.jexp.jequel.generator.tables_db.single";

    protected void setUpFileGeneratorWithArticleMetaData(final boolean multiFile) {
        fileGenerationProcessor = new JavaFileGenerationProcessor(GeneratorTestUtils.createTestSchemaMetaData());
        fileGenerationProcessor.setJavaClassName(GeneratorTestUtils.TEST_CLASS);
        fileGenerationProcessor.setMultiFile(multiFile);
    }

    protected void assertTestFileContent(final String testFileSource, final String testClassName, final String testPackage) {
        assertTrue(testFileSource.contains(TABLE_LINE_ARTICLE));
        assertTrue(testFileSource.contains(TABLE_LINE_ARTICLE_COLOR));
        assertTrue(testFileSource.contains("package " + testPackage + ";"));
        assertTrue(testFileSource.contains("public abstract class " + testClassName));
    }

    protected String createColumnSource(final TableMetaData metaData, final String columnName) {
        return fileGenerationProcessor.createColumnSource(metaData.getColumn(columnName));
    }
}
