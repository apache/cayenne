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

package org.apache.cayenne.util;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Map;

/**
 * Map that stores values wrapped into {@link SoftReference}
 *
 * @see WeakValueMap
 *
 * @since 4.1
 */
public class SoftValueMap<K, V> extends ReferenceMap<K, V, SoftReference<V>> implements Serializable {

    private static final long serialVersionUID = 8146103761927411986L;

    public SoftValueMap() {
        super();
    }

    public SoftValueMap(int initialCapacity) {
        super(initialCapacity);
    }

    public SoftValueMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    @Override
    SoftReference<V> newReference(V value) {
        return new SoftReference<>(value, referenceQueue);
    }
}
