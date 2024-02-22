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

import org.apache.cayenne.test.file.FileUtil;
import org.apache.cayenne.test.resource.ResourceUtil;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.Path;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CayenneGeneratorTaskCrossMapRelationshipsTest {

	/**
	 * Tests pairs generation with a cross-DataMap relationship.
	 */
	@Test
	public void testCrossDataMapRelationships() throws Exception {

		CayenneGeneratorTask task = new CayenneGeneratorTask();
		task.setProject(new Project());
		task.setTaskName("Test");
		task.setLocation(Location.UNKNOWN_LOCATION);

		// prepare destination directory

		File destDir = new File(FileUtil.baseTestDirectory(), "cgen12");
		// prepare destination directory
		if (!destDir.exists()) {
			assertTrue(destDir.mkdirs());
		}

		File map = new File(destDir, "cgen-dependent.map.xml");
		ResourceUtil.copyResourceToFile("org/apache/cayenne/tools/cgen-dependent.map.xml", map);

		File additionalMaps[] = new File[1];
		additionalMaps[0] = new File(destDir, "cgen.map.xml");
		ResourceUtil.copyResourceToFile("org/apache/cayenne/tools/cgen.map.xml", additionalMaps[0]);

		FileList additionalMapsFilelist = new FileList();
		additionalMapsFilelist.setDir(additionalMaps[0].getParentFile());
		additionalMapsFilelist.setFiles(additionalMaps[0].getName());

		Path additionalMapsPath = new Path(task.getProject());
		additionalMapsPath.addFilelist(additionalMapsFilelist);

		// setup task
		task.setMap(map);
		task.setAdditionalMaps(additionalMapsPath);
		task.setMakepairs(true);
		task.setOverwrite(false);
		task.setMode("entity");
		task.setIncludeEntities("MyArtGroup");
		task.setDestDir(destDir);
		task.setSuperpkg("org.apache.cayenne.testdo.cgen2.auto");
		task.setUsepkgpath(true);

		// run task
		task.execute();

		// check results
		File a = new File(destDir, convertPath("org/apache/cayenne/testdo/cgen2/MyArtGroup.java"));
		assertTrue(a.isFile());
		assertContents(a, "MyArtGroup", "org.apache.cayenne.testdo.cgen2", "_MyArtGroup");

		File _a = new File(destDir, convertPath("org/apache/cayenne/testdo/cgen2/auto/_MyArtGroup.java"));
		assertTrue(_a.exists());
		assertContents(_a, "_MyArtGroup", "org.apache.cayenne.testdo.cgen2.auto", "PersistentObject");
		assertContents(_a, "import org.apache.cayenne.testdo.testmap.ArtGroup;");
		assertContents(_a, " ArtGroup getToParentGroup()");
		assertContents(_a, "setToParentGroup(ArtGroup toParentGroup)");
	}

	private String convertPath(String unixPath) {
		return unixPath.replace('/', File.separatorChar);
	}

	private void assertContents(File f, String content) throws Exception {

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));) {
			String s = null;
			while ((s = in.readLine()) != null) {
				if (s.contains(content))
					return;
			}

			fail("<" + content + "> not found in " + f.getAbsolutePath() + ".");
		}

	}

	private void assertContents(File f, String className, String packageName, String extendsName) throws Exception {

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));) {
			assertPackage(in, packageName);
			assertClass(in, className, extendsName);
		}
	}

	private void assertPackage(BufferedReader in, String packageName) throws Exception {

		String s = null;
		while ((s = in.readLine()) != null) {

			if (Pattern.matches("^package\\s+([^\\s;]+);", s)) {
				assertTrue(s.contains(packageName));
				return;
			}
		}

		fail("No package declaration found.");
	}

	private void assertClass(BufferedReader in, String className, String extendsName) throws Exception {

		Pattern classPattern = Pattern.compile("^public\\s+");

		String s = null;
		while ((s = in.readLine()) != null) {
			if (classPattern.matcher(s).find()) {
				assertTrue(s.contains(className));
				assertTrue(s.contains(extendsName));
				assertTrue(s.indexOf(className) < s.indexOf(extendsName));
				return;
			}
		}

		fail("No class declaration found.");
	}
}
