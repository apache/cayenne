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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.util.Util;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.map.DataMap;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class that performs runtime loading of Cayenne configuration.
 */
public class ConfigLoader {

    protected XMLReader parser;
    protected ConfigLoaderDelegate delegate;

    /** Creates new ConfigLoader. */
    public ConfigLoader(ConfigLoaderDelegate delegate) throws Exception {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate must not be null.");
        }

        this.delegate = delegate;
        parser = Util.createXmlReader();
    }

    /**
     * Returns the delegate.
     * 
     * @return ConfigLoaderDelegate
     */
    public ConfigLoaderDelegate getDelegate() {
        return delegate;
    }

    /**
     * Parses XML input, invoking delegate methods to interpret loaded XML.
     * 
     * @param in
     * @return boolean
     */
    public boolean loadDomains(InputStream in) {
        DefaultHandler handler = new RootHandler();
        parser.setContentHandler(handler);
        parser.setErrorHandler(handler);

        try {
            delegate.startedLoading();
            parser.parse(new InputSource(in));
            delegate.finishedLoading();
        }
        catch (IOException ioex) {
            getDelegate().loadError(ioex);
        }
        catch (SAXException saxex) {
            getDelegate().loadError(saxex);
        }

        // return true if no failures
        return !getDelegate().getStatus().hasFailures();
    }

    // SAX handlers start below

    /**
     * Handler for the root element. Its only child must be the "domains" element.
     */
    private class RootHandler extends DefaultHandler {

        /**
         * Handles the start of a datadomains element. A domains handler is created and
         * initialised with the element name and attributes.
         * 
         * @exception SAXException if the tag given is not <code>"domains"</code>
         */
        @Override
        public void startElement(
                String namespaceURI,
                String localName,
                String qName,
                Attributes attrs) throws SAXException {
            if (localName.equals("domains")) {
                delegate.shouldLoadProjectVersion(attrs.getValue("", "project-version"));
                new DomainsHandler(parser, this);
            }
            else {
                throw new SAXParseException("<domains> should be the root element. <"
                        + localName
                        + "> is unexpected.", null);
            }
        }
    }

    /**
     * Handler for the top level "project" element.
     */
    private class DomainsHandler extends AbstractHandler {

        /**
         * Constructor which just delegates to the superconstructor.
         * 
         * @param parentHandler The handler which should be restored to the parser at the
         *            end of the element. Must not be <code>null</code>.
         */
        public DomainsHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        /**
         * Handles the start of a top-level element within the project. An appropriate
         * handler is created and initialised with the details of the element.
         */
        @Override
        public void startElement(
                String namespaceURI,
                String localName,
                String qName,
                Attributes atts) throws SAXException {
            if (localName.equals("domain")) {
                new DomainHandler(getParser(), this).init(localName, atts);
            }
            else if (localName.equals("view")) {
                new ViewHandler(getParser(), this).init(atts);
            }
            else {
                String message = "<domain> or <view> are only valid children of <domains>. <"
                        + localName
                        + "> is unexpected.";
                throw new SAXParseException(message, null);
            }
        }
    }

    private class ViewHandler extends AbstractHandler {

        public ViewHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(Attributes attrs) {
            String name = attrs.getValue("", "name");
            String location = attrs.getValue("", "location");
            delegate.shouldRegisterDataView(name, location);
        }
    }

    /**
     * Handler for the "domain" element.
     */
    private class DomainHandler extends AbstractHandler {

        private String domainName;
        private Map<String, String> properties;
        private Map<String, DataMap> mapLocations;

        public DomainHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs) {
            domainName = attrs.getValue("", "name");
            mapLocations = new HashMap<String, DataMap>();
            properties = new HashMap<String, String>();
            delegate.shouldLoadDataDomain(domainName);
        }

        @Override
        public void startElement(
                String namespaceURI,
                String localName,
                String qName,
                Attributes atts) throws SAXException {

            if (localName.equals("property")) {
                new PropertyHandler(getParser(), this).init(atts, properties);
            }
            else if (localName.equals("map")) {
                // "map" elements go after "property" elements
                // must flush properties if there are any
                loadProperties();

                new MapHandler(getParser(), this).init(
                        localName,
                        atts,
                        domainName,
                        mapLocations);
            }
            else if (localName.equals("node")) {
                // "node" elements go after "map" elements
                // must flush maps if there are any
                loadMaps();

                new NodeHandler(getParser(), this).init(localName, atts, domainName);
            }
            else {
                String message = "<node> or <map> should be the children of <domain>. <"
                        + localName
                        + "> is unexpected.";
                throw new SAXParseException(message, null);
            }
        }

        @Override
        protected void finished() {
            loadProperties();
            loadMaps();
        }

        private void loadProperties() {
            if (properties.size() > 0) {
                // load all properties
                delegate.shouldLoadDataDomainProperties(domainName, properties);

                // clean properties to avoid loading them twice
                properties.clear();
            }
        }

        private void loadMaps() {
            if (mapLocations.size() > 0) {
                // load all maps
                delegate.shouldLoadDataMaps(domainName, mapLocations);
                // clean map locations to avoid loading maps twice
                mapLocations.clear();
            }
        }
    }

    private class PropertyHandler extends AbstractHandler {

        public PropertyHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(Attributes attrs, Map<String, String> properties) {

            String name = attrs.getValue("", "name");
            String value = attrs.getValue("", "value");
            if (name != null && value != null) {
                properties.put(name, value);
            }
        }
    }

    private class MapHandler extends AbstractHandler {

        protected String domainName;
        protected String mapName;
        protected String location;
        private Map mapLocations;

        public MapHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(
                String name,
                Attributes attrs,
                String domainName,
                Map<String, DataMap> locations) {
            this.domainName = domainName;
            this.mapLocations = locations;
            mapName = attrs.getValue("", "name");
            location = attrs.getValue("", "location");
        }

        @Override
        public void startElement(
                String namespaceURI,
                String localName,
                String qName,
                Attributes attrs) throws SAXException {
            if (localName.equals("dep-map-ref")) {

                // this is no longer supported, but kept as noop
                // for backwards compatibility
                new DepMapRefHandler(getParser(), this).init(localName, attrs);
            }
            else {
                throw new SAXParseException(
                        "<dep-map-ref> should be the only map child. <"
                                + localName
                                + "> is unexpected.",
                        null);
            }
        }

        @Override
        protected void finished() {
            mapLocations.put(mapName, location);
        }
    }

    /** Handles processing of "node" element. */
    private class NodeHandler extends AbstractHandler {

        protected String nodeName;
        protected String domainName;

        public NodeHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs, String domainName) {
            this.domainName = domainName;

            nodeName = attrs.getValue("", "name");
            String dataSrcLocation = attrs.getValue("", "datasource");
            String adapterClass = attrs.getValue("", "adapter");
            String factoryName = attrs.getValue("", "factory");
            String schemaUpdateStrategyName = attrs
                    .getValue("", "schema-update-strategy");
            if (schemaUpdateStrategyName == null) {
                schemaUpdateStrategyName = SkipSchemaUpdateStrategy.class.getName();
            }
            delegate.shouldLoadDataNode(
                    domainName,
                    nodeName,
                    dataSrcLocation,
                    adapterClass,
                    factoryName,
                    schemaUpdateStrategyName);
        }

        @Override
        public void startElement(
                String namespaceURI,
                String localName,
                String qName,
                Attributes attrs) throws SAXException {

            if (localName.equals("map-ref")) {
                new MapRefHandler(getParser(), this).init(
                        localName,
                        attrs,
                        domainName,
                        nodeName);
            }
            else {
                throw new SAXParseException("<map-ref> should be the only node child. <"
                        + localName
                        + "> is unexpected.", null);
            }
        }
    }

    // this handler is deprecated, but is kept around for backwards compatibility
    private class DepMapRefHandler extends AbstractHandler {

        public DepMapRefHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs) {
        }
    }

    private class MapRefHandler extends AbstractHandler {

        public MapRefHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs, String domainName, String nodeName) {
            String mapName = attrs.getValue("", "name");
            delegate.shouldLinkDataMap(domainName, nodeName, mapName);
        }
    }
}
