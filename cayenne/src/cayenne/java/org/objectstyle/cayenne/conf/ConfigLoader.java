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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.util.AbstractHandler;
import org.objectstyle.cayenne.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class that performs runtime loading of Cayenne configuration.
 * 
 * @author Andrei Adamchik
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
         * Handles the start of a datadomains element. A domains handler is created
         * and initialised with the element name and attributes.
         *
         * @exception SAXException if the tag given is not
         *                              <code>"domains"</code>
         */
        public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes attrs)
            throws SAXException {
            if (localName.equals("domains")) {
                delegate.shouldLoadProjectVersion(attrs.getValue("", "project-version"));
                new DomainsHandler(parser, this);
            }
            else {
                throw new SAXParseException(
                    "<domains> should be the root element. <"
                        + localName
                        + "> is unexpected.",
                    null);
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
         * @param parentHandler The handler which should be restored to the
         *                      parser at the end of the element.
         *                      Must not be <code>null</code>.
         */
        public DomainsHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        /**
         * Handles the start of a top-level element within the project. An
         * appropriate handler is created and initialised with the details
         * of the element.
         */
        public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
            if (localName.equals("domain")) {
                new DomainHandler(getParser(), this).init(localName, atts);
            }
            else if (localName.equals("view")) {
                new ViewHandler(getParser(), this).init(atts);
            }
            else {
                String message =
                    "<domain> or <view> are only valid children of <domains>. <"
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

        public void init(Attributes attrs) throws SAXException {

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
        private Map properties;
        private Map mapLocations;

        public DomainHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs) throws SAXException {
            domainName = attrs.getValue("", "name");
            mapLocations = new HashMap();
            properties = new HashMap();
            delegate.shouldLoadDataDomain(domainName);
        }

        public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {

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
                String message =
                    "<node> or <map> should be the children of <domain>. <"
                        + localName
                        + "> is unexpected.";
                throw new SAXParseException(message, null);
            }
        }

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

        public void init(Attributes attrs, Map properties) throws SAXException {

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

        public void init(String name, Attributes attrs, String domainName, Map locations)
            throws SAXException {
            this.domainName = domainName;
            this.mapLocations = locations;
            mapName = attrs.getValue("", "name");
            location = attrs.getValue("", "location");
        }

        public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes attrs)
            throws SAXException {
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

        public void init(String name, Attributes attrs, String domainName)
            throws SAXException {
            this.domainName = domainName;

            nodeName = attrs.getValue("", "name");
            String dataSrcLocation = attrs.getValue("", "datasource");
            String adapterClass = attrs.getValue("", "adapter");
            String factoryName = attrs.getValue("", "factory");
            delegate.shouldLoadDataNode(
                domainName,
                nodeName,
                dataSrcLocation,
                adapterClass,
                factoryName);
        }

        public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes attrs)
            throws SAXException {

            if (localName.equals("map-ref")) {
                new MapRefHandler(getParser(), this).init(
                    localName,
                    attrs,
                    domainName,
                    nodeName);
            }
            else {
                throw new SAXParseException(
                    "<map-ref> should be the only node child. <"
                        + localName
                        + "> is unexpected.",
                    null);
            }
        }
    }

    // this handler is deprecated, but is kept around for backwards compatibility
    private class DepMapRefHandler extends AbstractHandler {

        public DepMapRefHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(String name, Attributes attrs)
            throws SAXException {
        }
    }

    private class MapRefHandler extends AbstractHandler {
        public MapRefHandler(XMLReader parser, ContentHandler parentHandler) {
            super(parser, parentHandler);
        }

        public void init(
            String name,
            Attributes attrs,
            String domainName,
            String nodeName)
            throws SAXException {
            String mapName = attrs.getValue("", "name");
            delegate.shouldLinkDataMap(domainName, nodeName, mapName);
        }
    }
}
