/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DataPort;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.FileConfiguration;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.util.Util;

/**
 * A "cdataport" Ant task implementing a frontend to DataPort allowing porting database
 * data using Ant build scripts.
 * 
 * @author Andrei Adamchik
 * @since 1.2: Prior to 1.2 DataPort classes were a part of cayenne-examples package.
 */
public class DataPortTask extends CayenneTask {

    protected File projectFile;
    protected String maps;
    protected String srcNode;
    protected String destNode;
    protected String includeTables;
    protected String excludeTables;
    protected boolean cleanDest = true;

    public DataPortTask() {
        // set defaults
        this.cleanDest = true;
    }

    public void execute() throws BuildException {
        configureLogging();
        validateParameters();

        FileConfiguration configuration = new FileConfiguration(projectFile);

        try {
            configuration.initialize();
        }
        catch (Exception ex) {
            throw new BuildException("Error loading Cayenne configuration from "
                    + projectFile, ex);
        }

        // perform project validation
        DataNode source = findNode(configuration, srcNode);
        if (source == null) {
            throw new BuildException("srcNode not found in the project: " + srcNode);
        }

        DataNode destination = findNode(configuration, destNode);
        if (destination == null) {
            throw new BuildException("destNode not found in the project: " + destNode);
        }

        log("Porting from '" + srcNode + "' to '" + destNode + "'.");

        AntDataPortDelegate portDelegate = new AntDataPortDelegate(
                this,
                maps,
                includeTables,
                excludeTables);
        DataPort dataPort = new DataPort(portDelegate);
        dataPort.setEntities(getAllEntities(source, destination));
        dataPort.setCleaningDestination(cleanDest);
        dataPort.setSourceNode(source);
        dataPort.setDestinationNode(destination);

        try {
            dataPort.execute();
        }
        catch (Exception e) {
            Throwable topOfStack = Util.unwindException(e);
            throw new BuildException(
                    "Error porting data: " + topOfStack.getMessage(),
                    topOfStack);
        }
    }

    protected DataNode findNode(Configuration configuration, String name) {
        Iterator domains = configuration.getDomains().iterator();
        while (domains.hasNext()) {
            DataDomain domain = (DataDomain) domains.next();
            DataNode node = domain.getNode(name);
            if (node != null) {
                return node;
            }
        }

        return null;
    }

    protected Collection getAllEntities(DataNode source, DataNode target) {
        // use a set to exclude duplicates, though a valid project will probably have
        // none...
        Collection allEntities = new HashSet();

        Iterator maps = source.getDataMaps().iterator();
        while (maps.hasNext()) {
            DataMap map = (DataMap) maps.next();
            allEntities.addAll(map.getDbEntities());
        }

        maps = target.getDataMaps().iterator();
        while (maps.hasNext()) {
            DataMap map = (DataMap) maps.next();
            allEntities.addAll(map.getDbEntities());
        }

        log("Number of entities: " + allEntities.size(), Project.MSG_VERBOSE);

        if (allEntities.size() == 0) {
            log("No entities found for either source or target.");
        }
        return allEntities;
    }

    protected void validateParameters() throws BuildException {
        if (projectFile == null) {
            throw new BuildException("Required 'projectFile' parameter is missing.");
        }

        if (!projectFile.exists()) {
            throw new BuildException("'projectFile' does not exist: " + projectFile);
        }

        if (srcNode == null) {
            throw new BuildException("Required 'srcNode' parameter is missing.");
        }

        if (destNode == null) {
            throw new BuildException("Required 'destNode' parameter is missing.");
        }
    }

    public void setDestNode(String destNode) {
        this.destNode = destNode;
    }

    public void setExcludeTables(String excludeTables) {
        this.excludeTables = excludeTables;
    }

    public void setIncludeTables(String includeTables) {
        this.includeTables = includeTables;
    }

    public void setMaps(String maps) {
        this.maps = maps;
    }

    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
    }

    public void setSrcNode(String srcNode) {
        this.srcNode = srcNode;
    }

    public void setCleanDest(boolean flag) {
        this.cleanDest = flag;
    }
}