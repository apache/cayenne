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
package org.apache.cayenne.tools;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.xml.sax.InputSource;

/**
 * Loads a DataMap and a shared entity namespace.
 * 
 * @since 3.0
 */
class CayenneGeneratorMapLoaderAction {

    private File mainDataMapFile;
    private File[] additionalDataMapFiles;
    private DataMap mainDataMap;

    DataMap getMainDataMap() throws MalformedURLException {
        if (mainDataMap == null) {
            MapLoader mapLoader = new MapLoader();

            DataMap mainDataMap = loadDataMap(mapLoader, mainDataMapFile);

            if (additionalDataMapFiles != null) {

                EntityResolver entityResolver = new EntityResolver();
                entityResolver.addDataMap(mainDataMap);
                mainDataMap.setNamespace(entityResolver);

                for (File additionalDataMapFile : additionalDataMapFiles) {

                    DataMap dataMap = loadDataMap(mapLoader, additionalDataMapFile);
                    entityResolver.addDataMap(dataMap);
                    dataMap.setNamespace(entityResolver);
                }
            }

            this.mainDataMap = mainDataMap;
        }

        return mainDataMap;
    }

    protected DataMap loadDataMap(MapLoader mapLoader, File dataMapFile)
            throws MalformedURLException {
        InputSource in = new InputSource(dataMapFile.toURI().toURL().toString());
        return mapLoader.loadDataMap(in);
    }

    void setMainDataMapFile(File mainDataMapFile) {
        this.mainDataMapFile = mainDataMapFile;
    }

    void setAdditionalDataMapFiles(File[] additionalDataMapFiles) {
        this.additionalDataMapFiles = additionalDataMapFiles;
    }
}
