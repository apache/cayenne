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

package org.apache.cayenne.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Map that transparently stores values as references and resolves them as needed.
 * <p>
 * This implementation supports serialization.
 * <p>
 * Internally data stored in HashMap thus this class and all implementations are not thread safe.
 *
 * @see WeakValueMap
 * @see SoftValueMap
 *
 * @since 4.1
 */
public abstract class ReferenceMap<K, V, R extends Reference<V>> implements Map<K, V>, Serializable {

    private static final long serialVersionUID = -3365744592038165092L;

    protected transient Map<K, R> map;

    protected transient Map<R, K> reverseMap;

    protected transient ReferenceQueue<V> referenceQueue;

    public ReferenceMap() {
        map = new HashMap<>();
        reverseMap = new HashMap<>();
        referenceQueue = new ReferenceQueue<>();
    }

    public ReferenceMap(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
        reverseMap = new HashMap<>(initialCapacity);
        referenceQueue = new ReferenceQueue<>();
    }

    public ReferenceMap(Map<? extends K, ? extends V> m) {
        this(m.size());
        putAll(m);
    }

    @Override
    public int size() {
        checkReferenceQueue();
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        checkReferenceQueue();
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        checkReferenceQueue();
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        checkReferenceQueue();
        for(R ref : map.values()) {
            if(ref != null) {
                V v = ref.get();
                if(v != null) {
                    if(v.equals(value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        checkReferenceQueue();
        R ref = map.get(key);
        if(ref == null) {
            return null;
        }
        return ref.get();
    }

    @Override
    public V put(K key, V value) {
        checkReferenceQueue();
        R refValue = newReference(value);
        R oldValue = map.put(key, refValue);
        reverseMap.put(refValue, key);
        if(oldValue == null) {
            return null;
        }
        return oldValue.get();
    }

    @Override
    public V remove(Object key) {
        checkReferenceQueue();
        R oldValue = map.remove(key);
        if(oldValue == null) {
            return null;
        }
        reverseMap.remove(oldValue);
        return oldValue.get();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkReferenceQueue();
        for(Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            R value = newReference(entry.getValue());
            map.put(entry.getKey(), value);
            reverseMap.put(value, entry.getKey());
        }
    }

    @Override
    public void clear() {
        map.clear();
        reverseMap.clear();
        resetReferenceQueue();
    }

    @Override
    public Set<K> keySet() {
        checkReferenceQueue();
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        checkReferenceQueue();
        Collection<V> values = new ArrayList<>();
        for(R v : map.values()) {
            if(v != null) {
                V value = v.get();
                if(value != null) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Cleanup all collected references
     */
    protected void checkReferenceQueue() {
        Reference<? extends V> reference;

        while((reference = referenceQueue.poll()) != null) {
            K keyToRemove = reverseMap.remove(reference);
            if(keyToRemove != null) {
                map.remove(keyToRemove);
            }
        }
    }

    private void resetReferenceQueue() {
        while(referenceQueue.poll() != null) {
            // just purge this queue
        }
    }

    /**
     * This method should be implemented by concrete implementations of this abstract class.
     *
     * @param value to be wrapped into reference
     * @return new reference to the value
     */
    abstract R newReference(V value);

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeObjectInternal(out);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readObjectInternal(in);
    }

    protected void writeObjectInternal(ObjectOutputStream out) throws IOException {
        checkReferenceQueue();
        Map<K, V> replacementMap = new HashMap<>();
        for(Entry<K, R> entry : map.entrySet()) {
            if(entry.getValue() != null) {
                replacementMap.put(entry.getKey(), entry.getValue().get());
            }
        }
        out.writeObject(replacementMap);
    }

    protected void readObjectInternal(ObjectInputStream in) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Map<K, V> replacement = (Map<K, V>) in.readObject();
        map = new HashMap<>(replacement.size());
        reverseMap = new HashMap<>(replacement.size());
        referenceQueue = new ReferenceQueue<>();
        putAll(replacement);
        map.forEach((k, v) -> {
            reverseMap.put(v, k);
        });
    }
}
