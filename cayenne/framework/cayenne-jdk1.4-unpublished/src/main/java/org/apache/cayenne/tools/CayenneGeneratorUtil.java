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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.gen.DefaultClassGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.Util;
import org.xml.sax.InputSource;

/**
 * Utility class to perform class generation from data map. This class is used by
 * ant and Maven plugins.
 * 
 * @author Andrus Adamchik, Kevin Menard
 * @since 3.0
 */
class CayenneGeneratorUtil {

    protected ILog logger;
    
    protected MapLoader mapLoader;
    protected File map;
    protected File additionalMaps[];
    protected DefaultClassGenerator generator;
    
    protected String includeEntitiesPattern;
    protected String excludeEntitiesPattern;
    
    /** Loads and returns a DataMap by File. */
    public DataMap loadDataMap(File mapName) throws Exception {
        InputSource in = new InputSource(mapName.toURL().toString());
        if(mapLoader == null) {
            mapLoader = new MapLoader();
        }
        return mapLoader.loadDataMap(in);
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    public DataMap loadDataMap() throws Exception {
        return loadDataMap(map);
    }
    
    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap[] loadAdditionalDataMaps() throws Exception {
        if (null == additionalMaps) {
            return new DataMap[0];
        }

        DataMap dataMaps[] = new DataMap[additionalMaps.length];
        for (int i = 0; i < additionalMaps.length; i++) {
            dataMaps[i] = loadDataMap(additionalMaps[i]);
        }
        return dataMaps;
    }
    
    public void processMap() throws Exception {

        DataMap dataMap = loadDataMap();
        DataMap additionalDataMaps[] = loadAdditionalDataMaps();

        // Create MappingNamespace for maps.
        EntityResolver entityResolver = new EntityResolver(Collections.singleton(dataMap));
        dataMap.setNamespace(entityResolver);
        for (int i = 0; i < additionalDataMaps.length; i++) {
            entityResolver.addDataMap(additionalDataMaps[i]);
            additionalDataMaps[i].setNamespace(entityResolver);
        }

        Collection allEntities = dataMap.getObjEntities();
        List filteredEntities = new ArrayList(allEntities.size());

        // filter client entities
        if (generator.isClient()) {
            if (dataMap.isClientSupported()) {
                Iterator it = allEntities.iterator();
                while (it.hasNext()) {
                    ObjEntity entity = (ObjEntity) it.next();
                    if (entity.isClientAllowed()) {
                        filteredEntities.add(entity);
                    }
                }
            }
        }
        else {
            filteredEntities.addAll(allEntities);
        }

        // filter names according to the specified pattern
        NamePatternMatcher namePatternMatcher = new NamePatternMatcher(
                logger,
                includeEntitiesPattern,
                excludeEntitiesPattern);
        namePatternMatcher.filter(filteredEntities);

        generator.setTimestamp(map.lastModified());
        generator.setDataMap(dataMap);
        generator.setObjEntities(filteredEntities);
        generator.validateAttributes();
        generator.execute();
    }
    
    public void execute() throws Exception
    {
        try {
            processMap();
        }
        catch (Throwable th) {
            th = Util.unwindException(th);

            String thMessage = th.getLocalizedMessage();
            String message = "Error generating classes: ";
            message += (!Util.isEmptyString(thMessage)) ? thMessage : th
                    .getClass()
                    .getName();

            logger.log(message);
            throw new Exception(message, th);
        }
    }

    public void setAdditionalMaps(File[] additionalMaps) {
        this.additionalMaps = additionalMaps;
    }
    
    public void setExcludeEntitiesPattern(String excludeEntitiesPattern) {
        this.excludeEntitiesPattern = excludeEntitiesPattern;
    }
    
    public void setGenerator(DefaultClassGenerator generator) {
        this.generator = generator;
    }
    
    public void setIncludeEntitiesPattern(String includeEntitiesPattern) {
        this.includeEntitiesPattern = includeEntitiesPattern;
    }
    
    public void setLogger(ILog logger) {
        this.logger = logger;
    }
    
    public void setMap(File map) {
        this.map = map;
    }
}
