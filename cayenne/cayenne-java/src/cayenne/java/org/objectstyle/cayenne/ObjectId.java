/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.objectstyle.cayenne.util.IDUtil;
import org.objectstyle.cayenne.util.Util;

/**
 * A portable global identifier for persistent objects. ObjectId can be temporary (used
 * for transient or new uncommitted objects) or permanent (used for objects that have been
 * already stored in DB). A temporary ObjectId stores object entity name and a
 * pseudo-unique binary key; permanent id stores a map of values from an external
 * persistent store (aka "primary key").
 * 
 * @author Andrus Adamchik
 */
public class ObjectId implements Serializable {

    protected String entityName;
    protected Map objectIdKeys;
    
    private String singleKey;
    private Object singleValue; 
    

    protected byte[] key;

    protected Map replacementIdMap;

    // hash code is transient to make sure id is portable across VM
    transient int hashCode;

    /**
     * Converts class to the entity name using default naming convention used by the
     * Modeler. I.e. package name stripped from class. A utility method simplifying
     * migration from class-based ObjectIds to the entity-based.
     * 
     * @since 1.2
     * @deprecated since 1.2
     */
    static final String entityNameFromClass(Class javaClass) {
        if (javaClass == null) {
            return null;
        }

        String fqn = javaClass.getName();
        int dot = fqn.lastIndexOf('.');
        return dot > 0 ? fqn.substring(dot + 1) : fqn;
    }

    // exists for deserialization with Hessian and similar
    private ObjectId() {
    }

    /**
     * Creates a TEMPORARY ObjectId. Assignes a generated unique key.
     * 
     * @since 1.2
     */
    public ObjectId(String entityName) {
        this.entityName = entityName;
        this.key = IDUtil.pseudoUniqueByteSequence8();
    }

    /**
     * Creates a TEMPORARY id with a specified entity name and a binary key. It is a
     * caller responsibility to provide a globally unique binary key.
     * 
     * @since 1.2
     */
    public ObjectId(String entityName, byte[] key) {
        this.entityName = entityName;
        this.key = key;
    }

    /**
     * Creates a portable permanent ObjectId.
     * 
     * @since 1.2
     */
    public ObjectId(String entityName, String key, int value) {
        this(entityName, key, new Integer(value));
    }

    /**
     * Creates a portable permanent ObjectId.
     * 
     * @since 1.2
     */
    public ObjectId(String entityName, String key, Object value) {
        this.entityName = entityName;

        this.singleKey = key;
        this.singleValue = value;
    }

    /**
     * Creates a portable permanent ObjectId.
     * 
     * @since 1.2
     */
    public ObjectId(String entityName, Map idMap) {
        this.entityName = entityName;

        if (idMap == null || idMap.size() == 0) {

        }
        else if (idMap.size() == 1) {
            Map.Entry e = (Map.Entry) idMap.entrySet().iterator().next();
            this.singleKey = String.valueOf(e.getKey());
            this.singleValue = e.getValue();
        }
        else {

            // we have to create a copy of the map, otherwise we may run into
            // serialization
            // problems with hessian
            this.objectIdKeys = new HashMap(idMap);
        }
    }

    /**
     * @deprecated since 1.2, as new portable ObjectIds can't store Java Class and store
     *             entity name instead. This constructor relies on default CayenneModeler
     *             naming convention to figure out entity name from class name. This may
     *             not work if the classes where mapped differently.
     */
    public ObjectId(Class objectClass) {
        this(entityNameFromClass(objectClass));
    }

    /**
     * @deprecated since 1.2, as new portable ObjectIds can't store Java Class and store
     *             entity name instead. This constructor relies on default CayenneModeler
     *             naming convention to figure out entity name from class name. This may
     *             not work if the classes where mapped differently.
     */
    public ObjectId(Class objectClass, String keyName, int id) {
        this(entityNameFromClass(objectClass), keyName, new Integer(id));
    }

