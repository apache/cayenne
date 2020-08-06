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
package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;

/**
 * @since 3.1
 * @since 4.1 moved from org.apache.cayenne.configuration package
 */
public class XMLDataMapLoader implements DataMapLoader {

    private static final String DATA_MAP_LOCATION_SUFFIX = ".map.xml";

    @Inject
    protected HandlerFactory handlerFactory;

    @Inject
    protected Provider<XMLReader> xmlReaderProvider;

    public synchronized DataMap load(Resource configurationResource) throws CayenneRuntimeException {

        final DataMap[] maps = new DataMap[1];

        try(InputStream in = configurationResource.getURL().openStream()) {
            XMLReader parser = xmlReaderProvider.get();
            LoaderContext loaderContext = new LoaderContext(parser, handlerFactory);
            loaderContext.addDataMapListener(dataMap -> {
                dataMap.setConfigurationSource(configurationResource);
                maps[0] = dataMap;
            });
            RootDataMapHandler rootHandler = new RootDataMapHandler(loaderContext);

            parser.setContentHandler(rootHandler);
            parser.setErrorHandler(rootHandler);
            InputSource input = new InputSource(in);
            input.setSystemId(configurationResource.getURL().toString());
            parser.parse(input);
        } catch (Exception e) {
            throw new CayenneRuntimeException("Error loading configuration from %s", e, configurationResource.getURL());
        }

        if(maps[0] == null) {
            throw new CayenneRuntimeException("Unable to load data map from %s", configurationResource.getURL());
        }
        DataMap map = maps[0];

        if(map.getName() == null) {
            // set name based on location if no name provided by map itself
            map.setName(mapNameFromLocation(configurationResource.getURL().getFile()));
        }
        return map;
    }

    /**
     * Helper method to guess the map name from its location.
     */
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
