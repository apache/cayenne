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
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * @since 4.0
 */
public class BaseTaskIT {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected File projectDir;

    @Before
    public void createProjectDir() throws IOException {
        projectDir = tempFolder.newFolder();
    }

    protected GradleRunner createRunner(String projectName, String... args) throws Exception {
        prepareBuildScript(projectName);
        prepareDataMap(args);

        List<String> gradleArguments = new ArrayList<>(Arrays.asList(args));
        gradleArguments.add("--stacktrace");

        return GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(gradleArguments);
    }

    private void prepareBuildScript(String name) throws Exception {
        Path src = new File(getClass().getResource(name + ".gradle").toURI()).toPath();
        Path dst = FileSystems.getDefault().getPath(projectDir.getAbsolutePath(), "build.gradle");
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    private void prepareDataMap(String... args) throws Exception {
        String pattern = "-PdataMap=";
        for(String arg : args) {
            if(arg.startsWith(pattern)) {
                String path = arg.substring(pattern.length());
                Path src = new File(getClass().getResource(path).toURI()).toPath();
                Path dst = FileSystems.getDefault().getPath(projectDir.getAbsolutePath(), path);
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
        }

    }
}