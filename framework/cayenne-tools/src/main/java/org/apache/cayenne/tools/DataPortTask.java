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
import java.util.Collection;
import java.util.HashSet;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DataPort;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.resource.FilesystemResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.util.Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * A "cdataport" Ant task implementing a frontend to DataPort allowing porting
 * database data using Ant build scripts.
 * 
 * @since 1.2: Prior to 1.2 DataPort classes were a part of cayenne-examples
 *        package.
 * @deprecated since 3.2
 */
@Deprecated
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

    @Override
    public void execute() throws BuildException {

        log("*** 'cdataport' task is deprecated and will be removed after 3.2",
                Project.MSG_WARN);

        validateParameters();

        String projectFileLocation = projectFile.getName();
        Module dataPortModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ResourceLocator.class).toInstance(
                        new FilesystemResourceLocator(projectFile));
            }
        };

        ServerRuntime runtime = new ServerRuntime(projectFileLocation,
                dataPortModule);
        DataDomain domain;

        ClassLoader threadContextClassLoader = Thread.currentThread()
                .getContextClassLoader();
        try {
            // need to set context class loader so that cayenne can find jdbc
            // driver and
            // PasswordEncoder
            // TODO: andrus 04/11/2010 is this still relevant in 3.1?
            Thread.currentThread().setContextClassLoader(
                    getClass().getClassLoader());

            domain = runtime.getDataDomain();
        } catch (Exception ex) {
            throw new BuildException(
                    "Error loading Cayenne configuration from " + projectFile,
                    ex);
        } finally {
            // set back to original ClassLoader
            Thread.currentThread().setContextClassLoader(
                    threadContextClassLoader);
        }

        // perform project validation
        DataNode source = domain.getDataNode(srcNode);
        if (source == null) {
            throw new BuildException("srcNode not found in the project: "
                    + srcNode);
        }

        DataNode destination = domain.getDataNode(destNode);
        if (destination == null) {
            throw new BuildException("destNode not found in the project: "
                    + destNode);
        }

        log("Porting from '" + srcNode + "' to '" + destNode + "'.");

        AntDataPortDelegate portDelegate = new AntDataPortDelegate(this, maps,
                includeTables, excludeTables);
        DataPort dataPort = new DataPort(portDelegate);
        dataPort.setEntities(getAllEntities(source, destination));
        dataPort.setCleaningDestination(cleanDest);
        dataPort.setSourceNode(source);
        dataPort.setDestinationNode(destination);

        try {
            dataPort.execute();
        } catch (Exception e) {
            Throwable topOfStack = Util.unwindException(e);
            throw new BuildException("Error porting data: "
                    + topOfStack.getMessage(), topOfStack);
        }
    }

    protected Collection<DbEntity> getAllEntities(DataNode source,
            DataNode target) {
        // use a set to exclude duplicates, though a valid project will probably
        // have
        // none...
        Collection<DbEntity> allEntities = new HashSet<DbEntity>();

        for (DataMap map : source.getDataMaps()) {
            allEntities.addAll(map.getDbEntities());
        }

        for (DataMap map : target.getDataMaps()) {
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
            throw new BuildException(
                    "Required 'projectFile' parameter is missing.");
        }

        if (!projectFile.exists()) {
            throw new BuildException("'projectFile' does not exist: "
                    + projectFile);
        }

        if (srcNode == null) {
            throw new BuildException("Required 'srcNode' parameter is missing.");
        }

        if (destNode == null) {
            throw new BuildException(
                    "Required 'destNode' parameter is missing.");
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
