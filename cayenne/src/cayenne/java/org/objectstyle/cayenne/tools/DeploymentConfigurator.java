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
package org.objectstyle.cayenne.tools;

import java.io.File;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.project.DataNodeConfigInfo;
import org.objectstyle.cayenne.project.ProjectConfigInfo;
import org.objectstyle.cayenne.project.ProjectConfigurator;
import org.objectstyle.cayenne.project.ProjectException;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public class DeploymentConfigurator extends Task {
    static {
        // init logging properties
        Configuration.configureCommonLogging();
    }

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
    public void execute() throws BuildException {
        validateAttributes();

        try {
            processProject();
        } catch (Exception ex) {
        	Throwable th = Util.unwindException(ex);
            String message = th.getMessage();
            StringBuffer buf = new StringBuffer();

            if (message != null && message.trim().length() > 0) {
                buf.append("Error: [").append(message).append("].");
            } else {
                buf.append("Error reconfiguring jar file.");
            }

            buf.append(" Source: ").append(info.getSourceJar()).append(
                "; target: ").append(
                info.getDestJar());

            String errorMessage = buf.toString();
            super.log(errorMessage);
            throw new BuildException(errorMessage, ex);
        }
    }

    /**
     * Performs validation of task attributes. Throws BuildException if
     * validation fails.
     */
    protected void validateAttributes() throws BuildException {
        if (info.getSourceJar() == null) {
            throw new BuildException("'src' attribute is required.");
        }

        if (!info.getSourceJar().isFile()) {
            throw new BuildException(
                "'src' must be a valid file: " + info.getSourceJar());
        }

        if (info.getAltProjectFile() != null
            && !info.getAltProjectFile().isFile()) {
            throw new BuildException(
                "'altProjectFile' must be a valid file: "
                    + info.getAltProjectFile());
        }

        Iterator nodes = info.getNodes().iterator();
        while (nodes.hasNext()) {
            DataNodeConfigInfo node = (DataNodeConfigInfo) nodes.next();
            if (node.getName() == null) {
                throw new BuildException("'node.name' attribute is required.");
            }

            if (node.getDataSource() != null && node.getDriverFile() != null) {
                throw new BuildException("'node.dataSource' and 'node.driverFile' are mutually exclusive.");
            }

            if (node.getDriverFile() != null
                && !node.getDriverFile().isFile()) {
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
