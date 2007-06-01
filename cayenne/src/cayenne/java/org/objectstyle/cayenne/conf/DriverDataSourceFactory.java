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

import java.io.InputStream;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.util.ConnectionEventLogger;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.util.AbstractHandler;
import org.objectstyle.cayenne.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/** 
 * Creates DataSource objects from XML configuration files that
 * describe a JDBC driver. Wraps JDBC driver in a generic DataSource 
 * implementation.
 *
 * @author Andrei Adamchik
 */
public class DriverDataSourceFactory implements DataSourceFactory {
    private static Logger logObj =
        Logger.getLogger(DriverDataSourceFactory.class);

    protected XMLReader parser;
    protected DataSourceInfo driverInfo;
    protected Level logLevel = Level.DEBUG;
    protected Configuration parentConfiguration;

    /**
     * Default constructor
     * @throws Exception
     */
    public DriverDataSourceFactory() throws Exception {
        super();
        this.parser = Util.createXmlReader();
    }

    /**
     * @see DataSourceFactory#initializeWithParentConfiguration(Configuration)
     */
    public void initializeWithParentConfiguration(Configuration conf) {
        this.parentConfiguration = conf;
    }

    /**
     * @see DataSourceFactory#getDataSource(String)
     */
    public DataSource getDataSource(String location) throws Exception {
        return this.getDataSource(location, Level.DEBUG);
    }

    /**
     * @see DataSourceFactory#getDataSource(String, Level)
     */
    public DataSource getDataSource(String location, Level logLevel)
        throws Exception {
        this.logLevel = logLevel;
        this.load(location);
        
        ConnectionEventLogger logger = new ConnectionEventLogger(Level.INFO);
        
        try {
            PoolManager pm =
                new PoolManager(
                    driverInfo.getJdbcDriver(),
                    driverInfo.getDataSourceUrl(),
                    driverInfo.getMinConnections(),
                    driverInfo.getMaxConnections(),
                    driverInfo.getUserName(),
                    driverInfo.getPassword(),
                    logger);

            return pm;
        } catch (Exception ex) {
            QueryLogger.logConnectFailure(logLevel, ex);
            throw ex;
        }
    }

    /**
     * Returns DataSourceInfo property.
     */
    protected DataSourceInfo getDriverInfo() {
        return this.driverInfo;
    }

    protected InputStream getInputStream(String location) {
        if (this.parentConfiguration == null) {
            throw new ConfigurationException("No parent Configuration set - cannot continue.");
        }

        return this.parentConfiguration.getResourceLocator()
        			.findResourceStream(location);
    }

    /**
     * Loads driver information from the file at <code>location</code>.
     * Called internally from "getDataSource"
     */
    protected void load(String location) throws Exception {
        logObj.log(
            logLevel,
            "loading driver information from '" + location + "'.");

        InputStream in = this.getInputStream(location);
        if (in == null) {
            logObj.log(
                logLevel,
                "Error: location '" + location + "' not found.");
            throw new ConfigurationException(
                "Can't find DataSource configuration file at " + location);
        }

        RootHandler handler = new RootHandler();
        parser.setContentHandler(handler);
        parser.setErrorHandler(handler);
        parser.parse(new InputSource(in));
    }

    // SAX handlers start below

    /** Handler for the root element. Its only child must be the "driver" element. */
    private class RootHandler extends DefaultHandler {
        /**
         * Handles the start of a "driver" element. A driver handler is created
         * and initialized with the element name and attributes.
         * 
         * @exception SAXException if the tag given is not 
         *                              <code>"driver"</code>
         */
        public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
            if (localName.equals("driver")) {
                new DriverHandler(parser, this).init(localName, atts);
            } else {
                logObj.log(
                    logLevel,
                    "<driver> must be the root element. <"
                        + localName
                        + "> is unexpected.");
                throw new SAXException(
                    "Config file is not of expected XML type. '"
                        + localName
                        + "' unexpected.");
            }
        }
    }

    /** Handler for the "driver" element. */
    private class DriverHandler extends AbstractHandler {
        public DriverHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs) throws SAXException {
            String className = attrs.getValue("", "class");
            logObj.log(logLevel, "loading driver " + className);
            driverInfo = new DataSourceInfo();
            driverInfo.setJdbcDriver(className);
        }

        /**
          * Handles the start of a driver child element. An appropriate handler 
          * is created and initialized with the element name and attributes.
          * 
          * @exception SAXException if the tag given is not recognized.
          */
        public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
            if (localName.equals("login")) {
                new LoginHandler(this.parser, this).init(
                    localName,
                    atts,
                    driverInfo);
            } else if (localName.equals("url")) {
                new UrlHandler(this.parser, this).init(
                    localName,
                    atts,
                    driverInfo);
            } else if (localName.equals("connectionPool")) {
                new ConnectionHandler(this.parser, this).init(
                    localName,
                    atts,
                    driverInfo);
            } else {
                logObj.log(
                    logLevel,
                    "<login, url, connectionPool> are valid. <"
                        + localName
                        + "> is unexpected.");
                throw new SAXException("Config file is not of expected XML type");
            }
        }

    }

    private class UrlHandler extends AbstractHandler {
        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public UrlHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(
            String name,
            Attributes atts,
            DataSourceInfo driverInfo)
            throws SAXException {
            driverInfo.setDataSourceUrl(atts.getValue("value"));
            if (driverInfo.getDataSourceUrl() == null) {
                logObj.log(logLevel, "error: <url> has no 'value'.");
                throw new SAXException("'<url value=' attribute is required.");
            }
        }
    }

    private class LoginHandler extends AbstractHandler {
        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public LoginHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(
            String name,
            Attributes atts,
            DataSourceInfo driverInfo)
            throws SAXException {
            logObj.log(logLevel, "loading user name and password.");
            driverInfo.setUserName(atts.getValue("userName"));
            driverInfo.setPassword(atts.getValue("password"));
        }
    }

    private class ConnectionHandler extends AbstractHandler {
        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         */
        public ConnectionHandler(
            XMLReader parser,
            ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(
            String name,
            Attributes atts,
            DataSourceInfo driverInfo)
            throws SAXException {
            try {
                String min = atts.getValue("min");
                if (min != null)
                    driverInfo.setMinConnections(Integer.parseInt(min));

                String max = atts.getValue("max");
                if (max != null)
                    driverInfo.setMaxConnections(Integer.parseInt(max));
            } catch (NumberFormatException nfex) {
                logObj.log(logLevel, "Error loading numeric attribute", nfex);
                throw new SAXException(
                    "Error reading numeric attribute.",
                    nfex);
            }
        }
    }
}