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
package org.objectstyle.cayenne.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;

/**
 * Batched UPDATE query.
 * 
 * @author Andriy Shapochka
 */
public class UpdateBatchQuery extends BatchQuery {
    private List qualifierSnapshots;
    private List updateSnapshots;

    private List updatedAttributes;
    private List qualifierAttributes;
    private Collection nullQualifierNames;

    private List dbAttributes;

    private Iterator qualifierIterator = IteratorUtils.EMPTY_ITERATOR;
    private Iterator updateIterator = IteratorUtils.EMPTY_ITERATOR;
    private Map currentUpdate = Collections.EMPTY_MAP;
    private Map currentQualifier = Collections.EMPTY_MAP;

    private boolean usingOptimisticLocking;

    /**
     * Creates new UpdateBatchQuery.
     * 
     * @param dbEntity Table or view to update.
     * @param qualifierAttributes DbAttributes used in the WHERE clause.
     * @param nullQualifierNames DbAttribute names in the WHERE clause that have null values.
     * @param updatedAttribute DbAttributes describing updated columns.
     * @param batchCapacity Estimated size of the batch.
     */
    public UpdateBatchQuery(
        DbEntity dbEntity,
        List qualifierAttributes,
        List updatedAttribute,
        Collection nullQualifierNames,
        int batchCapacity) {

        super(dbEntity);

        this.updatedAttributes = updatedAttribute;
        this.qualifierAttributes = qualifierAttributes;
        this.nullQualifierNames =
            nullQualifierNames != null ? nullQualifierNames : Collections.EMPTY_SET;

        qualifierSnapshots = new ArrayList(batchCapacity);
        updateSnapshots = new ArrayList(batchCapacity);

        dbAttributes =
            new ArrayList(updatedAttributes.size() + qualifierAttributes.size());
        dbAttributes.addAll(updatedAttributes);
        dbAttributes.addAll(qualifierAttributes);
    }

    /**
     * Returns true if a given attribute always has a null value 
     * in the batch.
     * 
     * @since 1.1
     */
    public boolean isNull(DbAttribute attribute) {
        return nullQualifierNames.contains(attribute.getName());
    }

    /**
     * Returns true if the batch query uses optimistic locking.
     * 
     * @since 1.1
     */
    public boolean isUsingOptimisticLocking() {
        return usingOptimisticLocking;
    }

    /**
     * @since 1.1
     */
    public void setUsingOptimisticLocking(boolean usingOptimisticLocking) {
        this.usingOptimisticLocking = usingOptimisticLocking;
    }

    public void reset() {
        qualifierIterator = qualifierSnapshots.iterator();
        updateIterator = updateSnapshots.iterator();
        currentQualifier = Collections.EMPTY_MAP;
        currentUpdate = Collections.EMPTY_MAP;
    }

    public boolean next() {
        if (!qualifierIterator.hasNext()) {
            return false;
        }

        currentQualifier = (Map) qualifierIterator.next();
        currentQualifier =
            (currentQualifier != null) ? currentQualifier : Collections.EMPTY_MAP;
        currentUpdate = (Map) updateIterator.next();
        currentUpdate = (currentUpdate != null) ? currentUpdate : Collections.EMPTY_MAP;
        return true;
    }

    public Object getObject(int dbAttributeIndex) {
        DbAttribute attribute = (DbAttribute) dbAttributes.get(dbAttributeIndex);
        String name = attribute.getName();

        // take value either from updated values or id's,
        // depending on the index
        return (dbAttributeIndex < updatedAttributes.size())
            ? currentUpdate.get(name)
            : currentQualifier.get(name);
    }

    /**
     * Adds a parameter row to the batch.
     * 
     * @param qualifierSnapshot describes WHERE clause of the update; includes PK values and 
     * any attributes used in optimistic locking.
     * @param updateSnapshot describes updated columns.
     */
    public void add(Map qualifierSnapshot, Map updateSnapshot) {
        qualifierSnapshots.add(qualifierSnapshot);
        updateSnapshots.add(updateSnapshot);
    }

    public int size() {
        return qualifierSnapshots.size();
    }

    public List getDbAttributes() {
        return Collections.unmodifiableList(dbAttributes);
    }

    /**
     * @deprecated Since 1.1 use getQualifierAttributes() that include attributes
     * used in optimistic locking.
     */
    public List getIdDbAttributes() {
        return getQualifierAttributes();
    }

    /**
     * @since 1.1
     */
    public List getUpdatedAttributes() {
        return Collections.unmodifiableList(updatedAttributes);
    }

    /**
     * @deprecated Since 1.1 use getUpdatedAttributes().
     */
    public List getUpdatedDbAttributes() {
        return Collections.unmodifiableList(updatedAttributes);
    }

    /**
     * @since 1.1
     */
    public List getQualifierAttributes() {
        return Collections.unmodifiableList(qualifierAttributes);
    }

    /**
     * Returns a snapshot of the current qualifier values.
     * 
     * @since 1.1
     */
    public Map getCurrentQualifier() {
        return currentQualifier;
    }
}
