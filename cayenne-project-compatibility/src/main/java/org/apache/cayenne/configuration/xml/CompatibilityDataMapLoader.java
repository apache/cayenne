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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.compatibility.CompatibilityUpgradeService;
import org.apache.cayenne.project.compatibility.DocumentProvider;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * @since 4.1
 */
public class CompatibilityDataMapLoader extends XMLDataMapLoader {

    @Inject
    Provider<UpgradeService> upgradeServiceProvider;

    @Inject
    DocumentProvider documentProvider;

    @Override
    public DataMap load(Resource configurationResource) throws CayenneRuntimeException {
        Document document = documentProvider.getDocument(configurationResource.getURL());
        // no document yet in provider, maybe DataMap is directly loaded
        if(document == null) {
            if(!(upgradeServiceProvider.get() instanceof CompatibilityUpgradeService)) {
                throw new ConfigurationException("CompatibilityUpgradeService expected");
            }
            // try to upgrade datamap directly
            CompatibilityUpgradeService upgradeService = (CompatibilityUpgradeService)upgradeServiceProvider.get();
            upgradeService.upgradeDataMap(configurationResource);
            document = documentProvider.getDocument(configurationResource.getURL());

            // still no document, try to load it without upgrade, though it likely will fail
            if(document == null) {
                return super.load(configurationResource);
            }
        }

        final DataMap[] maps = new DataMap[1];
        try {
            DOMSource source = new DOMSource(document);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TransformerFactory transFactory = TransformerFactory.newInstance();
            transFactory.newTransformer().transform(source, new StreamResult(baos));
            InputSource isource = new InputSource(source.getSystemId());
            isource.setByteStream(new ByteArrayInputStream(baos.toByteArray()));

            XMLReader parser = Util.createXmlReader();
            LoaderContext loaderContext = new LoaderContext(parser, handlerFactory);
            loaderContext.addDataMapListener(dataMap -> maps[0] = dataMap);
            RootDataMapHandler rootHandler = new RootDataMapHandler(loaderContext);

            parser.setContentHandler(rootHandler);
            parser.setErrorHandler(rootHandler);
            parser.parse(isource);
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
}
