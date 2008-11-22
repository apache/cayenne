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

package org.apache.cayenne.modeler.dialog.datadomain;

import org.apache.cayenne.access.DataRowStore;
import org.apache.cayenne.modeler.util.MapModel;
import org.scopemvc.core.Selector;

/**
 */
public class CacheSyncConfigModel extends MapModel {

    private static final String[] storedProperties =
        new String[] { DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY };

    // selectors
    public static final Selector FACTORY_CLASS_SELECTOR =
        Selector.fromString("factoryClass");

    public String[] supportedProperties() {
        return storedProperties;
    }

    public Selector selectorForKey(String key) {
        return (DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY.equals(key))
            ? FACTORY_CLASS_SELECTOR
            : null;
    }

    public String defaultForKey(String key) {
        return null;
    }

    public String getFactoryClass() {
        return getProperty(DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY);
    }

    public void setFactoryClass(String factoryClass) {
        setProperty(DataRowStore.EVENT_BRIDGE_FACTORY_PROPERTY, factoryClass);
    }
}
