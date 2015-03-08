package de.jexp.jequel.transform;

import de.jexp.jequel.util.FileUtils;
import de.jexp.jequel.tables.TEST_TABLES;
import junit.framework.TestCase;

import java.io.File;

public class FileTransformerTest extends TestCase {


    public void t_stTransformFile() {
        new FileTransformer().transformClass(TransformTestFile.class, TEST_TABLES.class);
        final File javaFile = FileUtils.getJavaFile(TransformTestFile.class);
        final File jequelFile = new File(javaFile.getParentFile(), javaFile.getName().replace(".java",".jequel"));
        final String jequelFileSource=FileUtils.readFileToString(jequelFile);
        final String expectedJequelFile=FileUtils.readFileToString(FileUtils.getJavaFile(TransformTestFileJequel.class));
        assertEquals(expectedJequelFile, jequelFileSource);
    }
}