/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.wocompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.Query;

/**
 * An EOObjEntity is a mapping descriptor of a Java class property with added fields for
 * WebObjects EOModel. It is an informal "decorator" of Cayenne ObjEntity to provide
 * access to the extra information of WebObjects EOEntity.
 * 
 * @author Dario Bagatto
 */
public class EOObjEntity extends ObjEntity {

    // flag that indicates whether this Entity represents a client java class
    protected boolean isClientEntity;
    // flag that indicates whether this Entity has a superclass set in the eonmodel
    protected boolean hasSuperClass;
    // flag that indicates whether this Entity is set as abstract in the eomodel
    protected boolean isAbstractEntity;

    private Collection filteredQueries;

    public EOObjEntity() {
        super();
    }

    public EOObjEntity(String s) {
        super(s);
    }

    /**
     * Sets the the superclass state.
     * 
     * @param value
     */
    public void setHasSuperClass(boolean value) {
        hasSuperClass = value;
    }

    /**
     * Returns the superclass state.
     * 
     * @return true when there is a superclass defined in the eomodel.
     */
    public boolean getHasSuperClass() {
        return hasSuperClass;
    }

    /**
     * Sets the client entity state.
     * 
     * @param value
     */
    public void setIsClientEntity(boolean value) {
        isClientEntity = value;
    }

    /**
     * Returns the client entity flag
     * 
     * @return true when this entity object represents a client java class.
     */
    public boolean getIsClientEntity() {
        return isClientEntity;
    }

    /**
     * Sets the abstract entity flag.
     * 
     * @param value
     */
    public void setIsAbstractEntity(boolean value) {
        isAbstractEntity = value;
    }

    /**
     * Returns the abstract Entity state
     * 
     * @return true if this entity is set as abstract int the eomodel.
     */
    public boolean getIsAbstractEntity() {
        return isAbstractEntity;
    }

    /**
     * Translates query name local to the ObjEntity to the global name. This translation
     * is needed since EOModels store queries by entity, while Cayenne DataMaps store them
     * globally.
     * 
     * @since 1.1
     */
    public String qualifiedQueryName(String queryName) {
        return getName() + "_" + queryName;
    }

    /**
     * @since 1.1
     */
    public String localQueryName(String qualifiedQueryName) {
        return (qualifiedQueryName != null && qualifiedQueryName.startsWith(getName()
                + "_"))
                ? qualifiedQueryName.substring(getName().length() + 1)
                : qualifiedQueryName;
    }

    /**
     * Returns stored EOQuery.
     * 
     * @since 1.1
     */
    public EOQuery getEOQuery(String queryName) {
        Query query = getDataMap().getQuery(qualifiedQueryName(queryName));
        if (query instanceof EOQuery) {
            return (EOQuery) query;
        }

        return null;
    }

    /**
     * Returns a collection of queries for this entity.
     * 
     * @since 1.1
     */
    public Collection getEOQueries() {
        if (filteredQueries == null) {
            Collection queries = getDataMap().getQueries();
            if (queries.isEmpty()) {
                filteredQueries = Collections.EMPTY_LIST;
            } else {
                Map params = Collections.singletonMap("root", EOObjEntity.this);
                Expression filter = Expression
                        .fromString("root = $root")
                        .expWithParameters(params);

                filteredQueries = filter.filter(queries, new ArrayList());
            }
        }

        return filteredQueries;
    }
}