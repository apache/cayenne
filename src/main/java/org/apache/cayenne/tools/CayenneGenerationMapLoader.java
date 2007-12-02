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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.xml.sax.InputSource;

/**
 * Provides DataMap loading and ObjEntity filtering functionality to the class generation tasks.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class CayenneGenerationMapLoader {

    private File mainDataMapFile;
    private File[] additionalDataMapFiles;
    private NamePatternMatcher nameFilter;
    private boolean client;

    private DataMap mainDataMap;

    DataMap getMainDataMap() throws MalformedURLException {
        if (mainDataMap == null) {
            MapLoader mapLoader = new MapLoader();

            DataMap mainDataMap = loadDataMap(mapLoader, mainDataMapFile);

            if (additionalDataMapFiles != null) {

                EntityResolver entityResolver = new EntityResolver();
                entityResolver.addDataMap(mainDataMap);
                mainDataMap.setNamespace(entityResolver);

                for (int i = 0; i < additionalDataMapFiles.length; i++) {

                    DataMap dataMap = loadDataMap(mapLoader, additionalDataMapFiles[i]);
                    entityResolver.addDataMap(dataMap);
                    dataMap.setNamespace(entityResolver);
                }
            }

            this.mainDataMap = mainDataMap;
        }

        return mainDataMap;
    }

    Collection<ObjEntity> getFilteredEntities() throws MalformedURLException {

        List<ObjEntity> entities = new ArrayList<ObjEntity>(getMainDataMap()
                .getObjEntities());

        // filter out excluded entities...
        Iterator<ObjEntity> it = entities.iterator();

        while (it.hasNext()) {
            ObjEntity e = it.next();
            if (e.isGeneric()) {
                it.remove();
            }
            else if (client && !e.isClientAllowed()) {
                it.remove();
            }
            else if (!nameFilter.isIncluded(e.getName())) {
                it.remove();
            }
        }

        return entities;
    }

    protected DataMap loadDataMap(MapLoader mapLoader, File dataMapFile)
            throws MalformedURLException {
        InputSource in = new InputSource(dataMapFile.toURL().toString());
        return mapLoader.loadDataMap(in);
    }

    void setMainDataMapFile(File mainDataMapFile) {
        this.mainDataMapFile = mainDataMapFile;
    }

    void setAdditionalDataMapFiles(File[] additionalDataMapFiles) {
        this.additionalDataMapFiles = additionalDataMapFiles;
    }

    void setClient(boolean client) {
        this.client = client;
    }

    public void setNameFilter(NamePatternMatcher nameFilter) {
        this.nameFilter = nameFilter;
    }
}
