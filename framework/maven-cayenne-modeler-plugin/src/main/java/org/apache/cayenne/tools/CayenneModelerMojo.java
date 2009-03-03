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
import java.util.Properties;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.modeler.Main;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

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
            if (modelFile.isDirectory()) {
                return new File(modelFile,Configuration.DEFAULT_DOMAIN_FILE);
            } else {
                return modelFile;
            }
        }
        
        //try to locate cayenne.xml at top level of a resource directory.
        for(Object o : project.getResources()) {
            Resource r = (Resource) o;
            File f = new File(r.getDirectory(),Configuration.DEFAULT_DOMAIN_FILE);
            if (f.exists()) {
                return f;
            }
        }
        
        //failing that, try for WEB-INF/DEFAULT_DOMAIN_FILE 
        //but only if we're using the war plugin
        for(Object o : project.getBuildPlugins()) {
            Plugin plugin = (Plugin) o;
            //means we're using the war plugin.
            if (plugin.getKey().equals("org.apache.maven.plugins:maven-war-plugin")) {
                //check to see if the default loc. is overridden.
                Xpp3Dom conf = (Xpp3Dom)plugin.getConfiguration();
                String path;

                if (conf != null && (conf = conf.getChild("warSourceDirectory")) != null) {
                    path = conf.getValue().trim();
                } else {
                   path = "src" + File.separator + "main" + File.separator + "webapp"; 
                }

                return new File(project.getBasedir().getAbsolutePath(),
                                    path + File.separator + 
                                    "WEB-INF" + File.separator + 
                                    Configuration.DEFAULT_DOMAIN_FILE);
            }
        }

        return null;
    }

	public void execute() throws MojoExecutionException, MojoFailureException {
        //setup the system property to tell the modeler where to generate the java source files.
        System.setProperty("cayenne.cgen.destdir",project.getBuild().getSourceDirectory());

        //start the modeler with the provided model file, if it exists.
        File f = lookupModelFile();
        if (f != null && f.exists() && !f.isDirectory()) {
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
