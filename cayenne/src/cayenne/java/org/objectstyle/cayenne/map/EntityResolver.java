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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.collection.CompositeCollection;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.query.Query;

/**
 * Represents a virtual shared namespace for zero or more DataMaps. 
 * EntityResolver is used by Cayenne runtime and in addition to 
 * EntityNamespace interface methods implements other convenience 
 * lookups, resolving entities for Queries, Java classes, etc. DataMaps 
 * can be added or removed dynamically at runtime.
 * 
 * <p>EntityResolver is thread-safe.</p>
 * 
 * @since 1.1 In 1.1 EntityResolver was moved from the access package.
 * @author Andrei Adamchik
 */
public class EntityResolver
    extends org.objectstyle.cayenne.access.EntityResolver
    implements MappingNamespace {
    // NOTE: this class explicitly overrides all superclass methods to avoid
    // deprecation warnings all over the code.

    public EntityResolver() {
        super();
    }

    public EntityResolver(Collection dataMaps) {
        super(dataMaps);
    }

    public Collection getDbEntities() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getDbEntities());
        }

        return c;
    }

    public Collection getObjEntities() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getObjEntities());
        }

        return c;
    }

    public Collection getProcedures() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getProcedures());
        }

        return c;
    }

    public Collection getQueries() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getQueries());
        }

        return c;
    }

    public DbEntity getDbEntity(String name) {
        return _lookupDbEntity(name);
    }

    public ObjEntity getObjEntity(String name) {
        return _lookupObjEntity(name);
    }

    public Procedure getProcedure(String name) {
        return lookupProcedure(name);
    }

    public Query getQuery(String name) {
        return lookupQuery(name);
    }

    public synchronized void addDataMap(DataMap map) {
        // in addition to super logic, we must set parent namespace
        if (!maps.contains(map)) {
            maps.add(map);
            map.setNamespace(this);
            clearCache();
        }
    }

    public synchronized void clearCache() {
        super.clearCache();
    }

    protected synchronized void constructCache() {
        super.constructCache();
    }

    public synchronized DataMap getDataMap(String mapName) {
        return super.getDataMap(mapName);
    }

    public Collection getDataMaps() {
        return super.getDataMaps();
    }

    public synchronized DataMap lookupDataMap(Query q) {
        return super.lookupDataMap(q);
    }

    public synchronized DbEntity lookupDbEntity(Class aClass) {
        return super.lookupDbEntity(aClass);
    }

    public synchronized DbEntity lookupDbEntity(DataObject dataObject) {
        return super.lookupDbEntity(dataObject);
    }

    public synchronized DbEntity lookupDbEntity(Query q) {
        return super.lookupDbEntity(q);
    }

    public EntityInheritanceTree lookupInheritanceTree(ObjEntity entity) {
        return super.lookupInheritanceTree(entity);
    }

    public synchronized ObjEntity lookupObjEntity(Class aClass) {
        return super.lookupObjEntity(aClass);
    }

    public synchronized ObjEntity lookupObjEntity(DataObject dataObject) {
        return super.lookupObjEntity(dataObject);
    }

    public synchronized ObjEntity lookupObjEntity(Query q) {
        return super.lookupObjEntity(q);
    }

    public Procedure lookupProcedure(Query q) {
        return super.lookupProcedure(q);
    }

    public synchronized void removeDataMap(DataMap map) {
        super.removeDataMap(map);
    }

    public synchronized void setDataMaps(Collection maps) {
        super.setDataMaps(maps);
    }

    public boolean isIndexedByClass() {
        return super.isIndexedByClass();
    }

    public void setIndexedByClass(boolean b) {
        super.setIndexedByClass(b);
    }
}
