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
package org.objectstyle.cayenne.access;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.DataDomainSyncBucket.PropagatedValueFactory;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * Processes object diffs, generating DB diffs. Can be used for both UPDATE and INSERT.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainDBDiffBuilder implements GraphChangeHandler {

    private ObjEntity objEntity;
    private DbEntity dbEntity;

    // diff snapshot expressed in terms of object properties.
    private Map currentPropertyDiff;
    private Map currentArcDiff;
    private Object currentId;

    /**
     * Resets the builder to process a new combination of objEntity/dbEntity.
     */
    void reset(ObjEntity objEntity, DbEntity dbEntity) {
        this.objEntity = objEntity;
        this.dbEntity = dbEntity;
    }

    /**
     * Resets the builder to process a new object for the previously set combination of
     * objEntity/dbEntity.
     */
    private void reset() {
        currentPropertyDiff = null;
        currentArcDiff = null;
        currentId = null;
    }

    /**
     * Processes GraphDiffs of a single object, converting them to DB diff.
     */
    Map buildDBDiff(GraphDiff singleObjectDiff) {

        reset();
        singleObjectDiff.apply(this);

        if (currentPropertyDiff == null && currentArcDiff == null && currentId == null) {
            return null;
        }

        Map dbDiff = new HashMap();

        appendSimpleProperties(dbDiff);
        appendForeignKeys(dbDiff);
        appendPrimaryKeys(dbDiff);

        return dbDiff.isEmpty() ? null : dbDiff;
    }

    private void appendSimpleProperties(Map dbDiff) {
        // populate changed columns
        if (currentPropertyDiff != null) {
            Iterator it = currentPropertyDiff.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                ObjAttribute attribute = (ObjAttribute) objEntity.getAttribute(entry
                        .getKey()
                        .toString());

                // this takes care of the flattened attributes, as 'getDbAttributeName'
                // returns the last path component...
                Attribute dbAttribute = dbEntity.getAttribute(attribute
                        .getDbAttributeName());
                dbDiff.put(dbAttribute.getName(), entry.getValue());
            }
        }
    }

    private void appendForeignKeys(Map dbDiff) {
        // populate changed FKs
        if (currentArcDiff != null) {
            Iterator it = currentArcDiff.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                ObjRelationship relation = (ObjRelationship) objEntity
                        .getRelationship(entry.getKey().toString());

                DbRelationship dbRelation = (DbRelationship) relation
                        .getDbRelationships()
                        .get(0);

                ObjectId targetId = (ObjectId) entry.getValue();
                Iterator joins = dbRelation.getJoins().iterator();
                while (joins.hasNext()) {
                    DbJoin join = (DbJoin) joins.next();
                    Object value = (targetId != null) ? new PropagatedValueFactory(
                            targetId,
                            join.getTargetName()) : null;

                    dbDiff.put(join.getSourceName(), value);
                }
            }
        }
    }

    private void appendPrimaryKeys(Map dbDiff) {

        // populate changed PKs, do not override values already set by users...
        if (currentId != null) {
            Iterator it = ((ObjectId) currentId).getIdSnapshot().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                if (!dbDiff.containsKey(entry.getKey())) {
                    dbDiff.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    // ==================================================
    // GraphChangeHandler methods.
    // ==================================================

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        // note - no checking for phantom mod... assuming there is no phantom diffs

        if (currentPropertyDiff == null) {
            currentPropertyDiff = new HashMap();
        }

        currentPropertyDiff.put(property, newValue);
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {

        ObjRelationship relationship = (ObjRelationship) objEntity.getRelationship(arcId
                .toString());
        if (!relationship.isSourceIndependentFromTargetChange()) {
            if (currentArcDiff == null) {
                currentArcDiff = new HashMap();
            }
            currentArcDiff.put(arcId, targetNodeId);
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {

        ObjRelationship relationship = (ObjRelationship) objEntity.getRelationship(arcId
                .toString());
        if (!relationship.isSourceIndependentFromTargetChange()) {

            if (currentArcDiff == null) {
                currentArcDiff = new HashMap();
                currentArcDiff.put(arcId, null);
            }
            // check for situation when a substitute arc was created prior to deleting the
            // old arc...
            else if (targetNodeId.equals(currentArcDiff.get(arcId))) {
                currentArcDiff.put(arcId, null);
            }
        }
    }

    public void nodeCreated(Object nodeId) {
        // need to append PK columns
        this.currentId = nodeId;
    }

    public void nodeRemoved(Object nodeId) {
        // noop
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        // noop
    }
}
