/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.conf;

import java.io.File;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.util.ResourceLocator;

/**
 * FileConfiguration loads a Cayenne configuraton file from a given
 * location in the file system.
 *
 * @author Holger Hoffstaette
 */
public class FileConfiguration extends DefaultConfiguration {
	private static Logger logObj = Logger.getLogger(FileConfiguration.class);

	/**
	 * The domain file used for this configuration
	 */
	protected File projectFile;

	/**
	 * Default constructor.
	 * Simply calls {@link FileConfiguration#FileConfiguration(String)}
	 * with {@link Configuration#DEFAULT_DOMAIN_FILE} as argument.
	 * @see DefaultConfiguration#DefaultConfiguration()
	 */
	public FileConfiguration() {
		this(Configuration.DEFAULT_DOMAIN_FILE);
	}

	/**
	 * Creates a configuration that uses the provided file name
	 * as the main project file, ignoring any other lookup strategies.
	 * The file name is <b>not</b> checked for existence and must not
	 * contain relative or absolute paths, i.e. only the file name.
	 *
	 * @throws ConfigurationException when projectFile is <code>null</code>.
	 * @see DefaultConfiguration#DefaultConfiguration(String)
	 */
	public FileConfiguration(String domainConfigurationName) {
		super(domainConfigurationName);

		// set the project file
		this.projectFile = new File(domainConfigurationName);

		// configure the ResourceLocator for plain files
		ResourceLocator locator = this.getResourceLocator();
		locator.setSkipAbsolutePath(false);
		locator.setSkipClasspath(true);
		locator.setSkipCurrentDirectory(false);
		locator.setSkipHomeDirectory(true);

		// add the file's location to the search path, if it exists
		File projectDirectory = this.getProjectDirectory();
		if (projectDirectory != null) {
			locator.addFilesystemPath(projectDirectory.getPath());
		}
	}

	/**
	 * Creates a configuration that uses the provided file 
	 * as the main project file, ignoring any other lookup strategies.
	 * 
	 * @throws ConfigurationException when projectFile is <code>null</code>,
	 * a directory or not readable.
	 */
	public FileConfiguration(File domainConfigurationFile) {
		super();

		logObj.debug("using domain file: " + domainConfigurationFile);

		// set the project file
		this.setProjectFile(domainConfigurationFile);

		// configure the ResourceLocator for plain files
		ResourceLocator locator = this.getResourceLocator();
		locator.setSkipAbsolutePath(false);
		locator.setSkipClasspath(true);
		locator.setSkipCurrentDirectory(false);
		locator.setSkipHomeDirectory(true);

		// add the file's location to the search path, if it exists
		File projectDirectory = this.getProjectDirectory();
		if (projectDirectory != null) {
			locator.addFilesystemPath(projectDirectory);
		}
	}

	/**
	 * Adds the given String as a custom path for filesystem lookups.
	 * The path can be relative or absolute and is <i>not</i> checked
	 * for existence.
	 *
	 * This allows for easy customization of resource search paths after
	 * Constructor invocation:
	 * <pre>
	 * conf = new FileConfiguration("myconfig-cayenne.xml");
	 * conf.addFilesystemPath(new File("a/relative/path"));
	 * conf.addFilesystemPath(new File("/an/absolute/search/path"));
	 * Configuration.initializeSharedConfiguration(conf);
	 * </pre>
	 * 
	 * Alternatively use {@link FileConfiguration#addFilesystemPath(File)}
	 * for adding a path that is checked for existence.
	 * 
	 * @throws IllegalArgumentException if <code>path</code> is <code>null</code>.
	 */
	public void addFilesystemPath(String path) {
		this.getResourceLocator().addFilesystemPath(path);
	}

	/**
	 * Adds the given directory as a path for filesystem lookups.
	 * The directory is checked for existence.
	 * 
	 * @throws IllegalArgumentException if <code>path</code> is <code>null</code>,
	 * not a directory or not readable.
	 */
	public void addFilesystemPath(File path) {
		this.getResourceLocator().addFilesystemPath(path);
	}

	/**
	 * Only returns <code>true</code> when {@link #getProjectFile} does not
	 * return <code>null</code>.
	 */
	public boolean canInitialize() {
		// I can only initialize myself when I have a valid file
		return (this.getProjectFile() != null);
	}

	/**
	 * Returns the main domain file used for this configuration. 
	 */
	public File getProjectFile() {
		return projectFile;
	}

	/**
	 * Sets the main domain file used for this configuration.
	 * @throws ConfigurationException if <code>projectFile</code> is null,
	 * a directory or not readable.
	 */
	protected void setProjectFile(File projectFile) {
		if (projectFile != null) {
			if (projectFile.isFile()) {
				this.projectFile = projectFile;
				this.setDomainConfigurationName(projectFile.getName());
			}
			else {
				throw new ConfigurationException("Project file: "
													+ projectFile
													+ " is a directory or not readable.");
			}
		}
		else {
			throw new ConfigurationException("Cannot use null as project file.");
		}
	}

	/**
	 * Returns the directory of the current project file as
	 * returned by {@link #getProjectFile}.
	 */
	public File getProjectDirectory() {
		File pfile = this.getProjectFile();
		if (pfile != null) {
			return pfile.getParentFile();
		}
		else {
			return null;
		}
	}

}
