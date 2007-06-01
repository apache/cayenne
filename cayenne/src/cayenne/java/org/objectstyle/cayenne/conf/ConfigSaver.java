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

import java.io.PrintWriter;
import java.util.Iterator;

import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.util.Util;

/**
 * Class that does saving of Cayenne configuration.
 * 
 * @author Andrei Adamchik
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
     * Saves domains into the specified file. Assumes that the maps have already
     * been saved.
     */
    public void storeDomains(PrintWriter pw) {
        pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        pw.println("<domains project-version=\"" + delegate.projectVersion() + "\">");

        Iterator it = delegate.domainNames();
        while (it.hasNext()) {
            storeDomain(pw, (String) it.next());
        }

        Iterator views = delegate.viewNames();
        while (views.hasNext()) {
            storeDataView(pw, (String) views.next());
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
            pw.println(">");

            while (mapNames.hasNext()) {
                String mapName = (String) mapNames.next();
                pw.println("\t\t\t<map-ref name=\"" + mapName.trim() + "\"/>");
            }
            pw.println("\t </node>");
        }
        pw.println("</domain>");
    }

    /**
     * Stores DataSolurceInfo to the specified PrintWriter.
     * <code>info</code> object may contain full or partial information.
     */
    public void storeDataNode(PrintWriter out, DataSourceInfo info) {
        out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        out.print("<driver project-version=\"" + Project.CURRENT_PROJECT_VERSION + "\"");
        if (info.getJdbcDriver() != null) {
            out.print(" class=\"" + info.getJdbcDriver() + "\"");
        }
        out.println(">");

        if (info.getDataSourceUrl() != null) {
            String encoded = Util.encodeXmlAttribute(info.getDataSourceUrl());
            out.println("\t<url value=\"" + encoded + "\"/>");
        }

        out.println(
            "\t<connectionPool min=\""
                + info.getMinConnections()
                + "\" max=\""
                + info.getMaxConnections()
                + "\" />");

        if (info.getUserName() != null || info.getPassword() != null) {
            out.print("\t<login");
            if (info.getUserName() != null) {
                String encoded = Util.encodeXmlAttribute(info.getUserName());
                out.print(" userName=\"" + encoded + "\"");
            }
            if (info.getPassword() != null) {
                String encoded = Util.encodeXmlAttribute(info.getPassword());
                out.print(" password=\"" + encoded + "\"");
            }
            out.println("/>");
        }

        out.println("</driver>");
    }
}
