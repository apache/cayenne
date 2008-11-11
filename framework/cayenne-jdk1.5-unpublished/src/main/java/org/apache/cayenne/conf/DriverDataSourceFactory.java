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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sql.DataSource;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.ConnectionLogger;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Creates DataSource objects from XML configuration files that describe a JDBC driver.
 * Wraps JDBC driver in a generic DataSource implementation.
 * 
 */
// TODO: factory shouldn't contain any state specific to location ("driverInfo" ivar
// should go, and probably "parser" too)... Otherwise the API doesn't make sense -
// sequential invocations of getDataSource() will have side effects....
public class DriverDataSourceFactory implements DataSourceFactory {

    private static final Log logger = LogFactory.getLog(DriverDataSourceFactory.class);

    protected XMLReader parser;
    protected DataSourceInfo driverInfo;
    protected Configuration parentConfiguration;

    /**
     * Creates new DriverDataSourceFactory.
     */
    public DriverDataSourceFactory() throws Exception {
        this.parser = Util.createXmlReader();
    }

    /**
     * Stores configuration object internally to use it later for resource loading.
     */
    public void initializeWithParentConfiguration(Configuration parentConfiguration) {
        this.parentConfiguration = parentConfiguration;
    }

    public DataSource getDataSource(String location) throws Exception {
        this.load(location);

        ConnectionLogger logger = new ConnectionLogger();

        try {
            return new PoolManager(driverInfo.getJdbcDriver(), driverInfo
                    .getDataSourceUrl(), driverInfo.getMinConnections(), driverInfo
                    .getMaxConnections(), driverInfo.getUserName(), driverInfo
                    .getPassword(), logger);
        }
        catch (Exception ex) {
            QueryLogger.logConnectFailure(ex);
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
            throw new ConfigurationException(
                    "No parent Configuration set - cannot continue.");
        }

        URL url = parentConfiguration.getResourceFinder().getResource(location);

        try {
            return url != null ? url.openStream() : null;
        }
        catch (IOException e) {
            throw new ConfigurationException("Error reading URL " + url, e);
        }
    }

