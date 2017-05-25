package org.apache.cayenne.tools;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class GradlePluginIT extends BaseTaskIT {

    private void testDbImportWithGradleVersion(String version) throws Exception {
        String dbUrl = "jdbc:derby:" + projectDir.getAbsolutePath() + "/build/" + version.replace('.', '_');
        dbUrl += ";create=true";
        GradleRunner runner = createRunner("dbimport_simple_db", "cdbimport", "--info", "-PdbUrl=" + dbUrl);
        runner.withGradleVersion(version);
        runner.build();
    }

    private void testCgenWithGradleVersion(String version) throws Exception {
        GradleRunner runner = createRunner(
                "cgen_default_config",
                "cgen",
                "-PdataMap=" + getClass().getResource("test_datamap.map.xml").getFile()
        );
        runner.withGradleVersion(version);
        runner.build();
    }

    @Test
    public void testGradleVersionsCompatibility() throws Exception {
        String[] versions = {"3.5", "3.3", "3.0", "2.12", "2.8"};
        List<String> failedVersions = new ArrayList<>();
        for(String version : versions) {
            try {
                testDbImportWithGradleVersion(version);
                testCgenWithGradleVersion(version);
            } catch(Throwable th) {
                failedVersions.add(version);
            }
        }

        StringBuilder versionString = new StringBuilder("Failed versions:");
        for(String version : failedVersions) {
            versionString.append(" ").append(version);
        }
        assertTrue(versionString.toString(), failedVersions.isEmpty());
    }
}
