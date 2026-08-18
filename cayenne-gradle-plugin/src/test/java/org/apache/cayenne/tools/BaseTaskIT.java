/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class BaseTaskIT {

    /**
     * Tear-down Derby logic, appended to every build script under test.
     */
    private static final String DERBY_SHUTDOWN = """

            tasks.configureEach { t ->
                t.doLast {
                    try {
                        new org.apache.derby.jdbc.EmbeddedDriver()
                                .connect('jdbc:derby:;shutdown=true', new Properties())
                    } catch (java.sql.SQLException expected) {
                        // Derby reports a successful shutdown by throwing (XJ015 / 08006)
                    }
                }
            }
            """;

    protected File projectDir;

    @BeforeEach
    public void createProjectDir() throws IOException {
        String root = System.getProperty("cayenne.testProjectsDir", System.getProperty("java.io.tmpdir"));
        projectDir = Files.createTempDirectory(Files.createDirectories(Path.of(root)), "p").toFile();

        // Gradle still searches parent directories for a settings file. Without one of our own the
        // nested build would find and evaluate the settings script of the plugin module itself.
        Files.writeString(projectDir.toPath().resolve("settings.gradle"),
                "rootProject.name = '" + projectDir.getName() + "'\n");
    }

    @AfterEach
    public void deleteProjectDir() {
        try (Stream<Path> paths = Files.walk(projectDir.toPath())) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    protected GradleRunner createRunner(String projectName, String... args) throws Exception {
        prepareBuildScript(projectName);
        prepareDataMap(args);

        List<String> gradleArguments = new ArrayList<>(Arrays.asList(args));
        gradleArguments.add("--stacktrace");
        // move the Derby log out of the module root
        gradleArguments.add("-Dderby.stream.error.file=" + new File(projectDir, "derby.log").getAbsolutePath());

        return GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(gradleArguments);
    }

    private void prepareBuildScript(String name) throws Exception {
        Path src = Path.of(getClass().getResource(name + ".gradle").toURI());
        Path dst = projectDir.toPath().resolve("build.gradle");
        Files.writeString(dst, Files.readString(src) + DERBY_SHUTDOWN);
    }

    private void prepareDataMap(String... args) throws Exception {
        String pattern = "-PdataMap=";
        for(String arg : args) {
            if(arg.startsWith(pattern)) {
                String path = arg.substring(pattern.length());
                Path src = Path.of(getClass().getResource(path).toURI());
                Path dst = projectDir.toPath().resolve(path);
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
