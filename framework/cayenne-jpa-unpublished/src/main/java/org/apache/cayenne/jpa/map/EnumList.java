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


package org.apache.cayenne.jpa.map;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list that converts added objects to the specified enum type.
 * 
 * @author Andrus Adamchik
 */
// TODO: andrus, 4/20/2006 - remove this class, replacing it with parameterized
// collections once CAY-520 gets implemented in Cayenne > 1.2
class EnumList extends ArrayList {

    private Class enumClass;

    EnumList(Class enumClass, int capacity) {
        super(capacity);
        this.enumClass = enumClass;
    }

    private Object convertValue(Object value) {
        if (value instanceof String) {
            value = Enum.valueOf(enumClass, value.toString());
        }

        return value;
    }
    
    private Collection convertValues(Collection values) {
        if(values != null && !values.isEmpty()) {
            Collection converted = new ArrayList(values.size());
            for(Object value : values) {
                converted.add(convertValue(value));
            }
            
            return converted;
        }
        else {
            return values;
        }
    }

    @Override
    public void add(int index, Object element) {
        super.add(index, convertValue(element));
    }

    @Override
    public boolean add(Object o) {
        return super.add(convertValue(o));
    }

    @Override
    public boolean addAll(Collection c) {
        return super.addAll(convertValues(c));
    }

    @Override
    public boolean addAll(int index, Collection c) {
        return super.addAll(index, convertValues(c));
    }
}
