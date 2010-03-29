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

package org.apache.cayenne.unit;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.springframework.beans.factory.FactoryBean;

/**
 * A Spring-compatible factory to load DataMaps.
 * 
 */
public class DataMapFactory implements FactoryBean {
    protected DataMap map;
    protected String location;

    public DataMapFactory(String location) {
        this.location = location;
    }

    public Object getObject() throws Exception {
        if (map == null) {
            map = new MapLoader().loadDataMap(location);
        }

        return map;
    }

    public Class getObjectType() {
        return DataMap.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
