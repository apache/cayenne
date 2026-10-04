/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.configuration.upgrade;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.resource.Resource;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Upgrades project or DataMap XML resources created by older versions of Cayenne to the current project version.
 *
 * @since 5.0
 */
// TODO: unlike ProjectUpgrader, this one can only upgrade individual files, not the entire project file hierarchy.
//   If the hierarchy changes (e.g. we merge map.xml in the main file), this upgrader will need to be redesigned
public class ConfigurationUpgrader {

    private static final Pattern VERSIONED_NAMESPACE =
            Pattern.compile("http://cayenne\\.apache\\.org/schema/(\\d+)/\\w+");

    private final Provider<XMLReader> xmlReaderProvider;
    private final List<UpgradeHandler> handlers;

    public ConfigurationUpgrader(@Inject Provider<XMLReader> xmlReaderProvider) {
        this.xmlReaderProvider = xmlReaderProvider;
        this.handlers = UpgradeHandler.all();
    }

    /**
     * Determines what kind of upgrade, if any, a project of the given version needs.
     */
    public UpgradeType checkUpgradeNeeded(String version) {
        if (compareVersions(version, UpgradeHandler.MIN_SUPPORTED_VERSION) < 0) {
            return UpgradeType.INTERMEDIATE_UPGRADE_NEEDED;
        }

        int c = compareVersions(UpgradeHandler.CURRENT_VERSION, version);
        if (c < 0) {
            return UpgradeType.DOWNGRADE_NEEDED;
        }
        return c == 0 ? UpgradeType.UPGRADE_NOT_NEEDED : UpgradeType.UPGRADE_NEEDED;
    }

    /**
     * Returns the version of a project or DataMap XML based on its root tag, or null if it can't be determined. The
     * version is a part of the root namespace, e.g. "http://cayenne.apache.org/schema/14/dataMap". The documents
     * older than version 14 may have no namespace, or a namespace with no project version in it, in which case the
     * version is taken from their "project-version" attribute.
     *
     * @since 5.0
     */
    public static String projectVersion(String rootNamespace, Attributes rootAttributes) {
        if (rootNamespace != null) {
            Matcher matcher = VERSIONED_NAMESPACE.matcher(rootNamespace);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return rootAttributes.getValue("", "project-version");
    }

    /**
     * Reads the version from the root tag of a project or DataMap XML, without reading the rest of the document.
     */
    public String readVersion(Resource resource) {
        RootTagHandler rootHandler = new RootTagHandler();
        URL url = resource.getURL();
        try (InputStream in = url.openStream()) {
            XMLReader parser = xmlReaderProvider.get();
            parser.setContentHandler(rootHandler);
            parser.setErrorHandler(rootHandler);
            parser.parse(new InputSource(in));
        } catch (SAXException e) {
            // expected... handler will terminate as soon as it finds a root tag.
        } catch (Exception e) {
            throw new ConfigurationException("Error reading configuration from %s", e, url);
        }

        return rootHandler.projectVersion != null ? rootHandler.projectVersion : UpgradeHandler.UNKNOWN_VERSION;
    }

    /**
     * Returns the handlers that upgrade a project from the given version to {@link UpgradeHandler#CURRENT_VERSION},
     * in the order they must be applied.
     */
    List<UpgradeHandler> handlersForVersion(String version) {
        if (UpgradeHandler.MIN_SUPPORTED_VERSION.equals(version)) {
            return handlers;
        }

        // the handler producing "version" has already been applied, so the upgrade starts with the next one
        for (int i = 0; i < handlers.size(); i++) {
            if (handlers.get(i).getVersion().equals(version)) {
                return handlers.subList(i + 1, handlers.size());
            }
        }

        return List.of();
    }

    /**
     * Reads the project XML and upgrades its DOM from the given version. The referenced DataMaps are not touched,
     * see {@link #upgradeDataMapDom(Resource, String)}.
     */
    public UpgradeContext upgradeProjectDom(Resource resource, String fromVersion) {
        UpgradeContext context = new UpgradeContext(resource, readDocument(resource.getURL()));
        for (UpgradeHandler handler : handlersForVersion(fromVersion)) {
            handler.processProjectDom(context);
        }
        return context;
    }

    /**
     * Reads the DataMap XML and upgrades its DOM from the given version.
     */
    public UpgradeContext upgradeDataMapDom(Resource resource, String fromVersion) {
        UpgradeContext context = new UpgradeContext(resource, readDocument(resource.getURL()));
        for (UpgradeHandler handler : handlersForVersion(fromVersion)) {
            handler.processDataMapDom(context);
        }
        return context;
    }

    /**
     * Applies the model-level part of the upgrade from the given version to a descriptor loaded from the upgraded XML.
     */
    public void upgradeModel(String fromVersion, DataChannelDescriptor descriptor) {
        for (UpgradeHandler handler : handlersForVersion(fromVersion)) {
            handler.processModel(descriptor);
        }
    }

    private static Document readDocument(URL url) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(false);
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setExpandEntityReferences(false);
        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException ex) {
            throw new ConfigurationException("Unable to configure DocumentBuilderFactory", ex);
        }

        try {
            DocumentBuilder domBuilder = documentBuilderFactory.newDocumentBuilder();
            try (InputStream inputStream = url.openStream()) {
                return domBuilder.parse(inputStream);
            } catch (IOException | SAXException e) {
                throw new ConfigurationException("Error loading configuration from %s", e, url);
            }
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException(e);
        }
    }

    static int compareVersions(String v1, String v2) {
        if (v1.equals(v2)) {
            return 0;
        }
        return decodeVersion(v1) < decodeVersion(v2) ? -1 : 1;
    }

    static double decodeVersion(String version) {
        if (version == null || version.isBlank()) {
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
            } else if (Character.isDigit(nextChar)) {
                buffer.append(nextChar);
            }
        }
        return Double.parseDouble(buffer.toString());
    }

    static class RootTagHandler extends DefaultHandler {

        private String projectVersion;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            this.projectVersion = projectVersion(uri, attributes);

            // bail right away - we are not interested in reading this to the end
            throw new SAXException("finished");
        }
    }
}
