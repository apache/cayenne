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

package org.apache.cayenne.modeler.util;

import java.util.Map;

import org.apache.cayenne.util.Util;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 * Scope active model that has an internal map of properties, 
 * firing change events when a value in the map changes.
 * 
 */
public abstract class MapModel extends BasicModel {
    
    protected Map<String, String> map;

    public MapModel() {
        super();
    }

    public abstract Selector selectorForKey(String key);

    public abstract String defaultForKey(String key);

    public abstract String[] supportedProperties();

    /**
     * Saves properties in provided map.
     */
    public void storeProperties(Map<String, String> map) {
        String[] properties = supportedProperties();
        for (String property : properties) {
            map.put(property, this.map.get(property));
        }
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public void setProperty(String key, String value) {
        if (map != null) {

            if (Util.nullSafeEquals(defaultForKey(key), value)) {
                value = null;
            }

            Object oldValue = map.put(key, value);
            if (!Util.nullSafeEquals(oldValue, value)) {
                fireModelChange(VALUE_CHANGED, selectorForKey(key));
            }
        }
    }

    public String getProperty(String key) {
        String value = (map != null) ? map.get(key) : null;
        return (value != null) ? value : defaultForKey(key);
    }

}
