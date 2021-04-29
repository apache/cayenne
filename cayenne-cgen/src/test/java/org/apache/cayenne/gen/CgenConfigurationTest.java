package org.apache.cayenne.gen;

import org.apache.cayenne.validation.ValidationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CgenConfigurationTest {

    private String rootC;
    private String rootE;

    private String oldPath;
    private String newPath;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        rootC = "C:\\";
        rootE = "E:\\";

        oldPath = "test\\test_folder\\";
        newPath = "test\\test_folder\\test";
    }

    /**
     * build full path
     * @param paths parts of the path
     * @return wrapper full path
     */
    private Path buildPath(String ... paths) {
        String fullPath = String.join("\\\\",paths);
        return Paths.get(fullPath);
    }

    /**
     * execute test with equal path roots
     * @param rootPathValues parts of the root path
     * @param relPathValues parts of the rel path
     */
    private void equalPathRootTest(String[] rootPathValues, String[] relPathValues) {

        CgenConfiguration configuration = new CgenConfiguration(false);

        Path rootPath = buildPath(rootPathValues);
        Path relPath = buildPath(relPathValues);

        configuration.setRootPath(rootPath);
        configuration.setRelPath(relPath.toAbsolutePath().toString());

        Assert.assertEquals(configuration.getRelPath(), rootPath.relativize(relPath));
    }

    @Test
    public  void EqualRootEqualDirectoryTest () {
        equalPathRootTest(new String[]{rootC,newPath}, new String[] {rootC + newPath});
    }

    @Test
    public  void EqualRootNotEqualDirectoryTest () {
        equalPathRootTest(new String[]{rootC,oldPath}, new String[] {rootC + newPath});
    }

    @Test
    public  void EqualRootEmptyDirectoryTest () {
        equalPathRootTest(new String[]{rootC}, new String[] {rootC});
    }

    /**
     * execute test with not equal or empty path roots
     * @param rootPathValues parts of the root path
     * @param relPathValues parts of the rel path
     */
    private void notEqualPathRootTest(String[] rootPathValues, String[] relPathValues) {

        CgenConfiguration configuration = new CgenConfiguration(false);

        Path rootPath = buildPath(rootPathValues);
        Path relPath = buildPath(relPathValues);

        configuration.setRootPath(rootPath);
        configuration.setRelPath(relPath.toAbsolutePath().toString());

        Assert.assertEquals(configuration.getRelPath(), relPath.toAbsolutePath());
    }


    @Test
    public  void NotEqualRootEqualDirectoryTest () {
        notEqualPathRootTest(new String[]{rootC,newPath}, new String[]{rootE,newPath});
    }

    @Test
    public  void NotEqualRootNotEqualDirectoryTest () {
        notEqualPathRootTest(new String[]{rootC,oldPath}, new String[]{rootE,newPath});
    }

    @Test
    public  void NotEqualRootEmptyDirectoryTest () {
        notEqualPathRootTest(new String[]{rootC}, new String[]{rootE});
    }

    @Test
    public  void RootEmptyRootEmptyDirectoryTest () {
        notEqualPathRootTest(new String[]{}, new String[]{rootC});
    }

    /**
     * execute test with invalid path
     * @param rootPathValues parts of the root path
     * @param relPathValues parts of the rel path
     */
    private void emptyPathRootTest (String[] rootPathValues, String[] relPathValues) {


        expectedException.expect(ValidationException.class);

        CgenConfiguration configuration = new CgenConfiguration(false);

        Path rootPath = buildPath(rootPathValues);
        Path relPath = buildPath(relPathValues);

        configuration.setRootPath(rootPath);
        configuration.setRelPath(relPath.toString());
    }

    @Test
    public  void RelEmptyRootEmptyDirectoryTest () {
        emptyPathRootTest(new String[]{rootC}, new String[]{});
    }

    @Test
    public  void RootRefEmptyRootEmptyDirectoryTest () {
        emptyPathRootTest(new String[]{}, new String[]{});
    }

    @Test
    public  void invalidRootPathTest () {
        notEqualPathRootTest(new String[]{"testDirectory","_invalid_directory"}, new String[]{rootC,"testDirectory"});
    }

    @Test
    public  void invalidRectPathTest () {
        emptyPathRootTest(new String[]{rootC,"testDirectory"}, new String[]{"_invalid_directory","_invalid_directory"});
    }


    @Test
    public  void invalidRootAndRectPathTest () {
        emptyPathRootTest(new String[]{"_invalid_directory","_invalid_directory"}, new String[]{"_invalid_directory","_invalid_directory"});
    }



}