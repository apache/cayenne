/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.model.CallbackMethodEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.toolkit.copypaste.CMTransferable;
import org.apache.cayenne.modeler.ui.errors.ErrorDialog;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.ObjCallbackMethod;
import org.apache.cayenne.modeler.ui.project.querytype.QueryTypeDialog;
import org.apache.cayenne.modeler.undo.PasteCompoundUndoableEdit;
import org.apache.cayenne.modeler.undo.PasteUndoableEdit;
import org.apache.cayenne.query.Query;

import javax.swing.*;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action for pasting entities, queries etc. from the system buffer
 */
public class PasteAction extends AppAction implements FlavorListener {

    private static final String COPY_PATTERN = "copy of %s (%d)";

    /**
     * Constructor for PasteAction
     */
    public PasteAction(Application application) {
        super(application, "Paste");

        // add listener, so that button state would update event if clipboard was filled by other app
        Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(this);
    }

    @Override
    public String getIconName() {
        return "icon-paste.png";
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * Performs pasting items from the system buffer
     */
    @Override
    public void performAction(ActionEvent e) {
        try {
            Object content = Toolkit.getDefaultToolkit().getSystemClipboard().getData(
                    CMTransferable.CAYENNE_FLAVOR);

            Object currentObject = getProjectSession().getSelectedObject();

            if (content instanceof DataMap) {
                currentObject = getProjectSession().project().getRootNode();
            }

            if (content != null && currentObject != null) {
                DataChannelDescriptor domain = (DataChannelDescriptor) getProjectSession()
                        .project()
                        .getRootNode();
                DataMap map = getProjectSession().getSelectedDataMap();

                UndoableEdit undoableEdit;
                if (content instanceof List) {
                    undoableEdit = new PasteCompoundUndoableEdit();

                    for (Object o : (List) content) {
                        paste(currentObject, o);
                        undoableEdit.addEdit(new PasteUndoableEdit(
                                getProjectSession(),
                                domain,
                                map,
                                currentObject,
                                o));
                    }
                } else {
                    paste(currentObject, content);
                    undoableEdit = new PasteUndoableEdit(getProjectSession(), domain, map, currentObject, content);
                }

                app.getUndoManager().addEdit(undoableEdit);
            }
        } catch (UnsupportedFlavorException ufe) {
            // do nothing
        } catch (Exception ex) {
            new ErrorDialog(app, "Paste Error", ex).open();
        }
    }

    private void paste(Object where, Object content) {
        paste(where, content, (DataChannelDescriptor) getProjectSession().project().getRootNode());
    }

    /**
     * Pastes single object
     */
    public void paste(
            Object where,
            Object content,
            DataChannelDescriptor dataChannelDescriptor) {

        ProjectSession session = getProjectSession();

        // Add a little intelligence - if a tree leaf is selected, we can paste to a parent datamap
        if (isTreeLeaf(where) && isTreeLeaf(content)) {
            where = session.getSelectedDataMap();
        }

        if ((where instanceof DataChannelDescriptor || where instanceof DataNodeDescriptor) && content instanceof DataMap dataMap) {
            // paste DataMap to DataDomain or DataNode

            dataMap.setName(NameBuilder
                    .of(dataMap).parent(dataChannelDescriptor)
                    .baseName(dataMap.getName())
                    .dupesPattern(COPY_PATTERN)
                    .name());

            // Update all names in the new DataMap, so that they would not conflict with
            // names from other datamaps of this domain.
            // Add some intelligence - if we rename an entity, we should rename all links
            // to it as well
            Map<String, String> renamedDbEntities = new HashMap<>();
            Map<String, String> renamedObjEntities = new HashMap<>();

            for (DbEntity dbEntity : dataMap.getDbEntities()) {
                String oldName = dbEntity.getName();
                dbEntity.setName(NameBuilder
                        .of(dbEntity).parent(dataMap)
                        .baseName(dbEntity.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                if (!oldName.equals(dbEntity.getName())) {
                    renamedDbEntities.put(oldName, dbEntity.getName());
                }
            }

            for (ObjEntity objEntity : dataMap.getObjEntities()) {
                String oldName = objEntity.getName();
                objEntity.setName(NameBuilder
                        .of(objEntity).parent(dataMap)
                        .baseName(objEntity.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                if (!oldName.equals(objEntity.getName())) {
                    renamedObjEntities.put(oldName, objEntity.getName());
                }
            }

            for (Embeddable embeddable : dataMap.getEmbeddables()) {
                embeddable.setClassName(NameBuilder
                        .of(embeddable).parent(dataMap)
                        .baseName(embeddable.getClassName())
                        .dupesPattern(COPY_PATTERN)
                        .name());
            }

            for (Procedure procedure : dataMap.getProcedures()) {
                procedure.setName(NameBuilder
                        .of(procedure).parent(dataMap)
                        .baseName(procedure.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());
            }

            for (QueryDescriptor query : dataMap.getQueryDescriptors()) {
                query.setName(NameBuilder.of(query).parent(dataMap)
                        .baseName(query.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());
            }

            // if an entity was renamed, we rename all links to it too
            for (DbEntity dbEntity : dataMap.getDbEntities()) {
                for (DbRelationship rel : dbEntity.getRelationships()) {
                    if (renamedDbEntities.containsKey(rel.getTargetEntityName())) {
                        rel.setTargetEntityName(renamedDbEntities.get(rel
                                .getTargetEntityName()));
                    }
                }
            }
            for (ObjEntity objEntity : dataMap.getObjEntities()) {
                if (renamedDbEntities.containsKey(objEntity.getDbEntityName())) {
                    objEntity.setDbEntityName(renamedDbEntities.get(objEntity
                            .getDbEntityName()));
                }

                if (renamedObjEntities.containsKey(objEntity.getSuperEntityName())) {
                    objEntity.setSuperEntityName(renamedDbEntities.get(objEntity
                            .getSuperEntityName()));
                }

                for (ObjRelationship rel : objEntity.getRelationships()) {
                    if (renamedObjEntities.containsKey(rel.getTargetEntityName())) {
                        rel.setTargetEntityName(renamedObjEntities.get(rel
                                .getTargetEntityName()));
                    }
                }
            }

            CreateDataMapAction.onMapCreated(this, getProjectSession(), dataMap);
        } else if (where instanceof DataMap dataMap) {
            // paste DbEntity to DataMap

            // clear data map parent cache
            clearDataMapCache(dataMap);

            if (content instanceof DbEntity dbEntity) {
                dbEntity.setName(NameBuilder
                        .of(dbEntity).parent(dataMap)
                        .baseName(dbEntity.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                dataMap.addDbEntity(dbEntity);
                CreateDbEntityAction.onDbEntityCreated(this, session, dbEntity);
            } else if (content instanceof ObjEntity objEntity) {
                // paste ObjEntity to DataMap
                objEntity.setName(NameBuilder.of(objEntity).parent(dataMap)
                        .baseName(objEntity.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                dataMap.addObjEntity(objEntity);
                CreateObjEntityAction.onObjEntityCreated(
                        this,
                        session,
                        dataMap,
                        objEntity);
            } else if (content instanceof Embeddable embeddable) {
                // paste Embeddable to DataMap
                embeddable.setClassName(NameBuilder
                        .of(embeddable).parent(dataMap)
                        .baseName(embeddable.getClassName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                dataMap.addEmbeddable(embeddable);
                CreateEmbeddableAction.fireEmbeddableEvent(
                        this,
                        session,
                        dataMap,
                        embeddable);
            } else if (content instanceof QueryDescriptor query) {

                query.setName(NameBuilder
                        .of(query).parent(dataMap)
                        .dupesPattern(COPY_PATTERN)
                        .baseName(query.getName())
                        .name());
                query.setDataMap(dataMap);

                dataMap.addQueryDescriptor(query);
                QueryTypeDialog.fireQueryEvent(this, session, dataMap, query);
            } else if (content instanceof Procedure procedure) {
                // paste Procedure to DataMap
                procedure.setName(NameBuilder
                        .of(procedure).parent(dataMap)
                        .dupesPattern(COPY_PATTERN)
                        .baseName(procedure.getName())
                        .name());

                dataMap.addProcedure(procedure);
                CreateProcedureAction.fireProcedureEvent(
                        this,
                        session,
                        dataMap,
                        procedure);
            }
        } else if (where instanceof DbEntity dbEntity) {

            if (content instanceof DbAttribute attr) {
                attr.setName(NameBuilder
                        .of(attr).parent(dbEntity)
                        .dupesPattern(COPY_PATTERN)
                        .baseName(attr.getName())
                        .name());

                dbEntity.addAttribute(attr);
                CreateAttributeAction.fireDbAttributeEvent(this, session, session
                        .getSelectedDataMap(), dbEntity, attr);
            } else if (content instanceof DbRelationship rel) {
                rel.setName(NameBuilder
                        .of(rel).parent(dbEntity)
                        .baseName(rel.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                dbEntity.addRelationship(rel);
                CreateRelationshipAction.fireDbRelationshipEvent(
                        this,
                        session,
                        dbEntity,
                        rel);
            }
        } else if (where instanceof ObjEntity objEntity) {

            if (content instanceof ObjAttribute attr) {
                attr.setName(NameBuilder
                        .of(attr).parent(objEntity)
                        .baseName(attr.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                objEntity.addAttribute(attr);
                CreateAttributeAction.fireObjAttributeEvent(this, session, session
                        .getSelectedDataMap(), objEntity, attr);
            } else if (content instanceof ObjRelationship rel) {
                rel.setName(NameBuilder
                        .of(rel).parent(objEntity)
                        .baseName(rel.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                objEntity.addRelationship(rel);
                CreateRelationshipAction.fireObjRelationshipEvent(
                        this,
                        session,
                        objEntity,
                        rel);
            } else if (content instanceof ObjCallbackMethod method) {

                method.setName(NameBuilder
                        .ofCallbackMethod().parent(objEntity)
                        .baseName(method.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                objEntity.getCallbackMap()
                        .getCallbackDescriptor(method.getCallbackType().getType())
                        .addCallbackMethod(method.getName());

                CallbackMethodEvent ce = CallbackMethodEvent.ofAdd(this, method.getName());

                getProjectSession().fireCallbackMethodEvent(ce);
            }
        } else if (where instanceof Embeddable embeddable) {

            if (content instanceof EmbeddableAttribute attr) {
                attr.setName(NameBuilder
                        .of(attr).parent(embeddable)
                        .baseName(attr.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                embeddable.addAttribute(attr);
                CreateAttributeAction.fireEmbeddableAttributeEvent(
                        this,
                        session,
                        embeddable,
                        attr);
            }

        } else if (where instanceof Procedure procedure) {
            // paste param to procedure

            if (content instanceof ProcedureParameter param) {

                param.setName(NameBuilder
                        .of(param).parent(procedure)
                        .baseName(param.getName())
                        .dupesPattern(COPY_PATTERN)
                        .name());

                procedure.addCallParameter(param);
                CreateProcedureParameterAction.fireProcedureParameterEvent(
                        this,
                        session,
                        procedure,
                        param);
            }

        }
    }

    private void clearDataMapCache(DataMap dataMap) {
        MappingNamespace ns = dataMap.getNamespace();
        if (ns instanceof EntityResolver) {
            ((EntityResolver) ns).refreshMappingCache();
        }
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable object.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        return getState();
    }

    /**
     * Enables or disables the action, judging last selected component
     */
    public void updateState() {
        setEnabled(getState());
    }

    /**
     * Returns desired enable state for this action
     */
    private boolean getState() {
        try {
            Object content = Toolkit.getDefaultToolkit().getSystemClipboard().getData(
                    CMTransferable.CAYENNE_FLAVOR);

            if (content instanceof List) {
                content = ((List) content).getFirst();
            }

            Object currentObject = getProjectSession().getSelectedObject();

            if (currentObject == null) {
                return false;
            }

            //  Checking all available pairs source-pasting object
            return ((currentObject instanceof DataChannelDescriptor || currentObject instanceof DataNodeDescriptor) && content instanceof DataMap)
                    ||

                    (currentObject instanceof DataMap && isTreeLeaf(content))
                    ||

                    (currentObject instanceof DataMap && content instanceof DataMap)
                    ||

                    (currentObject instanceof DbEntity && (content instanceof DbAttribute
                            || content instanceof DbRelationship || isTreeLeaf(content)))
                    ||

                    (currentObject instanceof ObjEntity && (content instanceof ObjAttribute
                            || content instanceof ObjRelationship || content instanceof ObjCallbackMethod || isTreeLeaf(content)))
                    ||

                    (currentObject instanceof Embeddable && (content instanceof EmbeddableAttribute || isTreeLeaf(content)))
                    ||

                    (currentObject instanceof Procedure
                            && (content instanceof ProcedureParameter || isTreeLeaf(content)) ||

                            (currentObject instanceof Query && isTreeLeaf(content)));
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * @return true if the object is in a lowest level of the tree
     */
    private boolean isTreeLeaf(Object content) {
        return content instanceof DbEntity
                || content instanceof ObjEntity
                || content instanceof Embeddable
                || content instanceof Procedure
                || content instanceof Query;
    }

    public void flavorsChanged(FlavorEvent e) {
        updateState();
    }
}
