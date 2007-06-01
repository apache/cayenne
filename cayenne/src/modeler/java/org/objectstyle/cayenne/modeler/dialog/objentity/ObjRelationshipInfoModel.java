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
package org.objectstyle.cayenne.modeler.dialog.objentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.modeler.util.Comparators;
import org.objectstyle.cayenne.util.Util;
import org.scopemvc.core.IntIndexSelector;
import org.scopemvc.core.ModelChangeEvent;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;
import org.scopemvc.model.collection.ListModel;

/**
 * A Scope model for mapping an ObjRelationship to one or 
 * more DbRelationships.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class ObjRelationshipInfoModel extends BasicModel {
    static final Logger logObj = Logger.getLogger(ObjRelationshipInfoModel.class);


    public static final Selector DB_RELATIONSHIP_PATH_SELECTOR =
        Selector.fromString("dbRelationshipPath");
    public static final Selector SOURCE_ENTITY_NAME_SELECTOR =
        Selector.fromString("relationship.sourceEntity.name");
    public static final Selector SELECTED_PATH_COMPONENT_SELECTOR =
        Selector.fromString("selectedPathComponent");
    public static final Selector OBJECT_TARGET_SELECTOR =
        Selector.fromString("objectTarget");
    public static final Selector OBJECT_TARGETS_SELECTOR =
        Selector.fromString("objectTargets");
    public static final Selector RELATIONSHIP_NAME_SELECTOR =
        Selector.fromString("relationshipName");

    protected ObjRelationship relationship;
    protected ListModel dbRelationshipPath;
    protected EntityRelationshipsModel selectedPathComponent;
    protected ObjEntity objectTarget;
    protected List objectTargets;
    protected String relationshipName;

    public ObjRelationshipInfoModel(
        ObjRelationship relationship,
        Collection objEntities) {

        this.relationship = relationship;
        this.relationshipName = relationship.getName();
        this.objectTarget = (ObjEntity) relationship.getTargetEntity();

        // prepare entities - copy those that have DbEntities mapped, and then sort

        this.objectTargets = new ArrayList(objEntities.size());
        Iterator entities = objEntities.iterator();
        while (entities.hasNext()) {
            ObjEntity entity = (ObjEntity) entities.next();
            if (entity.getDbEntity() != null) {
                objectTargets.add(entity);
            }
        }

        Collections.sort(objectTargets, Comparators.getNamedObjectComparator());

        // validate -
        // current limitation is that an ObjRelationship must have source 
        // and target entities present, with DbEntities chosen.
        validateCanMap();

        // wrap path
        this.dbRelationshipPath = new ListModel();
        Iterator it = relationship.getDbRelationships().iterator();
        while (it.hasNext()) {
            DbRelationship dbRelationship = (DbRelationship) it.next();
            this.dbRelationshipPath.add(new EntityRelationshipsModel(dbRelationship));
        }

        // add dummy last relationship if we are not connected
        connectEnds();
        this.dbRelationshipPath.addModelChangeListener(this);
    }

    public ObjRelationship getRelationship() {
        return relationship;
    }

    public ListModel getDbRelationshipPath() {
        return dbRelationshipPath;
    }

    public EntityRelationshipsModel getSelectedPathComponent() {
        return selectedPathComponent;
    }

    public void setSelectedPathComponent(EntityRelationshipsModel selectedPathComponent) {
        if (this.selectedPathComponent != selectedPathComponent) {
            unlistenOldSubmodel(SELECTED_PATH_COMPONENT_SELECTOR);
            this.selectedPathComponent = selectedPathComponent;
            listenNewSubmodel(SELECTED_PATH_COMPONENT_SELECTOR);
            fireModelChange(
                ModelChangeEvent.VALUE_CHANGED,
                SELECTED_PATH_COMPONENT_SELECTOR);
        }
    }

    /**
     * Returns currently selected target of the ObjRelationship.
     */
    public ObjEntity getObjectTarget() {
        return objectTarget;
    }

    /** 
     * Sets a new target 
     */
    public void setObjectTarget(ObjEntity objectTarget) {
        if (this.objectTarget != objectTarget) {
            unlistenOldSubmodel(OBJECT_TARGET_SELECTOR);
            this.objectTarget = objectTarget;
            listenNewSubmodel(OBJECT_TARGET_SELECTOR);
            fireModelChange(ModelChangeEvent.VALUE_CHANGED, OBJECT_TARGET_SELECTOR);

            // change the list of relationships 
            breakChain(-1);
            connectEnds();
            fireModelChange(
                ModelChangeEvent.VALUE_CHANGED,
                DB_RELATIONSHIP_PATH_SELECTOR);
        }
    }

    /**
     * Returns a list of ObjEntities available for target mapping.
     */
    public List getObjectTargets() {
        return objectTargets;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public void modelChanged(ModelChangeEvent event) {

        // if a different relationship was selected, we may need to rebuild the list
        Selector selector = event.getSelector();
        while (selector != null) {
            if (selector instanceof IntIndexSelector) {
                IntIndexSelector indexSel = (IntIndexSelector) selector;
                relationshipChanged(indexSel.getIndex());
                break;
            }

            selector = selector.getNext();
        }

        super.modelChanged(event);
    }

    /**
     * Processes relationship path when path component at index was changed.
     */
    public synchronized void relationshipChanged(int index) {
        // strip everything starting from the index
        breakChain(index);

        // connect the ends
        connectEnds();
        
        // must fire with null selector, or refresh won't happen
        dbRelationshipPath.fireModelChange(VALUE_CHANGED, null);
    }

    /**
     * Stores current state of the model in the internal ObjRelationship.
     */
    public synchronized boolean savePath() {
        // check for modifications
        if (relationship.getTargetEntity() == objectTarget) {
            if (Util.nullSafeEquals(relationship.getName(), relationshipName)) {
                List oldPath = relationship.getDbRelationships();
                if (oldPath.size() == dbRelationshipPath.size()) {
                    boolean hasChanges = false;
                    for (int i = 0; i < oldPath.size(); i++) {
                        EntityRelationshipsModel next =
                            (EntityRelationshipsModel) dbRelationshipPath.get(i);
                        if (oldPath.get(i) != next.getSelectedRelationship()) {
                            hasChanges = true;
                            break;
                        }
                    }

                    if (!hasChanges) {
                        return false;
                    }
                }
            }
        }

        // detected modifications, save...
        relationship.clearDbRelationships();

        // note on events notification - this needs to be propagated
        // via old modeler events, but we leave this to the controller
        // since model knows nothing about Modeler mediator.
        relationship.setTargetEntity(objectTarget);
        relationship.setName(relationshipName);

        Iterator it = dbRelationshipPath.iterator();
        while (it.hasNext()) {
            EntityRelationshipsModel next = (EntityRelationshipsModel) it.next();
            Relationship nextPathComponent = next.getSelectedRelationship();
            if (nextPathComponent == null) {
                break;
            }

            relationship.addDbRelationship((DbRelationship) nextPathComponent);
        }

        return true;
    }

    private void breakChain(int index) {
        // strip everything starting from the index
        dbRelationshipPath.makeActive(false);

        try {
            while (dbRelationshipPath.size() > (index + 1)) {
                // remove last
                dbRelationshipPath.remove(dbRelationshipPath.size() - 1);
            }
        }
        finally {
            dbRelationshipPath.makeActive(true);
        }
    }

    // Connects last selected DbRelationship in the path to the 
    // last DbEntity, creating a dummy relationship if needed.
    private void connectEnds() {
        Relationship last = null;

        int size = dbRelationshipPath.size();
        if (size > 0) {
            EntityRelationshipsModel wrapper =
                (EntityRelationshipsModel) dbRelationshipPath.get(size - 1);
            last = wrapper.getSelectedRelationship();

        }

        Entity target = getEndEntity();

        if (last == null || last.getTargetEntity() != target) {
            // try to connect automatically, if we can't use dummy connector

            Entity source = (last == null) ? getStartEntity() : last.getTargetEntity();
            Relationship anyConnector = source.getAnyRelationship(target);
            EntityRelationshipsModel connector = null;

            connector =
                (anyConnector == null)
                    ? new EntityRelationshipsModel(source, getEndEntity())
                    : new EntityRelationshipsModel(anyConnector);

            dbRelationshipPath.makeActive(false);
            try {
                dbRelationshipPath.add(connector);
            }
            finally {
                dbRelationshipPath.makeActive(true);
            }
        }
    }

    private void validateCanMap() {
        if (relationship.getSourceEntity() == null) {
            throw new CayenneRuntimeException("Can't map relationship without source entity.");
        }

        if (relationship.getTargetEntity() == null) {
            throw new CayenneRuntimeException("Can't map relationship without target entity.");
        }

        if (getStartEntity() == null) {
            throw new CayenneRuntimeException("Can't map relationship without source DbEntity.");
        }

        if (getEndEntity() == null) {
            throw new CayenneRuntimeException("Can't map relationship without target DbEntity.");
        }
    }

    public DbEntity getStartEntity() {
        return ((ObjEntity) relationship.getSourceEntity()).getDbEntity();
    }

    public DbEntity getEndEntity() {
        return objectTarget.getDbEntity();
    }
}
