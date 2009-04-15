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

import java.io.PrintWriter;

import org.apache.cayenne.conf.ConfigSaver;
import org.apache.cayenne.conf.ConfigSaverDelegate;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.RuntimeSaveDelegate;

/**
 * ApplicationProjectFile is a ProjectFile abstraction of the main project file in a
 * Cayenne project. Right now Cayenne projects can not be renamed, so all the name
 * tracking functionality is pretty much noop.
 */
public class ApplicationProjectFile extends ProjectFile {

    protected ConfigSaverDelegate saveDelegate;

    private String objectName = null;

    /**
     * Constructor for default ApplicationProjectFile.
     */
    public ApplicationProjectFile(Project project) {
        this(project, Configuration.DEFAULT_DOMAIN_FILE);
    }

    /**
     * Constructor for ApplicationProjectFile with an existing file.
     */
    public ApplicationProjectFile(Project project, String fileName) {
        super(project, fileName);
        this.objectName = fileName.substring(0, fileName.lastIndexOf(this
                .getLocationSuffix()));
    }

    /**
     * Returns suffix to append to object name when creating a file name. Default
     * implementation returns empty string.
     */
    @Override
    public String getLocationSuffix() {
        return ".xml";
    }

    /**
     * Returns a project.
     */
    @Override
    public Object getObject() {
        return getProject();
    }

    /**
     * @see org.apache.cayenne.project.ProjectFile#getObjectName()
     */
    @Override
    public String getObjectName() {
        return this.objectName;
    }

    @Override
    public void save(PrintWriter out) throws Exception {
        ConfigSaverDelegate localDelegate = (saveDelegate != null)
                ? saveDelegate
                : new RuntimeSaveDelegate(((ApplicationProject) projectObj)
                        .getConfiguration());
        new ConfigSaver(localDelegate).storeDomains(out);
    }

    @Override
    public boolean canHandle(Object obj) {
        return obj instanceof ApplicationProject;
    }

    /**
     * Returns the saveDelegate.
     * 
     * @return ConfigSaverDelegate
     */
    public ConfigSaverDelegate getSaveDelegate() {
        return saveDelegate;
    }

    /**
     * Sets the saveDelegate.
     * 
     * @param saveDelegate The saveDelegate to set
     */
    public void setSaveDelegate(ConfigSaverDelegate saveDelegate) {
        this.saveDelegate = saveDelegate;
    }
}
