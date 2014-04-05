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
package org.apache.cayenne.project.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A common superclass of UpgradeHandlers.
 * 
 * @since 3.1
 */
// there's no guarantee this will survive the further version upgrades, but for now all
// the code here seems like version-agnostic
public abstract class BaseUpgradeHandler implements UpgradeHandler {

    static final String UNKNOWN_VERSION = "0";

    protected Resource projectSource;
    protected UpgradeMetaData metaData;

    public BaseUpgradeHandler(Resource projectSource) {

        if (projectSource == null) {
            throw new NullPointerException("Null project source");
        }

        this.projectSource = projectSource;
    }
    
    /**
     * Creates a single common EntityResolver for all project DataMaps, setting
     * it as a namespace for all of them. This is needed for resolving cross-map
     * relationships.
     */
    protected void attachToNamespace(DataChannelDescriptor channelDescriptor) {
        EntityResolver entityResolver = new EntityResolver(channelDescriptor.getDataMaps());

        for (DataMap map : entityResolver.getDataMaps()) {
            map.setNamespace(entityResolver);
        }
    }

    public Resource getProjectSource() {
        return projectSource;
    }

    public UpgradeMetaData getUpgradeMetaData() {
        // no attempts at thread-safety... shouldn't be needed for upgrades
        if (metaData == null) {
            metaData = loadMetaData();
        }

        return metaData;
    }

    public Resource performUpgrade() throws ConfigurationException {
        UpgradeMetaData metaData = getUpgradeMetaData();
        switch (metaData.getUpgradeType()) {
            case DOWNGRADE_NEEDED:
                throw new ConfigurationException("Downgrade can not be performed");
            case INTERMEDIATE_UPGRADE_NEEDED:
                throw new ConfigurationException(
                        "Upgrade can not be performed - intermediate version upgrade needed");
            case UPGRADE_NEEDED:
                return doPerformUpgrade(metaData);
            default:
                return getProjectSource();
        }
    }

    /**
     * Does the actual project upgrade, assuming the caller already verified that the
     * upgrade is possible.
     * @param metaData object describing the type of upgrade
     */
    protected abstract Resource doPerformUpgrade(UpgradeMetaData metaData) throws ConfigurationException;

    /**
     * Creates a metadata object describing the type of upgrade needed.
     */
    protected abstract UpgradeMetaData loadMetaData();

    /**
     * A default method for quick extraction of the project version from an XML file.
     */
    protected String loadProjectVersion() {

        RootTagHandler rootHandler = new RootTagHandler();
        URL url = projectSource.getURL();

        InputStream in = null;

        try {
            in = url.openStream();
            XMLReader parser = Util.createXmlReader();

            parser.setContentHandler(rootHandler);
            parser.setErrorHandler(rootHandler);
            parser.parse(new InputSource(in));
        }
        catch (SAXException e) {
            // expected ... handler will terminate as soon as it finds a root tag.
        }
        catch (Exception e) {
            throw new ConfigurationException(
                    "Error reading configuration from %s",
                    e,
                    url);
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            }
            catch (IOException ioex) {
                // ignoring...
            }
        }

        return rootHandler.projectVersion != null
                ? rootHandler.projectVersion
                : UNKNOWN_VERSION;
    }

    /**
     * Compares two String versions.
     */
    protected int compareVersions(String v1, String v2) {

        if (v1.equals(v2)) {
            return 0;
        }

        double v1Double = decodeVersion(v1);
        double v2Double = decodeVersion(v2);
        return v1Double < v2Double ? -1 : 1;
    }

    protected double decodeVersion(String version) {
        if (version == null || version.trim().length() == 0) {
            return 0;
        }

        // leave the first dot, and treat remaining as a fraction
        // remove all non digit chars
        StringBuilder buffer = new StringBuilder(version.length());
        boolean dotProcessed = false;
        for (int i = 0; i < version.length(); i++) {
            char nextChar = version.charAt(i);
            if (nextChar == '.' && !dotProcessed) {
                dotProcessed = true;
                buffer.append('.');
            }
            else if (Character.isDigit(nextChar)) {
                buffer.append(nextChar);
            }
        }

        return Double.parseDouble(buffer.toString());
    }

    class RootTagHandler extends DefaultHandler {

        private String projectVersion;

        @Override
        public void startElement(
                String uri,
                String localName,
                String qName,
                Attributes attributes) throws SAXException {

            this.projectVersion = attributes.getValue("", "project-version");

            // bail right away - we are not interested in reading this to the end
            throw new SAXException("finished");
        }
    }
}
