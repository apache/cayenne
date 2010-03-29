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

package org.apache.cayenne.project;

import java.io.File;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DataSourceFactory;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.conf.FileConfiguration;

/**
 * Subclass of FileConfiguration used in the project model.
 * 
 */
public class ProjectConfiguration extends FileConfiguration {

    /**
     * Override parent implementation to ignore loading failures.
     * 
     * @see FileConfiguration#FileConfiguration(File)
     */
    public ProjectConfiguration(File projectFile) {
        super(projectFile);

        // ignore loading failures
        this.setIgnoringLoadFailures(true);

        // configure deterministic file opening rules
        locator.setSkipAbsolutePath(false);
        locator.setSkipClasspath(true);
        locator.setSkipCurrentDirectory(true);
        locator.setSkipHomeDirectory(true);
    }

    /**
     * Override parent implementation to prevent loading of nonexisting files.
     * 
     * @see FileConfiguration#canInitialize()
     */
    @Override
    public boolean canInitialize() {
        return (super.canInitialize() && this.getProjectFile().isFile());
    }

    /**
     * Override parent implementation to allow for null files.
     * 
     * @see FileConfiguration#setProjectFile(File)
     */
    @Override
    protected void setProjectFile(File projectFile) {
        if ((projectFile != null) && (projectFile.exists())) {
            super.setProjectFile(projectFile);
        }
        else {
            super.projectFile = projectFile;
            this.setDomainConfigurationName(Configuration.DEFAULT_DOMAIN_FILE);
        }
    }

    @Override
    public DataSourceFactory getDataSourceFactory(String userFactoryName) {
        File projectDir = getProjectDirectory();
        boolean canLoad = DriverDataSourceFactory.class.getName().equals(userFactoryName);
        ProjectDataSourceFactory factory = new ProjectDataSourceFactory(projectDir, canLoad);
        return factory;
    }

    /**
     * Returns an instance of {@link ProjectDataSourceFactory}.
     * 
     * @deprecated since 3.0 as the super method is deprecated as well.
     */
    @Override
    public DataSourceFactory getDataSourceFactory() {
        try {
            return new ProjectDataSourceFactory(getProjectDirectory());
        }
        catch (Exception e) {
            throw new ProjectException("Error creating DataSourceFactory.", e);
        }
    }
}
