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
package org.objectstyle.cayenne.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.EntityResolver;

/**
 * BatchQuery and its descendants allow to group similar data for the batch database
 * modifications, including inserts, updates and deletes. Single BatchQuery corresponds to
 * a parameterized PreparedStatement and a matrix of values.
 * 
 * @author Andriy Shapochka
 * @author Andrus Adamchik
 */
public abstract class BatchQuery implements Query {

    /**
     * @since 1.2
     */
    protected int batchIndex;

    /**
     * @since 1.2
     */
    protected DbEntity dbEntity;

    protected String name;

    public BatchQuery(DbEntity dbEntity) {
        this.dbEntity = dbEntity;
        this.batchIndex = -1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns default select parameters.
     * 
     * @since 1.2
     */
    public QueryMetadata getMetaData(EntityResolver resolver) {
        return new DefaultQueryMetadata() {

            public DbEntity getDbEntity() {
                return dbEntity;
            }
        };
    }

    /**
     * @deprecated since 1.2 as the corresponding interface method is also deprecated.
     */
    public Object getRoot() {
        return dbEntity;
    }

    /**
     * @deprecated since 1.2
     */
    public void setRoot(Object root) {
        if (root == null || root instanceof DbEntity) {
            this.dbEntity = (DbEntity) root;
        }
        else {
            throw new CayenneRuntimeException("Only DbEntity is supported as root: "
                    + root);
        }
    }

    /**
     * @since 1.2
     */
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        router.route(
                router.engineForDataMap(dbEntity.getDataMap()),
                this,
                substitutedQuery);
    }

    /**
     * Calls "batchAction" on the visitor.
     * 
     * @since 1.2
     */
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.batchAction(this);
    }

    /**
     * Returns true if the batch query uses optimistic locking.
     * 
     * @since 1.1
     */
    public boolean isUsingOptimisticLocking() {
        return false;
    }

    /**
     * Returns a DbEntity associated with this batch.
     */
    public DbEntity getDbEntity() {
        return dbEntity;
    }

    /**
     * Returns a List of values for the current batch iteration, in the order they are
     * bound to the query. Used mainly for logging.
     * 
     * @param includeNullValues A <code>true</code> value indicates that the returned
     *            list should include <code>null</code> values and <code>false</code>
     *            indicates they should not be included.
     * @deprecated Since 1.2 use BatchQueryBuilder.getParameterValues(), as this allows
     *             better control over which attributes are logged.
     */
    public List getValuesForUpdateParameters(boolean includeNullValues) {
        int len = getDbAttributes().size();
        List values = new ArrayList(len);
        for (int i = 0; i < len; i++) {
            Object value = getObject(i);
            if (includeNullValues || value != null) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * Returns <code>true</code> if this batch query has no parameter rows.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns a list of DbAttributes describing batch parameters.
     */
    public abstract List getDbAttributes();

    /**
     * Rewinds batch to the first parameter row.
     */
    public void reset() {
        batchIndex = -1;
    }

    /**
     * Repositions batch to the next object, so that subsequent calls to getObject(int)
     * would return the values of the next batch object. Returns <code>true</code> if
     * batch has more objects to iterate over, <code>false</code> otherwise.
     */
    public boolean next() {
        batchIndex++;
        return size() > batchIndex;
    }

    /**
     * @deprecated Since 1.2 renamed to "getValue()"
     */
    public Object getObject(int valueIndex) {
        return getValue(valueIndex);
    }

    /**
     * Returns a value at a given index for the current batch iteration.
     * 
     * @since 1.2
     */
    public abstract Object getValue(int valueIndex);

    /**
     * Returns the number of parameter rows in a batch.
     */
    public abstract int size();

    /**
     * A helper method used by subclasses to resolve deferred values on demand. This is
     * useful when a certain value comes from a generated key of another master object.
     * 
     * @since 1.2
     */
    protected Object getValue(Map valueMap, DbAttribute attribute) {

        Object value = valueMap.get(attribute.getName());

        // if a value is a Factory, resolve it here...
        // slight chance that a normal value will implement Factory interface???
        if (value instanceof Factory) {
            value = ((Factory) value).create();

            // update replacement id
            if (attribute.isPrimaryKey()) {
                // sanity check
                if (value == null) {
                    String name = attribute.getEntity() != null ? attribute
                            .getEntity()
                            .getName() : "<null>";
                    throw new CayenneRuntimeException("Failed to generate PK: "
                            + name
                            + "."
                            + attribute.getName());
                }

                ObjectId id = getObjectId();
                if (id != null) {
                    // always override with fresh value as this is what's in the DB
                    id.getReplacementIdMap().put(attribute.getName(), value);
                }
            }

            // update snapshot
            valueMap.put(attribute.getName(), value);
        }

        return value;
    }

    /**
     * Returns an ObjectId associated with the current batch iteration. Used internally by
     * Cayenne to match current iteration with a specific object and assign it generated
     * keys.
     * <p>
     * Default implementation simply returns null.
     * </p>
     * 
     * @since 1.2
     */
    public ObjectId getObjectId() {
        return null;
    }
}
