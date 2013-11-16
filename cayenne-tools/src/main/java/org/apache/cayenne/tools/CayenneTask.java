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

import javax.sql.DataSource;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.xml.sax.InputSource;

/**
 * Base task for all Cayenne ant tasks, providing support for common
 * configuration items.
 * 
 * @since 1.2
 */
public abstract class CayenneTask extends Task {
    protected Path classpath;

    protected String adapter;
    protected File map;
    protected String driver;
    protected String url;
    protected String userName;
    protected String password;

    /**
     * Sets the classpath used by the task.
     * 
     * @param path
     *            The classpath to set.
     */
    public void setClasspath(Path path) {
        createClasspath().append(path);
    }

    /**
     * Sets the classpath reference used by the task.
     * 
     * @param reference
     *            The classpath reference to set.
     */
    public void setClasspathRef(Reference reference) {
        createClasspath().setRefid(reference);
    }

    /**
     * Convenience method for creating a classpath instance to be used for the
     * task.
     * 
     * @return The new classpath.
     */
    private Path createClasspath() {
        if (null == classpath) {
            classpath = new Path(getProject());
        }

        return classpath.createPath();
    }

    /**
     * Sets the map.
     * 
     * @param map
     *            The map to set
     */
    public void setMap(File map) {
        this.map = map;
    }

    /**
     * Sets the db adapter.
     * 
     * @param adapter
     *            The db adapter to set.
     */
    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    /**
     * Sets the JDBC driver used to connect to the database server.
     * 
     * @param driver
     *            The driver to set.
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Sets the JDBC URL used to connect to the database server.
     * 
     * @param url
     *            The url to set.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the username used to connect to the database server.
     */
    public void setUserName(String username) {
        this.userName = username;
    }

    /**
     * Sets the password used to connect to the database server.
     * 
     * @param password
     *            The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap loadDataMap() throws Exception {
        InputSource in = new InputSource(map.getCanonicalPath());
        return new MapLoader().loadDataMap(in);
    }

    protected DbAdapter getAdapter(Injector injector, DataSource dataSource)
            throws Exception {

        DbAdapterFactory adapterFactory = injector
                .getInstance(DbAdapterFactory.class);

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setAdapterType(adapter);

        return adapterFactory.createAdapter(nodeDescriptor, dataSource);
    }
}
