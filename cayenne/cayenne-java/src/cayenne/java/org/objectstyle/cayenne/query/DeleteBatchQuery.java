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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;

/**
 * Batched delete query.
 * 
 * @author Andriy Shapochka
 */
public class DeleteBatchQuery extends BatchQuery {

    protected List qualifierSnapshots;
    protected List dbAttributes;
    protected boolean usingOptimisticLocking;

    private List qualifierAttributes;
    private Collection nullQualifierNames;

    /**
     * Creates new DeleteBatchQuery. Used by
     * ContextCommit.categorizeFlattenedDeletesAndCreateBatches for deleting flattenned
     * relationships.
     * 
     * @param dbEntity Table or view to delete.
     * @param batchCapacity Estimated size of the batch.
     */
    public DeleteBatchQuery(DbEntity dbEntity, int batchCapacity) {
        this(dbEntity, dbEntity.getPrimaryKey(), Collections.EMPTY_SET, batchCapacity);
    }

    /**
     * Creates new DeleteBatchQuery.
     * 
     * @param dbEntity Table or view to delete.
     * @param qualifierAttributes DbAttributes used in the WHERE clause.
     * @param nullQualifierNames DbAttribute names in the WHERE clause that have null
     *            values.
     * @param batchCapacity Estimated size of the batch.
     */
    public DeleteBatchQuery(DbEntity dbEntity, List qualifierAttributes,
            Collection nullQualifierNames, int batchCapacity) {

        super(dbEntity);

        this.qualifierAttributes = qualifierAttributes;
        this.nullQualifierNames = nullQualifierNames != null
                ? nullQualifierNames
                : Collections.EMPTY_SET;

        qualifierSnapshots = new ArrayList(batchCapacity);
        dbAttributes = new ArrayList(qualifierAttributes.size());
        dbAttributes.addAll(qualifierAttributes);
        batchIndex = -1;
    }

    /**
     * Returns true if a given attribute always has a null value in the batch.
     * 
     * @since 1.2
     */
    public boolean isNull(DbAttribute attribute) {
        return nullQualifierNames.contains(attribute.getName());
    }

    /**
     * Returns true if the batch query uses optimistic locking.
     * 
     * @since 1.2
     */
    public boolean isUsingOptimisticLocking() {
        return usingOptimisticLocking;
    }

    /**
     * @since 1.2
     */
    public void setUsingOptimisticLocking(boolean usingOptimisticLocking) {
        this.usingOptimisticLocking = usingOptimisticLocking;
    }

    /**
     * @since 1.2
     */
    public List getQualifierAttributes() {
        return Collections.unmodifiableList(qualifierAttributes);
    }

    public Object getValue(int dbAttributeIndex) {
        DbAttribute attribute = (DbAttribute) dbAttributes.get(dbAttributeIndex);
        return getCurrentQualifier().get(attribute.getName());
    }

    public void add(Map dataObjectId) {
        qualifierSnapshots.add(dataObjectId);
    }

    public int size() {
        return qualifierSnapshots.size();
    }

    public List getDbAttributes() {
        return dbAttributes;
    }

    /**
     * Returns a snapshot of the current qualifier values.
     * 
     * @since 1.2
     */
    public Map getCurrentQualifier() {
        return (Map) qualifierSnapshots.get(batchIndex);
    }
}
