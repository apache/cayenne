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
import java.util.List;

import org.apache.cayenne.modeler.Main;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private File lookupModelFile() {
        if (modelFile != null) {
            return modelFile;
        }
        
        //try to locate cayenne.xml at top level of a resource directory.
        for(Object o : project.getResources()) {
            Resource r = (Resource) o;
            File f = new File(r.getDirectory(),"cayenne.xml");
            if (f.exists()) {
                return f;
            }
        }
        
        //failing that, try for WEB-INF/cayenne.xml in the maven-conventional webapp directory, src/main/webapp
        File f = new File(project.getBasedir().getAbsolutePath(),
                            "src" + File.separator + 
                            "main" + File.separator + 
                            "webapp" + File.separator + 
                            "WEB-INF" + File.separator +
                            "cayenne.xml");
        return f;
    }

	public void execute() throws MojoExecutionException, MojoFailureException {
        File f = lookupModelFile();
        //start the modeler with the provided model file, if it exists.
        if (f.exists() && !f.isDirectory()) {
            Main.main(new String[] {f.getAbsolutePath()});
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
