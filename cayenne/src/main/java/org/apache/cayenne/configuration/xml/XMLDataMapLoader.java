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
import org.apache.cayenne.configuration.upgrade.UpgradeContext;
import org.apache.cayenne.configuration.upgrade.ConfigurationUpgrader;
import org.apache.cayenne.configuration.upgrade.UpgradeHandler;
import org.apache.cayenne.configuration.upgrade.UpgradeType;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.net.URL;

/**
 * @since 3.1
 * @since 4.1 moved from org.apache.cayenne.configuration package
 */
public class XMLDataMapLoader implements DataMapLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLDataMapLoader.class);

    private static final String DATA_MAP_LOCATION_SUFFIX = ".map.xml";

    @Inject
    protected HandlerFactory handlerFactory;

    @Inject
    protected Provider<XMLReader> xmlReaderProvider;

    @Inject
    protected ConfigurationUpgrader upgrader;

    public synchronized DataMap load(Resource configurationResource) throws CayenneRuntimeException {

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
        DataMap[] maps = new DataMap[1];

        XMLReader parser = xmlReaderProvider.get();
        LoaderContext loaderContext = new LoaderContext(parser, handlerFactory);
        loaderContext.addDataMapListener(dataMap -> {
            dataMap.setConfigurationSource(configurationResource);
            maps[0] = dataMap;
        });

        RootDataMapHandler rootHandler = new RootDataMapHandler(loaderContext);
        parser.setContentHandler(rootHandler);
        parser.setErrorHandler(rootHandler);
        parser.parse(input);

        return maps[0];
    }

    /**
     * Loads a DataMap created by an older version of Cayenne, upgrading its XML in memory. The DataMap file is not
     * modified.
     *
     * @since 5.0
     */
    protected DataMap upgradeAndLoad(Resource configurationResource, String version) {

        URL configurationURL = configurationResource.getURL();
        UpgradeType upgradeType = upgrader.checkUpgradeNeeded(version);

        if (upgradeType == UpgradeType.DOWNGRADE_NEEDED) {
            throw new CayenneRuntimeException("""
                    Unable to load DataMap from %s: project version %s is newer than the supported version %s. \
                    The DataMap was created with a newer version of Cayenne""",
                    configurationURL, version, UpgradeHandler.CURRENT_VERSION);
        }

        if (upgradeType == UpgradeType.INTERMEDIATE_UPGRADE_NEEDED) {
            throw new CayenneRuntimeException("""
                    Unable to load DataMap from %s: project version %s is too old to be upgraded. \
                    Open the project in an older CayenneModeler to upgrade it to version %s first""",
                    configurationURL, version, UpgradeHandler.MIN_SUPPORTED_VERSION);
        }

        UpgradeContext context = upgrader.upgradeDataMapDom(configurationResource, version);
        if (!context.getDestructiveChanges().isEmpty()) {
            throw new CayenneRuntimeException("""
                    Unable to upgrade DataMap from %s (project version %s) in memory, as the upgrade requires \
                    manual changes. Open the project in CayenneModeler to upgrade it. %s""",
                    configurationURL, version, String.join(" ", context.getDestructiveChanges()));
        }

        LOGGER.warn("""
                DataMap {} has project version {} and was upgraded to version {} in memory. \
                Open the project in CayenneModeler to upgrade its XML permanently""",
                configurationURL, version, UpgradeHandler.CURRENT_VERSION);

        try {
            return parse(configurationResource,
                    DocumentInputSource.of(context.getDocument(), configurationURL.toString()));
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
