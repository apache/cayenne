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

import org.apache.cayenne.project.DataNodeConfigInfo;
import org.apache.cayenne.project.ProjectConfigInfo;
import org.apache.cayenne.project.ProjectConfigurator;
import org.apache.cayenne.project.ProjectException;
import org.apache.cayenne.util.Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * A "cdeploy" Ant task providing an Ant frontend to
 * org.apache.cayenne.project.ProjectConfigurator.
 * 
 * @deprecated since 3.0. This task turned out to be not usable, and is in fact rarely
 *             used (if ever). It will be removed in subsequent releases.
 */
public class DeploymentConfigurator extends CayenneTask {

    protected ProjectConfigInfo info;

    /**
     * Constructor for DeploymentConfigurator.
     */
    public DeploymentConfigurator() {
        super();
        info = new ProjectConfigInfo();
    }

    public ProjectConfigInfo getInfo() {
        return info;
    }

    /**
     * Executes the task. It will be called by ant framework.
     */
    @Override
    public void execute() throws BuildException {

        log(
                "*** cdeploy is deprecated in Cayenne 3.0 and will be removed in subsequent releases",
                Project.MSG_WARN);

        validateAttributes();

        try {
            processProject();
        }
        catch (Exception ex) {
            Throwable th = Util.unwindException(ex);
            String message = th.getMessage();
            StringBuilder buf = new StringBuilder();

            if (message != null && message.trim().length() > 0) {
                buf.append("Error: [").append(message).append("].");
            }
            else {
                buf.append("Error reconfiguring jar file.");
            }

            buf
                    .append(" Source: ")
                    .append(info.getSourceJar())
                    .append("; target: ")
                    .append(info.getDestJar());

            String errorMessage = buf.toString();
            super.log(errorMessage);
            throw new BuildException(errorMessage, ex);
        }
    }

    /**
     * Performs validation of task attributes. Throws BuildException if validation fails.
     */
    protected void validateAttributes() throws BuildException {
        if (info.getSourceJar() == null) {
            throw new BuildException("'src' attribute is required.");
        }

        if (!info.getSourceJar().isFile()) {
            throw new BuildException("'src' must be a valid file: " + info.getSourceJar());
        }

        if (info.getAltProjectFile() != null && !info.getAltProjectFile().isFile()) {
            throw new BuildException("'altProjectFile' must be a valid file: "
                    + info.getAltProjectFile());
        }

        for (DataNodeConfigInfo node : info.getNodes()) {
            if (node.getName() == null) {
                throw new BuildException("'node.name' attribute is required.");
            }

            if (node.getDataSource() != null && node.getDriverFile() != null) {
                throw new BuildException(
                        "'node.dataSource' and 'node.driverFile' are mutually exclusive.");
            }

            if (node.getDriverFile() != null && !node.getDriverFile().isFile()) {
                throw new BuildException("'node.driverFile' does not exist.");
            }
        }
    }

    /**
     * Performs the actual work on the project.
     */
    protected void processProject() throws ProjectException {
        ProjectConfigurator conf = new ProjectConfigurator(info);
        conf.execute();
    }

    public void setSrc(File file) {
        info.setSourceJar(file);
    }

    public void setDest(File file) {
        info.setDestJar(file);
    }

    public void setAltProjectFile(File file) {
        info.setAltProjectFile(file);
    }

    public void addNode(DataNodeConfigInfo node) {
        info.addToNodes(node);
    }
}
