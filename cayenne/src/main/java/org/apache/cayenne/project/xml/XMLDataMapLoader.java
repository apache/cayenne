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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.project.DataMapLoader;
import org.apache.cayenne.project.upgrade.ProjectFileUpgrader;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.net.URL;

/**
 * @since 3.1
 * @since 4.1 moved from org.apache.cayenne.project package
 */
public class XMLDataMapLoader implements DataMapLoader {

    private static final String DATA_MAP_LOCATION_SUFFIX = ".map.xml";

    @Inject
    protected HandlerFactory handlerFactory;

    @Inject
    protected Provider<XMLReader> xmlReaderProvider;

    @Inject
    protected ProjectFileUpgrader upgrader;

    public DataMap load(Resource configurationResource) throws CayenneRuntimeException {

        URL configurationURL = configurationResource.getURL();
        DataMap map;

        try (InputStream in = configurationURL.openStream()) {
            InputSource input = new InputSource(in);
            input.setSystemId(configurationURL.toString());
            map = parse(configurationResource, input);
        } catch (UnsupportedVersionException e) {
            map = upgradeAndLoad(configurationResource, e.getVersion());
        } catch (Exception e) {
            throw new CayenneRuntimeException("Error loading configuration from %s", e, configurationURL);
        }

        if (map == null) {
            throw new CayenneRuntimeException("Unable to load data map from %s", configurationURL);
        }

        if (map.getName() == null) {
            // set name based on location if no name provided by map itself
            map.setName(mapNameFromLocation(configurationURL.getFile()));
        }
        return map;
    }

    protected DataMap parse(Resource configurationResource, InputSource input) throws Exception {
        XMLReader parser = xmlReaderProvider.get();
        LoaderContext loaderContext = new LoaderContext(parser, handlerFactory);

        RootDataMapHandler rootHandler = new RootDataMapHandler(loaderContext, configurationResource);
        parser.setContentHandler(rootHandler);
        parser.setErrorHandler(rootHandler);
        parser.parse(input);

        return rootHandler.getDataMap();
    }

    /**
     * Loads a DataMap created by an older version of Cayenne, upgrading its XML in memory. The DataMap file is not
     * modified.
     *
     * @since 5.0
     */
    protected DataMap upgradeAndLoad(Resource configurationResource, String version) {
        Document upgraded = upgrader.upgradeDataMapDOM(configurationResource, version).getRuntimeDocument();
        URL configurationURL = configurationResource.getURL();
        try {
            return parse(configurationResource, DocumentInputSource.of(upgraded, configurationURL.toString()));
        } catch (Exception e) {
            throw new CayenneRuntimeException("Error loading configuration from %s", e, configurationURL);
        }
    }

    protected String mapNameFromLocation(String location) {
        if (location == null) {
            return "Untitled";
        }

        int lastSlash = location.lastIndexOf('/');
        if (lastSlash < 0) {
            lastSlash = location.lastIndexOf('\\');
        }

        if (lastSlash >= 0 && lastSlash + 1 < location.length()) {
            location = location.substring(lastSlash + 1);
        }

        if (location.endsWith(DATA_MAP_LOCATION_SUFFIX)) {
            location = location.substring(0, location.length() - DATA_MAP_LOCATION_SUFFIX.length());
        }

        return location;
    }

    public void setHandlerFactory(HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }
}
