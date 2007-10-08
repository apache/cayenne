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

package org.objectstyle.cayenne.gen;

import java.util.Iterator;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.MappingNamespace;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Relationship;

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
        super();

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

    /**
     * Returns class name (without a package) of the sub class associated with this
     * generator.
     */
    public String getSubClassName() {
        return subClassName;
    }

    /**
     * Returns the super class (without a package) of the data object class associated
     * with this generator
     */
    public String getSuperClassName() {
        return superClassName;
    }

    /**
     * Returns the base class (without a package) of the data object class associated with
     * this generator. Class name must not include a package.
     */
    public String getBaseClassName() {
        return baseClassName;
    }

    /**
     * Returns Java package name of the class associated with this generator.
     */
    public String getSubPackageName() {
        return subPackageName;
    }

    /**
     * Returns <code>superPackageName</code> property that defines a superclass's
     * package name.
     */
    public String getSuperPackageName() {
        return superPackageName;
    }

    /**
     * Returns <code>basePackageName</code> property that defines a baseclass's
     * (superclass superclass) package name.
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

        Iterator it = anObjEntity.getRelationships().iterator();
        while (it.hasNext()) {
            Relationship r = (Relationship) it.next();
            if (r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if current ObjEntity contains at least one toMany relationship, ignoring
     * those declared in superentities.
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

        Iterator it = anObjEntity.getDeclaredRelationships().iterator();
        while (it.hasNext()) {
            Relationship r = (Relationship) it.next();
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

        Iterator it = anObjEntity.getRelationships().iterator();
        while (it.hasNext()) {
            Relationship r = (Relationship) it.next();
            if (false == r.isToMany()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if current ObjEntity contains at least one toOne relationship, ignoring
     * those declared in superentities.
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

        Iterator it = anObjEntity.getDeclaredRelationships().iterator();
        while (it.hasNext()) {
            Relationship r = (Relationship) it.next();
            if (!r.isToMany()) {
                return true;
            }
        }

        return false;
    }
}