    /**
     * @deprecated since 1.2, as new portable ObjectIds can't store Java Class and store
     *             entity name instead. This constructor relies on default CayenneModeler
     *             naming convention to figure out entity name from class name. This may
     *             not work if the classes where mapped differently.
     */
    public ObjectId(Class objectClass, String keyName, Object id) {
        this(entityNameFromClass(objectClass), keyName, id);
    }

    /**
     * @deprecated since 1.2, as new portable ObjectIds can't store Java Class and store
     *             entity name instead. This constructor relies on default CayenneModeler
     *             naming convention to figure out entity name from class name. This may
     *             not work if the classes where mapped differently.
     */
    public ObjectId(Class objectClass, Map idKeys) {
        this(entityNameFromClass(objectClass), idKeys);
    }

    public boolean isTemporary() {
        return key != null;
    }

    /**
     * @since 1.2
     */
    public String getEntityName() {
        return entityName;
    }

    public byte[] getKey() {
        return key;
    }

    /**
     * @deprecated since 1.2
     */
    protected void setIdKeys(Map idKeys) {
        if (idKeys != null && idKeys.size() == 1) {
            Map.Entry e = (Map.Entry) idKeys.entrySet().iterator().next();
            this.singleKey = String.valueOf(e.getKey());
            this.singleValue = e.getValue();
        }
        else {
            this.objectIdKeys = idKeys;
        }
    }

    /**
     * Returns an unmodifiable Map of persistent id values, essentailly a primary key map.
     * For temporary id returns replacement id, if it was already created. Otherwise
     * returns an empty map.
     */
    public Map getIdSnapshot() {
        if (isTemporary()) {
            return (replacementIdMap == null) ? Collections.EMPTY_MAP : Collections
                    .unmodifiableMap(replacementIdMap);
        }

        if(singleKey != null) {
            return Collections.singletonMap(singleKey, singleValue);
        }
        
        return objectIdKeys != null
                ? Collections.unmodifiableMap(objectIdKeys)
                : Collections.EMPTY_MAP;
    }

