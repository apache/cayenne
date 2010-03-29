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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.util.Util;

/**
 * Class that does saving of Cayenne configuration.
 */
public class ConfigSaver {

    protected ConfigSaverDelegate delegate;

    /**
     * Constructor for ConfigSaver.
     */
    public ConfigSaver() {
        super();
    }

    /**
     * Constructor for ConfigSaver.
     */
    public ConfigSaver(ConfigSaverDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Saves domains into the specified file. Assumes that the maps have already been
     * saved.
     */
    public void storeDomains(PrintWriter pw) {
        pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        pw.println("<domains project-version=\"" + delegate.projectVersion() + "\">");

        Iterator it = delegate.domainNames();
        while (it.hasNext()) {
            storeDomain(pw, (String) it.next());
        }

        Iterator<String> views = delegate.viewNames();
        while (views.hasNext()) {
            storeDataView(pw, views.next());
        }
        pw.println("</domains>");
    }

    protected void storeDataView(PrintWriter pw, String dataViewName) {
        String location = delegate.viewLocation(dataViewName);
        pw.print("<view name=\"" + dataViewName.trim());
        pw.print("\" location=\"" + location.trim());
        pw.println("\"/>");
    }

    protected void storeDomain(PrintWriter pw, String domainName) {
        pw.println("<domain name=\"" + domainName.trim() + "\">");

        // store properties
        Iterator properties = delegate.propertyNames(domainName);
        boolean breakNeeded = properties.hasNext();
        while (properties.hasNext()) {
            String name = (String) properties.next();
            if (name == null) {
                continue;
            }

            String value = delegate.propertyValue(domainName, name);
            if (value == null) {
                continue;
            }

            pw.print("\t<property name=\"" + Util.encodeXmlAttribute(name.trim()));
            pw.println("\" value=\"" + Util.encodeXmlAttribute(value.trim()) + "\"/>");
        }

        // store maps
        Iterator maps = delegate.mapNames(domainName);
        if (maps.hasNext()) {
            if (breakNeeded) {
                pw.println();
            }

            breakNeeded = true;
        }

        while (maps.hasNext()) {
            String mapName = (String) maps.next();
            String mapLocation = delegate.mapLocation(domainName, mapName);

            pw.print("\t<map name=\"" + mapName.trim());
            pw.print("\" location=\"" + mapLocation.trim());
            pw.println("\"/>");
        }

        // store nodes
        Iterator nodes = delegate.nodeNames(domainName);
        if (nodes.hasNext() && breakNeeded) {
            pw.println();
        }
        while (nodes.hasNext()) {
            String nodeName = (String) nodes.next();
            String datasource = delegate.nodeDataSourceName(domainName, nodeName);
            String adapter = delegate.nodeAdapterName(domainName, nodeName);
            String factory = delegate.nodeFactoryName(domainName, nodeName);
            String schemaUpdateStrategy = delegate.nodeSchemaUpdateStrategyName(
                    domainName,
                    nodeName);
            Iterator mapNames = delegate.linkedMapNames(domainName, nodeName);

            pw.println("\t<node name=\"" + nodeName.trim() + "\"");

            if (datasource != null) {
                datasource = datasource.trim();
                pw.print("\t\t datasource=\"" + datasource + "\"");
            }
            pw.println("");

            if (adapter != null) {
                pw.println("\t\t adapter=\"" + adapter + "\"");
            }

            if (factory != null) {
                pw.print("\t\t factory=\"" + factory.trim() + "\"");
            }

            if (schemaUpdateStrategy != null
                    && !(schemaUpdateStrategy.equals(SkipSchemaUpdateStrategy.class.getName()))) {
                pw.println("");
                pw.print("\t\t schema-update-strategy=\""
                        + schemaUpdateStrategy.trim()
                        + "\"");
            }
            pw.println(">");

            while (mapNames.hasNext()) {
                String mapName = (String) mapNames.next();
                pw.println("\t\t\t<map-ref name=\"" + mapName.trim() + "\"/>");
            }
            pw.println("\t </node>");
        }
        pw.println("</domain>");
    }

    private String attribute(String key, String value) {
        if (value != null) {
            return " " + key + "=\"" + Util.encodeXmlAttribute(value) + "\"";
        }
        else {
            return "";
        }
    }

    /**
     * Stores DataSolurceInfo to the specified PrintWriter. <code>info</code> object may
     * contain full or partial information.
     */
    public void storeDataNode(PrintWriter pw, Project project, DataSourceInfo info) {
        pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        pw.println("<driver"
                + attribute("project-version", Project.CURRENT_PROJECT_VERSION)
                + attribute("class", info.getJdbcDriver())
                + ">");

        if (info.getDataSourceUrl() != null)
            pw.println("\t<url" + attribute("value", info.getDataSourceUrl()) + "/>");

        pw.println("\t<connectionPool"
                + attribute("min", String.valueOf(info.getMinConnections()))
                + attribute("max", String.valueOf(info.getMaxConnections()))
                + "/>");

        pw.print("\t<login");

        if (info.getUserName() != null) {
            pw.print(attribute("userName", info.getUserName()));
        }

        if (info.getPasswordLocation().equals(DataSourceInfo.PASSWORD_LOCATION_MODEL)) {
            PasswordEncoding encoder = info.getPasswordEncoder();
            if (encoder != null && info.getPassword() != null) {
                pw.print(attribute("password", encoder.encodePassword(
                        info.getPassword(),
                        info.getPasswordEncoderKey())));
            }
        }
        else if (info.getPasswordLocation().equals(
                DataSourceInfo.PASSWORD_LOCATION_CLASSPATH)) {
            if (info.getPasswordSource() != null) {
                File passwordFile = new File(project.getProjectDirectory()
                        + File.separator
                        + info.getPasswordSource());
                PasswordEncoding encoder = info.getPasswordEncoder();
                if (encoder != null) {
                    try {
                        PrintStream out = new PrintStream(new FileOutputStream(
                                passwordFile));
                        out.print(encoder.encodePassword(info.getPassword(), info
                                .getPasswordEncoderKey()));
                        out.close();
                    }
                    catch (FileNotFoundException ex) {
                        throw new CayenneRuntimeException(ex);
                    }
                }
            }
        }

        if (info.getPasswordEncoderClass() != null
                && !PlainTextPasswordEncoder.class.getName().equals(
                        info.getPasswordEncoderClass())) {
            pw.print(attribute("encoderClass", info.getPasswordEncoderClass()));
        }

        if (info.getPasswordEncoderKey() != null
                && info.getPasswordEncoderKey().length() > 0) {
            pw.print(attribute("encoderKey", info.getPasswordEncoderKey()));
        }

        if (info.getPasswordLocation() != null
                && !info.getPasswordLocation().equals(
                        DataSourceInfo.PASSWORD_LOCATION_MODEL)) {
            pw.print(attribute("passwordLocation", info.getPasswordLocation()));
        }

        // TODO: this is very not nice... we need to clean up the whole DataSourceInfo to
        // avoid returning arbitrary labels...
        if (info.getPasswordSource() != null
                && info.getPasswordSource().length() > 0
                && !"Not Applicable".equals(info.getPasswordSource())) {
            pw.print(attribute("passwordSource", info.getPasswordSource()));
        }

        pw.println("/>");

        pw.println("</driver>");
    }
}
