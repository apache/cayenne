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

package org.apache.cayenne.gen;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;

/**
 * Attributes and Methods for working with ObjEntities.
 * 
 * @since 1.2
 */
public class EntityUtils {

    // template substitution values
    protected String subClassName;
    protected String superClassName;
    protected String baseClassName;
    protected String subPackageName;
    protected String superPackageName;
    protected String basePackageName;

    protected DataMap primaryDataMap;
    protected ObjEntity objEntity;

    protected Collection<String> callbackNames;

    public EntityUtils(DataMap dataMap, ObjEntity objEntity, String fqnBaseClass, String fqnSuperClass,
            String fqnSubClass) {

        StringUtils stringUtils = StringUtils.getInstance();

        this.baseClassName = stringUtils.stripPackageName(fqnBaseClass);
        this.basePackageName = stringUtils.stripClass(fqnBaseClass);
        this.superClassName = stringUtils.stripPackageName(fqnSuperClass);
        this.superPackageName = stringUtils.stripClass(fqnSuperClass);
        this.subClassName = stringUtils.stripPackageName(fqnSubClass);
        this.subPackageName = stringUtils.stripClass(fqnSubClass);

        this.primaryDataMap = dataMap;

        this.objEntity = objEntity;

        this.callbackNames = new LinkedHashSet<String>();
        for (CallbackDescriptor cb : objEntity.getCallbackMap().getCallbacks()) {
            callbackNames.addAll(cb.getCallbackMethods());
        }

    }

    EntityUtils(DataMap dataMap, ObjEntity objEntity, String baseClassName, String basePackageName,
            String superClassName, String superPackageName, String subClassName, String subPackageName) {

        this.baseClassName = baseClassName;
        this.basePackageName = basePackageName;
        this.superClassName = superClassName;
        this.superPackageName = superPackageName;
        this.subClassName = subClassName;
        this.subPackageName = subPackageName;

        this.primaryDataMap = dataMap;

        this.objEntity = objEntity;

        this.callbackNames = new LinkedHashSet<String>();
        for (CallbackDescriptor cb : objEntity.getCallbackMap().getCallbacks()) {
            callbackNames.addAll(cb.getCallbackMethods());
        }
    }

    /**
     * @return Returns the primary DataMap.
     * @since 1.2
     */
    public DataMap getPrimaryDataMap() {
        return primaryDataMap;
    }

    /**
     * Returns the EntityResolver for this set of DataMaps.
     * 
     * @since 1.2
     */
    public MappingNamespace getEntityResolver() {
        return primaryDataMap.getNamespace();
    }

    /**
     * Returns true if current ObjEntity is defined as abstract.
     */
    public boolean isAbstract() {
        return isAbstract(objEntity);
    }

    /**
     * Returns true if current ObjEntity is defined as abstract.
     */
    public boolean isAbstract(ObjEntity anObjEntity) {
        if (anObjEntity == null)
            return false;

        return objEntity.isAbstract();
    }

    /**
     * Returns true if current ObjEntity contains at least one toMany
     * relationship.
     */
    public boolean hasToManyRelationships() {
        return hasToManyRelationships(objEntity);
    }

    /**
     * Returns true if an ObjEntity contains at least one toMany relationship.
     */
    public boolean hasToManyRelationships(ObjEntity anObjEntity) {
        if (anObjEntity == null) {
            return false;
        }

        for (Relationship r : anObjEntity.getRelationships()) {
            if (r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if current ObjEntity contains at least one toMany
     * relationship, ignoring those declared in superentities.
     * 
     * @since 1.2
     */
    public boolean hasToManyDeclaredRelationships() {
        return hasToManyDeclaredRelationships(objEntity);
    }

    /**
     * Returns true if an ObjEntity contains at least one toMany relationship,
     * ignoring those declared in superentities.
     * 
     * @since 1.2
     */
    public boolean hasToManyDeclaredRelationships(ObjEntity anObjEntity) {
        if (anObjEntity == null) {
            return false;
        }

        for (Relationship r : anObjEntity.getDeclaredRelationships()) {
            if (r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if current ObjEntity contains at least one toOne
     * relationship.
     */
    public boolean hasToOneRelationships() {
        return hasToOneRelationships(objEntity);
    }

    /**
     * Returns true if an ObjEntity contains at least one toOne relationship.
     */
    public boolean hasToOneRelationships(ObjEntity anObjEntity) {
        if (anObjEntity == null) {
            return false;
        }

        for (Relationship r : anObjEntity.getRelationships()) {
            if (false == r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if current ObjEntity contains at least one toOne
     * relationship, ignoring those declared in superentities.
     */
    public boolean hasToOneDeclaredRelationships() {
        return hasToOneDeclaredRelationships(objEntity);
    }

    /**
     * Returns true if an ObjEntity contains at least one toOne relationship,
     * ignoring those declared in superentities.
     */
    public boolean hasToOneDeclaredRelationships(ObjEntity anObjEntity) {
        if (anObjEntity == null) {
            return false;
        }

        for (Relationship r : anObjEntity.getDeclaredRelationships()) {
            if (!r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the map key type for a collection relationship of type
     * java.util.Map.
     * 
     * @param relationship
     *            The relationship to look up type information for.
     * @return The type of the attribute keyed on.
     */
    public String getMapKeyType(final ObjRelationship relationship) {

        ObjEntity targetEntity = (ObjEntity) relationship.getTargetEntity();

        // If the map key is null, then we're doing look-ups by actual object
        // key.
        if (relationship.getMapKey() == null) {

            // If it's a multi-column key, then the return type is always
            // ObjectId.
            DbEntity dbEntity = targetEntity.getDbEntity();
            if ((dbEntity != null) && (dbEntity.getPrimaryKeys().size() > 1)) {
                return ObjectId.class.getName();
            }

            // If it's a single column key or no key exists at all, then we
            // really don't
            // know what the key type is,
            // so default to Object.
            return Object.class.getName();
        }

        // If the map key is a non-default attribute, then fetch the attribute
        // and return
        // its type.
        ObjAttribute attribute = targetEntity.getAttribute(relationship.getMapKey());
        if (attribute == null) {
            throw new CayenneRuntimeException("Invalid map key '" + relationship.getMapKey()
                    + "', no matching attribute found");
        }

        return attribute.getType();
    }

    /**
     * @return the list of all callback names registered for the entity.
     * @since 3.0
     */
    public Collection<String> getCallbackNames() {
        return callbackNames;
    }
}
