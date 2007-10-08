package org.objectstyle.cayenne.util;

import java.io.File;
import java.net.URL;

import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class ZipUtilTst extends BasicTestCase {

    public void testUnzip() throws Exception {

        URL jarResource = Thread.currentThread().getContextClassLoader().getResource(
                "jar-test.jar");
        File jarCopy = new File(getTestDir(), "jar-test.jar");
        Util.copy(jarResource, jarCopy);

        File unjarDir = getTestDir();
        File unjarRootDir = new File(unjarDir, "jar-test");
        File manifest = new File(unjarRootDir.getParentFile(), "META-INF"
                + File.separator
                + "MANIFEST.MF");
        assertFalse(unjarRootDir.exists());
        assertFalse(manifest.exists());

        try {
            // try unzipping the JAR
            ZipUtil.unzip(jarCopy, unjarDir);

            assertTrue(unjarRootDir.isDirectory());
            assertTrue(new File(unjarRootDir, "jar-test1.txt").length() > 0);
            assertTrue(new File(unjarRootDir, "jar-test2.txt").length() > 0);
            assertTrue(manifest.isFile());
        }
        finally {
            Util.delete(unjarRootDir.getPath(), true);
            Util.delete(new File(unjarDir, "META-INF").getPath(), true);
        }
    }

    public void testZip() throws Exception {
        URL jarResource = Thread.currentThread().getContextClassLoader().getResource(
                "jar-test.jar");
        File jarCopy = new File(getTestDir(), "jar-test.jar");
        Util.copy(jarResource, jarCopy);

        File unjarDir = getTestDir();
        File unjarRootDir = new File(unjarDir, "jar-test");
        File newJarFile = new File(unjarDir, "new-jar.jar");

        try {
            // unzip existing jar and recreate
            assertFalse(unjarRootDir.exists());
            ZipUtil.unzip(jarCopy, unjarDir);

            ZipUtil.zip(newJarFile, unjarDir, new File[] {
                    unjarRootDir, new File(unjarDir, "META-INF")
            }, '/');

            assertTrue(newJarFile.isFile());

            // can't compare length, since different algorithms may have been used
            // assertEquals(jar.length(), newJarFile.length());

            // try unzipping it again
            Util.delete(unjarRootDir.getPath(), true);
            Util.delete(new File(unjarDir, "META-INF").getPath(), true);
            ZipUtil.unzip(newJarFile, unjarDir);

        }
        finally {
            Util.delete(unjarRootDir.getPath(), true);
            Util.delete(new File(unjarDir, "META-INF").getPath(), true);
            newJarFile.delete();
        }
    }
}