    /**
     * Returns a value of id attribute identified by the name of DbAttribute.
     * 
     * @deprecated since 1.2. This method is redundant. Use
     *             <code>getIdSnapshot().get(attrName)</code> instead.
     */
    public Object getValueForAttribute(String attrName) {
        return getIdSnapshot().get(attrName);
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ObjectId)) {
            return false;
        }

        ObjectId id = (ObjectId) object;

        if (!Util.nullSafeEquals(entityName, id.entityName)) {
            return false;
        }

        if (isTemporary()) {
            return new EqualsBuilder().append(key, id.key).isEquals();
        }

        if (singleKey != null) {
            return Util.nullSafeEquals(singleKey, id.singleKey)
                    && valueEquals(singleValue, id.singleValue);
        }

        if (id.objectIdKeys == null) {
            return objectIdKeys == null;
        }

        if (id.objectIdKeys.size() != objectIdKeys.size()) {
            return false;
        }

        Iterator entries = objectIdKeys.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            Object entryKey = entry.getKey();
            Object entryValue = entry.getValue();

            if (entryValue == null) {
                if (id.objectIdKeys.get(entryKey) != null
                        || !id.objectIdKeys.containsKey(entryKey)) {
                    return false;
                }
            }
            else {
                if (!valueEquals(entryValue, id.objectIdKeys.get(entryKey))) {
                    return false;
                }
            }
        }

        return true;
    }
    
    private final boolean valueEquals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }

        if (o1 == null) {
            return o2 == null;
        }

        if (o1 instanceof Number) {
            return o2 instanceof Number
                    && ((Number) o1).longValue() == ((Number) o2).longValue();
        }

        if (o1.getClass().isArray()) {
            return new EqualsBuilder().append(o1, o2).isEquals();
        }

        return Util.nullSafeEquals(o1, o2);
    }

    public int hashCode() {

        if (this.hashCode == 0) {

            HashCodeBuilder builder = new HashCodeBuilder(3, 5);
            builder.append(entityName.hashCode());

            if (key != null) {
                builder.append(key);
            }
            else if(singleKey != null) {
                builder.append(singleKey.hashCode());

                // must reconcile all possible numeric types
                if (singleValue instanceof Number) {
                    builder.append(((Number) singleValue).longValue());
                }
                else {
                    builder.append(singleValue);
                }
            }
            else if (objectIdKeys != null) {
                int len = objectIdKeys.size();

                // handle multiple keys - must sort the keys to use with HashCodeBuilder

                Object[] keys = objectIdKeys.keySet().toArray();
                Arrays.sort(keys);

                for (int i = 0; i < len; i++) {
                    // HashCodeBuilder will take care of processing object if it
                    // happens to be a primitive array such as byte[]

                    // also we don't have to append the key hashcode, its index will
                    // work
                    builder.append(i);

                    Object value = objectIdKeys.get(keys[i]);
                    // must reconcile all possible numeric types
                    if (value instanceof Number) {
                        builder.append(((Number) value).longValue());
                    }
                    else {
                        builder.append(value);
                    }
                }
            }

            this.hashCode = builder.toHashCode();
            assert hashCode != 0 : "Generated zero hashCode";
        }

        return hashCode;
    }

    /**
     * Returns a replacement ObjectId associated with this id. Replacement ObjectId is
     * either a permananent ObjectId for an uncommitted object or a new id for object
     * whose id depends on its relationships.
     * 
     * @deprecated Since 1.2 replacement id is built by appending to replacementIdMap.
     */
    public ObjectId getReplacementId() {
        return (isReplacementIdAttached()) ? createReplacementId() : null;
    }

    /**
     * Initializes a replacement ObjectId.
     * 
     * @deprecated Since 1.2 replacement id is built by appending to replacementIdMap.
     */
    public void setReplacementId(ObjectId replacementId) {
        if (replacementId == null) {
            replacementIdMap = null;
        }
        else {
            Map map = getReplacementIdMap();
            map.clear();
            map.putAll(replacementId.getIdSnapshot());
        }
    }

    /**
     * Returns a non-null mutable map that can be used to append replacement id values.
     * This allows to incrementally build a replacement GlobalID.
     * 
     * @since 1.2
     */
    public Map getReplacementIdMap() {
        if (replacementIdMap == null) {
            replacementIdMap = new HashMap();
        }

        return replacementIdMap;
    }

    /**
     * Creates and returns a replacement ObjectId. No validation of ID is done.
     * 
     * @since 1.2
     */
    public ObjectId createReplacementId() {
        return new ObjectId(getEntityName(), replacementIdMap);
    }

    /**
     * Returns true if there is full or partial replacement id attached to this id. This
     * method is preferrable to "!getReplacementIdMap().isEmpty()" as it avoids unneeded
     * replacement id map creation.
     */
    public boolean isReplacementIdAttached() {
        return replacementIdMap != null && !replacementIdMap.isEmpty();
    }

    /**
     * A standard toString method used for debugging.
     */
    public String toString() {

        StringBuffer buffer = new StringBuffer();

        buffer.append("<ObjectId:").append(entityName);

        if (isTemporary()) {
            buffer.append(", TEMP:");
            for (int i = 0; i < key.length; i++) {
                IDUtil.appendFormattedByte(buffer, key[i]);
            }
        }
        else if (singleKey != null) {
            buffer.append(", ").append(String.valueOf(singleKey)).append("=").append(
                    singleValue);
        }
        else if (objectIdKeys != null) {
            Iterator it = objectIdKeys.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                buffer.append(", ");
                buffer.append(String.valueOf(entry.getKey())).append("=").append(
                        entry.getValue());
            }
        }

        buffer.append(">");
        return buffer.toString();
    }
}
