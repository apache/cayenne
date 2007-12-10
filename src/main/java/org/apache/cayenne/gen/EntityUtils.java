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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Relationship;

/**
 * Attributes and Methods for working with ObjEntities.
 * 
 * @since 1.2
 * @author Mike Kienenberger
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

    public EntityUtils(DataMap dataMap, ObjEntity objEntity, String fqnBaseClass,
            String fqnSuperClass, String fqnSubClass) {

        StringUtils stringUtils = StringUtils.getInstance();

        this.baseClassName = stringUtils.stripPackageName(fqnBaseClass);
        this.basePackageName = stringUtils.stripClass(fqnBaseClass);
        this.superClassName = stringUtils.stripPackageName(fqnSuperClass);
        this.superPackageName = stringUtils.stripClass(fqnSuperClass);
        this.subClassName = stringUtils.stripPackageName(fqnSubClass);
        this.subPackageName = stringUtils.stripClass(fqnSubClass);

        this.primaryDataMap = dataMap;

        this.objEntity = objEntity;
    }

    EntityUtils(DataMap dataMap, ObjEntity objEntity, String baseClassName,
            String basePackageName, String superClassName, String superPackageName,
            String subClassName, String subPackageName) {

        this.baseClassName = baseClassName;
        this.basePackageName = basePackageName;
        this.superClassName = superClassName;
        this.superPackageName = superPackageName;
        this.subClassName = subClassName;
        this.subPackageName = subPackageName;

        this.primaryDataMap = dataMap;

        this.objEntity = objEntity;
    }

    /**
     * Returns class name (without a package) of the sub class associated with this
     * generator.
     * 
     * @deprecated since 3.0 This value is a part of velocity context and therefore is
     *             redundant here.
     */
    public String getSubClassName() {
        return subClassName;
    }

    /**
     * Returns the super class (without a package) of the data object class associated
     * with this generator
     * 
     * @deprecated since 3.0 This value is a part of velocity context and therefore is
     *             redundant here.
     */
    public String getSuperClassName() {
        return superClassName;
    }

    /**
     * Returns the base class (without a package) of the data object class associated with
     * this generator. Class name must not include a package.
     * 
     * @deprecated since 3.0 This value is a part of velocity context and therefore is
     *             redundant here.
     */
    public String getBaseClassName() {
        return baseClassName;
    }

    /**
     * Returns Java package name of the class associated with this generator.
     * 
     * @deprecated since 3.0 This value is a part of velocity context and therefore is
     *             redundant here.
     */
    public String getSubPackageName() {
        return subPackageName;
    }

    /**
     * Returns <code>superPackageName</code> property that defines a superclass's
     * package name.
     * 
     * @deprecated since 3.0 This value is a part of velocity context and therefore is
     *             redundant here.
     */
    public String getSuperPackageName() {
        return superPackageName;
    }

    /**
     * Returns <code>basePackageName</code> property that defines a baseclass's
     * (superclass superclass) package name.
     * 
     * @deprecated since 3.0 This value is a part of velocity context and therefore is
     *             redundant here.
     */
    public String getBasePackageName() {
        return basePackageName;
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
     * Returns true if current ObjEntity contains at least one toMany relationship.
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
     * Returns true if current ObjEntity contains at least one toMany relationship,
     * ignoring those declared in superentities.
     * 
     * @since 1.2
     */
    public boolean hasToManyDeclaredRelationships() {
        return hasToManyDeclaredRelationships(objEntity);
    }

    /**
     * Returns true if an ObjEntity contains at least one toMany relationship, ignoring
     * those declared in superentities.
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
     * Returns true if current ObjEntity contains at least one toOne relationship.
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
     * Returns true if current ObjEntity contains at least one toOne relationship,
     * ignoring those declared in superentities.
     */
    public boolean hasToOneDeclaredRelationships() {
        return hasToOneDeclaredRelationships(objEntity);
    }

    /**
     * Returns true if an ObjEntity contains at least one toOne relationship, ignoring
     * those declared in superentities.
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
}
