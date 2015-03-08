package de.jexp.jequel.util;

import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

public class FileUtilsTest extends TestCase {

    @Test
    @Ignore
    public void t_stGetJavaFileFromClassPath() {
        final File javaFile = FileUtils.getJavaFile(FileUtilsTest.class);
        assertNotNull(javaFile);
        assertTrue(javaFile.exists());
        assertEquals("FileUtilsTest.java", javaFile.getName());
        assertTrue(javaFile.getParent().endsWith("de\\jexp\\jequel\\util"));
    }

    public void testProcessFile() {
        final File testFile = new File("testProcessFile.txt");
        final String testText = "This is\na Test\nfor File Utils.";
        FileUtils.writeFile(testFile, testText);
        final String processResult = FileUtils.processFile(testFile, new FileUtils.LineProcessor() {
            Iterator<String> testLines= Arrays.asList(testText.split("\n")).iterator();
            public String processLine(final String line) {
                assertEquals(testLines.next(), line);
                return line;
            }
        });
        assertEquals(testText, processResult);
    }
}
