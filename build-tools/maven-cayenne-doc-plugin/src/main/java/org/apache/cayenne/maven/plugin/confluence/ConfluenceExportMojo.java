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
package org.apache.cayenne.maven.plugin.confluence;

import java.io.File;
import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * A goal to export Confluence documentation.
 * 
 * 
 * @goal confluence-export
 */
public class ConfluenceExportMojo extends AbstractMojo {
	/**
	 * The directory to put the exported documentation into
	 * 
	 * @parameter expression="${project.build.directory}/${confluence.spaceName}"
	 */
	private String outputDirectory;

	/**
	 * The velocity template to use - defaults to loading
	 * 'doctemplates/default.vm' from the classpath
	 * 
	 * @parameter
	 */
	private String velocityTemplate;

	/**
	 * The root url to the Confluence instance For example in Cayenne:
	 * http://cwiki.apache.org/confluence/ is the base URL.
	 * 
	 * @parameter expression="${confluence.baseUrl}"
	 * @required
	 */
	private URL baseUrl;

	/**
	 * The name of the confluence space to export
	 * 
	 * @parameter expression="${confluence.spaceName}"
	 * @required
	 */
	private String spaceName;

	/**
	 * The page in the space to start with
	 * 
	 * @parameter expression="${confluence.startPage}"
	 * @required
	 */
	private String startPage;

	/**
	 * The username to log in as - define it on the commandline via the
	 * -Dconfluence.userName=user_name option or set the userName and password
	 * in your ~/.m2/settings.xml file like this;
	 * 
	 * <pre>
	 *      	 &lt;profiles&gt;
	 *      	 &lt;profile&gt;
	 *      	 &lt;properties&gt;
	 *      	 &lt;property&gt;
	 *      	 &lt;name&gt;confluence.userName&lt;/name&gt;
	 *      	 &lt;value&gt;user name&lt;/value&gt;
	 *      	 &lt;/property&gt;
	 *      	 &lt;property&gt;
	 *      	 &lt;name&gt;confluence.password&lt;/name&gt;
	 *      	 &lt;value&gt;password&lt;/value&gt;
	 *      	 &lt;/property&gt;
	 *      	 &lt;/properties&gt;
	 *      	 &lt;id&gt;confluence&lt;/id&gt;
	 *      	 &lt;/profile&gt;
	 *      	 &lt;/profiles&gt;
	 * </pre>
	 * 
	 * @parameter expression="${confluence.userName}"
	 * @required
	 */
	private String userName;

	/**
	 * The username to log in as - define it on the commandline via the
	 * -Dconfluence.password=password option
	 * 
	 * @parameter expression="${confluence.password}"
	 * @required
	 */
	private String password;

	/**
	 * Worker method.
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		File thisDir = new File(System.getProperty("user.dir"));
		File output = new File(thisDir, outputDirectory);
		getLog().info(
				"Exporting space '" + spaceName + "' to "
						+ output.getAbsolutePath());

		try {
			DocGenerator generator = new DocGenerator(baseUrl.toString(),
					spaceName, outputDirectory, startPage, userName, password,
					velocityTemplate);

			getLog().info(
					"Confluence base URL '" + generator.getBaseUrl() + "'");
			generator.generateDocs();
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("Failed to export: " + spaceName
					+ " from: " + baseUrl, e);
		}
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getVelocityTemplate() {
		return velocityTemplate;
	}

	public void setVelocityTemplate(String velocityTemplate) {
		this.velocityTemplate = velocityTemplate;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public URL getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(URL rootURL) {
		this.baseUrl = rootURL;
	}

	public String getSpaceName() {
		return spaceName;
	}

	public void setSpaceName(String spaceName) {
		this.spaceName = spaceName;
	}

	public String getStartPage() {
		return startPage;
	}

	public void setStartPage(String startPage) {
		this.startPage = startPage;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
