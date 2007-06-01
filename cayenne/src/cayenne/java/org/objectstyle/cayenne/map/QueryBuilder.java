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
package org.objectstyle.cayenne.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.Query;

/**
 * A builder that constructs Cayenne queries from abstract configuration information
 * defined in cayenne-data-map*.dtd. This abstract builder supports values declared in the
 * DTD, allowing subclasses to define their own Query creation logic.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public abstract class QueryBuilder {

    public static final String OBJ_ENTITY_ROOT = "obj-entity";
    public static final String DB_ENTITY_ROOT = "db-entity";
    public static final String PROCEDURE_ROOT = "procedure";
    public static final String DATA_MAP_ROOT = "data-map";
    public static final String JAVA_CLASS_ROOT = "java-class";

    protected String name;
    protected Map properties;
    protected List resultColumns;
    protected String sql;
    protected Map adapterSql;
    protected Expression qualifier;
    protected List orderings;
    protected List prefetches;
    protected DataMap dataMap;
    protected String rootType;
    protected String rootName;
    protected String resultType;
    protected boolean selecting = true;

    /**
     * Builds a Query object based on internal configuration information.
     */
    public abstract Query getQuery();

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Determines query root based on configuration info.
     * 
     * @throws CayenneRuntimeException if a valid root can't be established.
     */
    protected Object getRoot() {

        if (rootType == null || DATA_MAP_ROOT.equals(rootType)) {
            return dataMap;
        }
        else if (rootName == null) {
            return null;
        }
        else if (OBJ_ENTITY_ROOT.equals(rootType)) {
            return dataMap.getObjEntity(rootName);
        }
        else if (DB_ENTITY_ROOT.equals(rootType)) {
            return dataMap.getDbEntity(rootName);
        }
        else if (PROCEDURE_ROOT.equals(rootType)) {
            return dataMap.getProcedure(rootName);
        }
        else if (JAVA_CLASS_ROOT.equals(rootType)) {
            // setting root to ObjEntity, since creating a Class requires
            // the knowledge of the ClassLoader
            return dataMap.getObjEntityForJavaClass(rootName);
        }

        return null;
    }

    public void setSelecting(String selecting) {
        // "true" is a default per DTD
        this.selecting = ("false".equalsIgnoreCase(selecting)) ? false : true;
    }
    
    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    /**
     * Sets the information pertaining to the root of the query.
     */
    public void setRoot(DataMap dataMap, String rootType, String rootName) {
        this.dataMap = dataMap;
        this.rootType = rootType;
        this.rootName = rootName;
    }

    /**
     * Adds raw sql. If adapterClass parameter is not null, sets the SQL string to be
     * adapter-specific. Otherwise it is used as a default SQL string.
     */
    public void addSql(String sql, String adapterClass) {
        if (adapterClass == null) {
            this.sql = sql;
        }
        else {
            if (adapterSql == null) {
                adapterSql = new HashMap();
            }

            adapterSql.put(adapterClass, sql);
        }
    }

    public void setQualifier(String qualifier) {
        if (qualifier == null || qualifier.trim().length() == 0) {
            this.qualifier = null;
        }
        else {
            this.qualifier = Expression.fromString(qualifier.trim());
        }
    }

    public void addProperty(String name, String value) {
        if (properties == null) {
            properties = new HashMap();
        }

        properties.put(name, value);
    }

    public void addResultColumn(String label, String dbType, String objectType) {
        if (resultColumns == null) {
            resultColumns = new ArrayList();
        }

        resultColumns.add(new ResultColumn(label, dbType, objectType));
    }

    public void addOrdering(String path, String descending, String ignoreCase) {
        if (orderings == null) {
            orderings = new ArrayList();
        }

        if (path != null && path.trim().length() == 0) {
            path = null;
        }
        boolean isDescending = "true".equalsIgnoreCase(descending);
        boolean isIgnoringCase = "true".equalsIgnoreCase(ignoreCase);
        orderings.add(new Ordering(path, !isDescending, isIgnoringCase));
    }

    public void addPrefetch(String path) {
        if (path == null || path != null && path.trim().length() == 0) {
            // throw??
            return;
        }

        if (prefetches == null) {
            prefetches = new ArrayList();
        }
        prefetches.add(path.trim());
    }

    static class ResultColumn {

        String label;
        String dbType;
        String objectType;

        ResultColumn(String label, String dbType, String objectType) {
            this.label = label;
            this.dbType = dbType;
            this.objectType = objectType;
        }
    }
}