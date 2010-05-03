/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.cayenne.modeler.Main;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Maven mojo to start up the Cayenne modeler from the command-line.
 * 
 * @since 3.0
 * 
 * @prefix cayenne-modeler
 * @goal run
 */
public class CayenneModelerMojo extends AbstractMojo {

	/**
	 * Name of the model file to open.
	 * 
	 * @parameter expression="${modeler.modelFile}
	 */
	private File modelFile;

	/**
	 * Project instance.
	 * 
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	private File lookupModelFile() {
		if (modelFile != null && modelFile.isFile()) {
			return modelFile;
		}

		return null;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		// setup the system property to tell the modeler where to generate the
		// java source files.
		System.setProperty("cayenne.cgen.destdir", project.getBuild()
				.getSourceDirectory());

		// start the modeler with the provided model file, if it exists.
		File f = lookupModelFile();
		if (f != null && f.exists() && !f.isDirectory()) {
			Main.main(new String[] { f.getAbsolutePath() });
		} else {
			Main.main(new String[] {});
		}

		// Block until the modeler finishes executing.
		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
