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
package org.apache.cayenne.reflect;

import java.util.Comparator;
import java.util.Map.Entry;

/**
 * @since 3.1
 */
class PropertyComparator implements
        Comparator<Entry<String, PropertyDescriptor>> {

    static final Comparator<Entry<String, PropertyDescriptor>> comparator = new PropertyComparator();

    public int compare(Entry<String, PropertyDescriptor> o1,
            Entry<String, PropertyDescriptor> o2) {
        if (o1.getValue() instanceof ArcProperty) {
            if (o2.getValue() instanceof ArcProperty) {
                return o1.getKey().compareTo(o2.getKey());
            } else {
                return 1;
            }
        } else {
            if (o2.getValue() instanceof ArcProperty) {
                return -1;
            } else {
                return o1.getKey().compareTo(o2.getKey());
            }
        }
    }
}
