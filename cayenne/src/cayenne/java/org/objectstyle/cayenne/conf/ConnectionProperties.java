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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.project.CayenneUserDir;

/**
 * ConnectionProperties handles a set of DataSourceInfo objects 
 * using information stored in $HOME/.cayenne/connection.properties. 
 * As of now this is purely a utility class. Its features are not used
 * in deployment.
 * 
 * @author Andrei Adamchik
 */
public class ConnectionProperties {
    static final Logger logObj = Logger.getLogger(ConnectionProperties.class);

    public static final String PROPERTIES_FILE = "connection.properties";
    public static final String ADAPTER_KEY = "cayenne.adapter";
    public static final String USER_NAME_KEY = "jdbc.username";
    public static final String PASSWORD_KEY = "jdbc.password";
    public static final String URL_KEY = "jdbc.url";
    public static final String DRIVER_KEY = "jdbc.driver";

    protected static ConnectionProperties sharedInstance;
    protected Map connectionInfos = Collections.synchronizedMap(new HashMap());

    static {
        sharedInstance = loadDefaultProperties();
    }

    /**
     * Returns ConnectionProperties singleton.
     */
    public static ConnectionProperties getInstance() {
        return sharedInstance;
    }

    /**
     * Loads connection properties from $HOME/.cayenne/connection.properties.
     */
    protected static ConnectionProperties loadDefaultProperties() {
        File f = CayenneUserDir.getInstance().resolveFile(PROPERTIES_FILE);

        try {
            if (f.exists()) {
                return new ConnectionProperties(
                    new ExtendedProperties(f.getAbsolutePath()));
            } else {
                // lets touch this file so that users would get a clue of what it is
                createSamplePropertiesFile(f);
            }
        } catch (IOException e) {
            logObj.warn("Error loading connection properties. Ignoring..", e);
        }

        return new ConnectionProperties(new ExtendedProperties());
    }

    protected static void createSamplePropertiesFile(File f) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(f));

        try {
            out.write("# Cayenne named connections configuration file.");
            out.newLine();

            out.write("#");
            out.newLine();
            out.write("# Sample named connections (named 'example1' and 'example2'): ");
            out.newLine();

            out.write("#");
            out.newLine();
            out.write(
                "# example1."
                    + ADAPTER_KEY
                    + " = org.objectstyle.cayenne.dba.mysql.MySQLAdapter");
            out.newLine();
            out.write("# example1." + USER_NAME_KEY + " = some_user");
            out.newLine();
            out.write("# example1." + PASSWORD_KEY + " = some_passwd");
            out.newLine();
            out.write("# example1." + URL_KEY + " = jdbc:mysql://noise/cayenne");
            out.newLine();
            out.write("# example1." + DRIVER_KEY + " = org.gjt.mm.mysql.Driver");
            out.newLine();

            // example 2
            out.write("#");
            out.newLine();
            out.write(
                "# example2."
                    + ADAPTER_KEY
                    + " = org.objectstyle.cayenne.dba.mysql.MySQLAdapter");
            out.newLine();
            out.write("# example2." + USER_NAME_KEY + " = some_user");
            out.newLine();
            out.write("# example2." + PASSWORD_KEY + " = some_passwd");
            out.newLine();
            out.write("# example2." + URL_KEY + " = jdbc:mysql://noise/cayenne");
            out.newLine();
            out.write("# example2." + DRIVER_KEY + " = org.gjt.mm.mysql.Driver");
            out.newLine();
        } finally {
            out.close();
        }
    }

    /**
     * Constructor for ConnectionProperties.
     */
    public ConnectionProperties(ExtendedProperties props) {
        Iterator names = extractNames(props).iterator();
        while (names.hasNext()) {
            String name = (String) names.next();
            DataSourceInfo dsi = buildDataSourceInfo(props.subset(name));
            connectionInfos.put(name, dsi);
        }
    }

    /**
     * Returns DataSourceInfo object for a symbolic name.
     * If name does not match an existing object, returns null.
     */
    public DataSourceInfo getConnectionInfo(String name) {
        synchronized (connectionInfos) {
            return (DataSourceInfo) connectionInfos.get(name);
        }
    }

    /**
    * Creates a DataSourceInfo object from a set of properties.
    */
    protected DataSourceInfo buildDataSourceInfo(ExtendedProperties props) {
        DataSourceInfo dsi = new DataSourceInfo();

        dsi.setAdapterClassName(props.getString(ADAPTER_KEY));
        dsi.setUserName(props.getString(USER_NAME_KEY));
        dsi.setPassword(props.getString(PASSWORD_KEY));
        dsi.setDataSourceUrl(props.getString(URL_KEY));
        dsi.setJdbcDriver(props.getString(DRIVER_KEY));

        return dsi;
    }

    /**
     * Returns a list of connection names configured
     * in the properties object.
     */
    protected List extractNames(ExtendedProperties props) {
        Iterator it = props.getKeys();
        List list = new ArrayList();

        while (it.hasNext()) {
            String key = (String) it.next();

            int dotInd = key.indexOf('.');
            if (dotInd <= 0 || dotInd >= key.length()) {
                continue;
            }

            String name = key.substring(0, dotInd);
            if (!list.contains(name)) {
                list.add(name);
            }
        }

        return list;
    }
}
