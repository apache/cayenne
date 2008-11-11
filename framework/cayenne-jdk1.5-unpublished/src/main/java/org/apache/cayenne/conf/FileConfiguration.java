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

package org.apache.cayenne.conf;

import java.io.File;

import org.apache.cayenne.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * FileConfiguration loads a Cayenne configuraton file from a given location in the file
 * system.
 * 
 */
public class FileConfiguration extends DefaultConfiguration {

    private static final Log logger = LogFactory.getLog(FileConfiguration.class);

    /**
     * The domain file used for this configuration
     */
    protected File projectFile;

    /**
     * Default constructor. Simply calls
     * {@link FileConfiguration#FileConfiguration(String)} with
     * {@link Configuration#DEFAULT_DOMAIN_FILE} as argument.
     * 
     * @see DefaultConfiguration#DefaultConfiguration()
     */
    public FileConfiguration() {
        this(Configuration.DEFAULT_DOMAIN_FILE);
    }

    /**
     * Creates a configuration that uses the provided file name as the main project file,
     * ignoring any other lookup strategies. The file name is <b>not</b> checked for
     * existence and must not contain relative or absolute paths, i.e. only the file name.
     * 
     * @throws ConfigurationException when projectFile is <code>null</code>.
     * @see DefaultConfiguration#DefaultConfiguration(String)
     */
    public FileConfiguration(String domainConfigurationName) {
        super(domainConfigurationName);

        // set the project file
        this.projectFile = new File(domainConfigurationName);

        // configure the ResourceLocator for plain files
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
     * Creates a configuration that uses the provided file as the main project file,
     * ignoring any other lookup strategies.
     * 
     * @throws ConfigurationException when projectFile is <code>null</code>, a
     *             directory or not readable.
     */
    public FileConfiguration(File domainConfigurationFile) {
        super();

        logger.debug("using domain file: " + domainConfigurationFile);

        // set the project file
        setProjectFile(domainConfigurationFile);

        // configure the ResourceLocator for plain files
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
     * Adds the given String as a custom path for filesystem lookups. The path can be
     * relative or absolute and is <i>not</i> checked for existence. This allows for easy
     * customization of resource search paths after Constructor invocation:
     * 
     * <pre>
     * conf = new FileConfiguration(&quot;myconfig-cayenne.xml&quot;);
     * conf.addFilesystemPath(new File(&quot;a/relative/path&quot;));
     * conf.addFilesystemPath(new File(&quot;/an/absolute/search/path&quot;));
     * Configuration.initializeSharedConfiguration(conf);
     * </pre>
     * 
     * Alternatively use {@link FileConfiguration#addFilesystemPath(File)} for adding a
     * path that is checked for existence.
     * 
     * @throws IllegalArgumentException if <code>path</code> is <code>null</code>.
     */
    public void addFilesystemPath(String path) {
        locator.addFilesystemPath(path);
    }

    /**
     * Adds the given directory as a path for filesystem lookups. The directory is checked
     * for existence.
     * 
     * @throws IllegalArgumentException if <code>path</code> is <code>null</code>,
     *             not a directory or not readable.
     */
    public void addFilesystemPath(File path) {
        locator.addFilesystemPath(path);
    }

    /**
     * Only returns <code>true</code> when {@link #getProjectFile} does not return
     * <code>null</code>.
     * 
     * @deprecated since 3.0 - superclass method is deprecated.
     */
    @Override
    public boolean canInitialize() {
        // TODO: move this to "initialize" once the deprecated method is removed
        return this.getProjectFile() != null;
    }

    /**
     * Returns the main domain file used for this configuration.
     */
    public File getProjectFile() {
        return projectFile;
    }

    /**
     * Sets the main domain file used for this configuration.
     * 
     * @throws ConfigurationException if <code>projectFile</code> is null, a directory
     *             or not readable.
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
     * Returns the directory of the current project file as returned by
     * {@link #getProjectFile}.
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