    /**
     * Loads driver information from the file at <code>location</code>. Called
     * internally from "getDataSource"
     */
    // TODO: andrus 2008/04/22, while this never caused any troubles, storing loaded
    // DataSourceInfo in an ivar clearly violates the scope logic, as "location" is a
    // local variable.
    protected void load(String location) throws Exception {
        logger.info("loading driver information from '" + location + "'.");

        InputStream in = getInputStream(location);
        if (in == null) {
            logger.info("Error: location '" + location + "' not found.");
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
         * Handles the start of a "driver" element. A driver handler is created and
         * initialized with the element name and attributes.
         * 
         * @exception SAXException if the tag given is not <code>"driver"</code>
         */
        @Override
        public void startElement(
                String namespaceURI,
                String localName,
                String qName,
                Attributes atts) throws SAXException {
            if (localName.equals("driver")) {
                new DriverHandler(parser, this).init(localName, atts);
            }
            else {
                logger.info("<driver> must be the root element. <"
                        + localName
                        + "> is unexpected.");
                throw new SAXException("Config file is not of expected XML type. '"
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

        public void init(String name, Attributes attrs) {
            String className = attrs.getValue("", "class");
            logger.info("loading driver " + className);
            driverInfo = new DataSourceInfo();
            driverInfo.setJdbcDriver(className);
        }

        /**
         * Handles the start of a driver child element. An appropriate handler is created
         * and initialized with the element name and attributes.
         * 
         * @exception SAXException if the tag given is not recognized.
         */
        @Override
        public void startElement(
                String namespaceURI,
                String localName,
                String qName,
                Attributes atts) throws SAXException {
            if (localName.equals("login")) {
                new LoginHandler(this.parser, this).init(localName, atts, driverInfo);
            }
            else if (localName.equals("url")) {
                new UrlHandler(this.parser, this).init(localName, atts, driverInfo);
            }
            else if (localName.equals("connectionPool")) {
                new ConnectionHandler(this.parser, this)
                        .init(localName, atts, driverInfo);
            }
            else {
                logger.info("<login, url, connectionPool> are valid. <"
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
         * @param parentHandler The handler which should be restored to the parser at the
         *            end of the element. Must not be <code>null</code>.
         */
        public UrlHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes atts, DataSourceInfo driverInfo)
                throws SAXException {
            driverInfo.setDataSourceUrl(atts.getValue("value"));
            if (driverInfo.getDataSourceUrl() == null) {
                logger.info("error: <url> has no 'value'.");
                throw new SAXException("'<url value=' attribute is required.");
            }
        }
    }

    private class LoginHandler extends AbstractHandler {
        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the parser at the
         *            end of the element. Must not be <code>null</code>.
         */
        public LoginHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        private String passwordFromInputStream(InputStream inputStream) {
            BufferedReader bufferedReader = null;
            InputStreamReader inputStreamReader = null;
            String password = null;

            try {
                inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);
                password = bufferedReader.readLine();
            }
            catch (IOException exception) {
                logger.warn(exception);
            }
            finally {
                try {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                }
                catch (Exception exception) {
                }

                try {
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                }
                catch (Exception exception) {
                }

                try {
                    inputStream.close();
                }
                catch (IOException exception) {
                }
            }

            return password;
        }

        private String passwordFromURL(URL url) {
            InputStream inputStream = null;
            String password = null;

            try {
                inputStream = url.openStream();
                password = passwordFromInputStream(inputStream);
            }
            catch (IOException exception) {
                // Log the error while trying to open the stream. A null
                // password will be returned as a result.
                logger.warn(exception);
            }

            return password;
        }

        public void init(String name, Attributes atts, DataSourceInfo driverInfo) {
            logger.info("loading user name and password.");

            String encoderClass = atts.getValue("encoderClass");
            String encoderKey = atts.getValue("encoderKey") == null ? atts
                    .getValue("encoderSalt") : atts.getValue("encoderKey");
            String password = atts.getValue("password");
            String passwordLocation = atts.getValue("passwordLocation");
            String passwordSource = atts.getValue("passwordSource");
            if(passwordSource == null) {
                passwordSource = DataSourceInfo.PASSWORD_LOCATION_MODEL;
            }
            
            String username = atts.getValue("userName");

            driverInfo.setPasswordEncoderClass(encoderClass);
            driverInfo.setPasswordEncoderKey(encoderKey);
            driverInfo.setPasswordLocation(passwordLocation);
            driverInfo.setPasswordSource(passwordSource);
            driverInfo.setUserName(username);

            // Replace {} in passwordSource with encoderSalt -- useful for EXECUTABLE &
            // URL options
            if (encoderKey != null) {
                passwordSource = passwordSource.replaceAll("\\{\\}", encoderKey);
            }

            PasswordEncoding passwordEncoder = driverInfo.getPasswordEncoder();

            if (passwordLocation != null) {
                if (passwordLocation.equals(DataSourceInfo.PASSWORD_LOCATION_CLASSPATH)) {
                    URL url = parentConfiguration.getResourceFinder().getResource(
                            passwordLocation);

                    if (url != null)
                        password = passwordFromURL(url);
                    else
                        logger.error("Could not find resource in CLASSPATH: "
                                + passwordSource);
                }
                else if (passwordLocation.equals(DataSourceInfo.PASSWORD_LOCATION_URL)) {
                    try {
                        password = passwordFromURL(new URL(passwordSource));
                    }
                    catch (MalformedURLException exception) {
                        logger.warn(exception);
                    }
                }
                else if (passwordLocation
                        .equals(DataSourceInfo.PASSWORD_LOCATION_EXECUTABLE)) {
                    if (passwordSource != null) {
                        try {
                            Process process = Runtime.getRuntime().exec(passwordSource);
                            password = passwordFromInputStream(process.getInputStream());
                            process.waitFor();
                        }
                        catch (IOException exception) {
                            logger.warn(exception);
                        }
                        catch (InterruptedException exception) {
                            logger.warn(exception);
                        }
                    }
                }
            }

            if (password != null && passwordEncoder != null)
                driverInfo.setPassword(passwordEncoder.decodePassword(
                        password,
                        encoderKey));
        }
    }

    private class ConnectionHandler extends AbstractHandler {

        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the parser at the
         *            end of the element. Must not be <code>null</code>.
         */
        public ConnectionHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes atts, DataSourceInfo driverInfo)
                throws SAXException {
            try {
                String min = atts.getValue("min");
                if (min != null)
                    driverInfo.setMinConnections(Integer.parseInt(min));

                String max = atts.getValue("max");
                if (max != null)
                    driverInfo.setMaxConnections(Integer.parseInt(max));
            }
            catch (NumberFormatException nfex) {
                logger.info("Error loading numeric attribute", nfex);
                throw new SAXException("Error reading numeric attribute.", nfex);
            }
        }
    }
}
