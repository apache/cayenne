package de.jexp.jequel.expression;

import static de.jexp.jequel.generator.GeneratorTestUtils.*;

import java.io.IOException;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 19.10.2007 00:48:07
 */
public class TableJavaMultiFileGeneratorTest extends AbstractJavaFileGeneratorTest {
    public void testWriteFile() {
        setUpFileGeneratorWithArticleMetaData(GeneratorTestUtils.MULTI_FILE);
        final String articleClass = fileGenerationProcessor.createTableClassSource(GeneratorTestUtils.ARTICLE, GeneratorTestUtils.MULTI_FILE);
        assertEquals(TEST_CLASS_ARTICLE, articleClass);
        final String articleColorClass = fileGenerationProcessor.createTableClassSource(GeneratorTestUtils.ARTICLE_COLOR, GeneratorTestUtils.MULTI_FILE);
        assertEquals(TEST_CLASS_ARTICLE_COLOR_MULTI, articleColorClass);
    }

    public void testCreateAllTablesSource() {
        setUpFileGeneratorWithArticleMetaData(GeneratorTestUtils.MULTI_FILE);
        final String allTablesSource = fileGenerationProcessor.createSchemaFileSource(TEST_PACKAGE_MULTI, GeneratorTestUtils.TEST_CLASS, GeneratorTestUtils.MULTI_FILE);
        assertTestFileContent(allTablesSource, GeneratorTestUtils.TEST_CLASS, TEST_PACKAGE_MULTI);
        final String articleTableSource = fileGenerationProcessor.createTableClassSource(GeneratorTestUtils.ARTICLE, GeneratorTestUtils.MULTI_FILE);
        assertTrue(articleTableSource.contains(TEST_CLASS_ARTICLE));
        final String articleColorTableSource = fileGenerationProcessor.createTableClassSource(GeneratorTestUtils.ARTICLE_COLOR, GeneratorTestUtils.MULTI_FILE);
        assertTrue(articleColorTableSource.contains(TEST_CLASS_ARTICLE_COLOR_MULTI));
    }

    public void testCreateAndWriteFile() throws IOException {
        setUpFileGeneratorWithArticleMetaData(GeneratorTestUtils.MULTI_FILE);
        fileGenerationProcessor.createSchemaSourceFile(GeneratorTestUtils.TEST_PATH, TEST_PACKAGE_MULTI, GeneratorTestUtils.TEST_CLASS, GeneratorTestUtils.MULTI_FILE);
        assertTestFileContent(GeneratorTestUtils.readTestFile(GeneratorTestUtils.TEST_PATH, TEST_PACKAGE_MULTI, GeneratorTestUtils.TEST_CLASS), GeneratorTestUtils.TEST_CLASS, TEST_PACKAGE_MULTI);
        fileGenerationProcessor.createAllTablesSourceFiles(GeneratorTestUtils.TEST_PATH, TEST_PACKAGE_MULTI, fileGenerationProcessor.isMultiFile());
        assertTrue(GeneratorTestUtils.readTestFile(GeneratorTestUtils.TEST_PATH, TEST_PACKAGE_MULTI, GeneratorTestUtils.ARTICLE).contains(TEST_CLASS_ARTICLE));
        assertTrue(GeneratorTestUtils.readTestFile(GeneratorTestUtils.TEST_PATH, TEST_PACKAGE_MULTI, GeneratorTestUtils.ARTICLE_COLOR).contains(TEST_CLASS_ARTICLE_COLOR_MULTI));
    }

    public void testReadFromDbCreateAndWriteFile() throws IOException {
        GeneratorTestUtils.createTestMetaDataSourceFile(TEST_PACKAGE_MULTI_DB, GeneratorTestUtils.TEST_CLASS, GeneratorTestUtils.MULTI_FILE);
        assertTestFileContent(GeneratorTestUtils.readTestFile(GeneratorTestUtils.TEST_PATH, TEST_PACKAGE_MULTI_DB, GeneratorTestUtils.TEST_CLASS), GeneratorTestUtils.TEST_CLASS, TEST_PACKAGE_MULTI_DB);
    }

}