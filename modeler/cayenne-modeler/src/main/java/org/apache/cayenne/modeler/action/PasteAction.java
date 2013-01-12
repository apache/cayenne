/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.action;

import java.awt.Toolkit;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoableEdit;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
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
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.modeler.dialog.query.QueryType;
import org.apache.cayenne.modeler.undo.PasteCompoundUndoableEdit;
import org.apache.cayenne.modeler.undo.PasteUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.CayenneTransferable;
import org.apache.cayenne.query.AbstractQuery;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.Query;

/**
 * Action for pasting entities, queries etc. from the system buffer
 */
public class PasteAction extends CayenneAction implements FlavorListener {

    private static final String COPY_PREFIX = "Copy of ";

    public static String getActionName() {
        return "Paste";
    }

    /**
     * Constructor for PasteAction
     */
    public PasteAction(Application application) {
        super(getActionName(), application);

        // add listener, so that button state would update event if clipboard was filled
        // by other app
        Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(this);
    }

    @Override
    public String getIconName() {
        return "icon-paste.gif";
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit
                .getDefaultToolkit()
                .getMenuShortcutKeyMask());
    }

    /**
     * Performs pasting items from the system buffer
     */
    @Override
    public void performAction(ActionEvent e) {
        try {
            Object content = Toolkit.getDefaultToolkit().getSystemClipboard().getData(
                    CayenneTransferable.CAYENNE_FLAVOR);

            Object currentObject = getProjectController().getCurrentObject();

            if (content != null && currentObject != null) {
                DataChannelDescriptor domain = (DataChannelDescriptor) getProjectController()
                        .getProject()
                        .getRootNode();
                DataMap map = getProjectController().getCurrentDataMap();

                UndoableEdit undoableEdit;
                if (content instanceof List) {
                    undoableEdit = new PasteCompoundUndoableEdit();

                    for (Object o : (List) content) {
                        paste(currentObject, o);
                        undoableEdit.addEdit(new PasteUndoableEdit(
                                domain,
                                map,
                                currentObject,
                                o));
                    }
                }
                else {
                    paste(currentObject, content);
                    undoableEdit = new PasteUndoableEdit(domain, map, currentObject, content);
                }

                application.getUndoManager().addEdit(undoableEdit);
            }
        }
        catch (UnsupportedFlavorException ufe) {
            // do nothing
        }
        catch (Exception ex) {
            ErrorDebugDialog.guiException(ex);
        }
    }

    private void paste(Object where, Object content) {
        paste(where, content, (DataChannelDescriptor) getProjectController()
                .getProject()
                .getRootNode(), getProjectController().getCurrentDataMap());
    }

    /**
     * Pastes single object
     */
    public void paste(
            Object where,
            Object content,
            DataChannelDescriptor domain,
            DataMap map) {
        final ProjectController mediator = getProjectController();

        /**
         * Add a little intelligence - if a tree leaf is selected, we can paste to a
         * parent datamap
         */
        if (isTreeLeaf(where) && isTreeLeaf(content)) {
            where = mediator.getCurrentDataMap();
        }

        if ((where instanceof DataChannelDescriptor || where instanceof DataNodeDescriptor)
                && content instanceof DataMap) {
            // paste DataMap to DataDomain or DataNode
            DataMap dataMap = ((DataMap) content);

            dataMap
                    .setName(getFreeName(new DataMapNameChecker(domain), dataMap
                            .getName()));

            /**
             * Update all names in the new DataMap, so that they would not conflict with
             * names from other datamaps of this domain
             */

            // add some intelligence - if we rename an entity, we should rename all links
            // to it as well
            Map<String, String> renamedDbEntities = new HashMap<String, String>();
            Map<String, String> renamedObjEntities = new HashMap<String, String>();

            Map<String, String> renamedEmbeddables = new HashMap<String, String>();

            for (DbEntity dbEntity : dataMap.getDbEntities()) {
                String oldName = dbEntity.getName();
                dbEntity.setName(getFreeName(new DbEntityNameChecker(domain), dbEntity
                        .getName()));

                if (!oldName.equals(dbEntity.getName())) {
                    renamedDbEntities.put(oldName, dbEntity.getName());
                }
            }
            for (ObjEntity objEntity : dataMap.getObjEntities()) {
                String oldName = objEntity.getName();
                objEntity.setName(getFreeName(new ObjEntityNameChecker(domain), objEntity
                        .getName()));

                if (!oldName.equals(objEntity.getName())) {
                    renamedObjEntities.put(oldName, objEntity.getName());
                }
            }

            for (Embeddable embeddable : dataMap.getEmbeddables()) {
                String oldName = embeddable.getClassName();
                embeddable.setClassName(getFreeName(
                        new EmbeddableNameChecker(domain),
                        embeddable.getClassName()));

                if (!oldName.equals(embeddable.getClassName())) {
                    renamedEmbeddables.put(oldName, embeddable.getClassName());
                }
            }

            for (Procedure procedure : dataMap.getProcedures()) {
                procedure.setName(getFreeName(new ProcedureNameChecker(domain), procedure
                        .getName()));
            }
            for (Query query : dataMap.getQueries()) {
                ((AbstractQuery) query).setName(getFreeName(
                        new QueryNameChecker(domain),
                        query.getName()));
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

            mediator.addDataMap(this, dataMap);
        }
        else if (where instanceof DataMap) {
            // paste DbEntity to DataMap
            final DataMap dataMap = ((DataMap) where);

            // clear data map parent cache
            clearDataMapCache(dataMap);

            if (content instanceof DbEntity) {
                DbEntity dbEntity = (DbEntity) content;
                dbEntity.setName(getFreeName(new DbEntityNameChecker(domain), dbEntity
                        .getName()));

                dataMap.addDbEntity(dbEntity);
                CreateDbEntityAction.fireDbEntityEvent(this, mediator, dbEntity);
            }
            else if (content instanceof ObjEntity) {
                // paste ObjEntity to DataMap
                ObjEntity objEntity = (ObjEntity) content;
                objEntity.setName(getFreeName(new ObjEntityNameChecker(domain), objEntity
                        .getName()));

                dataMap.addObjEntity(objEntity);
                CreateObjEntityAction.fireObjEntityEvent(
                        this,
                        mediator,
                        dataMap,
                        objEntity);
            }
            else if (content instanceof Embeddable) {
                // paste Embeddable to DataMap
                Embeddable embeddable = (Embeddable) content;
                embeddable.setClassName(getFreeName(
                        new EmbeddableNameChecker(domain),
                        embeddable.getClassName()));

                dataMap.addEmbeddable(embeddable);
                CreateEmbeddableAction.fireEmbeddableEvent(
                        this,
                        mediator,
                        dataMap,
                        embeddable);
            }
            else if (content instanceof EJBQLQuery) {
                EJBQLQuery query = (EJBQLQuery) content;

                query.setName(getFreeName(new QueryNameChecker(domain), query.getName()));
                query.setDataMap(dataMap);

                dataMap.addQuery(query);
                QueryType.fireQueryEvent(this, mediator, dataMap, query);
            }
            else if (content instanceof Query) {
                // paste Query to DataMap
                AbstractQuery query = (AbstractQuery) content;

                query.setName(getFreeName(new QueryNameChecker(domain), query.getName()));
                query.setDataMap(dataMap);

                dataMap.addQuery(query);
                QueryType.fireQueryEvent(this, mediator, dataMap, query);
            }
            else if (content instanceof Procedure) {
                // paste Procedure to DataMap
                Procedure procedure = (Procedure) content;
                procedure.setName(getFreeName(new ProcedureNameChecker(domain), procedure
                        .getName()));

                dataMap.addProcedure(procedure);
                CreateProcedureAction.fireProcedureEvent(
                        this,
                        mediator,
                        dataMap,
                        procedure);
            }
        }
        else if (where instanceof DbEntity) {
            final DbEntity dbEntity = (DbEntity) where;

            // attrs and rels must be unique in entity namespace
            FreeNameChecker checker = new FreeNameChecker() {

                public boolean isNameFree(String name) {
                    return dbEntity.getAttribute(name) == null
                            && dbEntity.getRelationship(name) == null;
                }
            };

            if (content instanceof DbAttribute) {
                DbAttribute attr = (DbAttribute) content;
                attr.setName(getFreeName(checker, attr.getName()));

                dbEntity.addAttribute(attr);
                CreateAttributeAction.fireDbAttributeEvent(this, mediator, mediator
                        .getCurrentDataMap(), dbEntity, attr);
            }
            else if (content instanceof DbRelationship) {
                DbRelationship rel = (DbRelationship) content;
                rel.setName(getFreeName(checker, rel.getName()));

                dbEntity.addRelationship(rel);
                CreateRelationshipAction.fireDbRelationshipEvent(
                        this,
                        mediator,
                        dbEntity,
                        rel);
            }
        }
        else if (where instanceof ObjEntity) {
            final ObjEntity objEntity = (ObjEntity) where;

            // attrs and rels must be unique in entity namespace
            FreeNameChecker checker = new FreeNameChecker() {

                public boolean isNameFree(String name) {
                    return objEntity.getAttribute(name) == null
                            && objEntity.getRelationship(name) == null;
                }
            };

            if (content instanceof ObjAttribute) {
                ObjAttribute attr = (ObjAttribute) content;
                attr.setName(getFreeName(checker, attr.getName()));

                objEntity.addAttribute(attr);
                CreateAttributeAction.fireObjAttributeEvent(this, mediator, mediator
                        .getCurrentDataMap(), objEntity, attr);
            }
            else if (content instanceof ObjRelationship) {
                ObjRelationship rel = (ObjRelationship) content;
                rel.setName(getFreeName(checker, rel.getName()));

                objEntity.addRelationship(rel);
                CreateRelationshipAction.fireObjRelationshipEvent(
                        this,
                        mediator,
                        objEntity,
                        rel);
            }
        }

        else if (where instanceof Embeddable) {
            final Embeddable embeddable = (Embeddable) where;

            // attrs and rels must be unique in entity namespace
            FreeNameChecker checker = new FreeNameChecker() {

                public boolean isNameFree(String name) {
                    return embeddable.getAttribute(name) == null;
                }
            };

            if (content instanceof EmbeddableAttribute) {
                EmbeddableAttribute attr = (EmbeddableAttribute) content;
                attr.setName(getFreeName(checker, attr.getName()));

                embeddable.addAttribute(attr);
                CreateAttributeAction.fireEmbeddableAttributeEvent(
                        this,
                        mediator,
                        embeddable,
                        attr);
            }

        }

        else if (where instanceof Procedure) {
            // paste param to procedure
            final Procedure procedure = (Procedure) where;

            if (content instanceof ProcedureParameter) {
                ProcedureParameter param = (ProcedureParameter) content;

                param.setName(getFreeName(new FreeNameChecker() {

                    public boolean isNameFree(String name) {
                        for (ProcedureParameter existingParam : procedure
                                .getCallParameters()) {
                            if (name.equals(existingParam.getName())) {
                                return false;
                            }
                        }

                        return true;
                    }
                }, param.getName()));

                procedure.addCallParameter(param);
                CreateProcedureParameterAction.fireProcedureParameterEvent(
                        this,
                        mediator,
                        procedure,
                        param);
            }

        }
    }

    private void clearDataMapCache(DataMap dataMap) {
        MappingNamespace ns = dataMap.getNamespace();
        if (ns instanceof EntityResolver) {
            ((EntityResolver) ns).clearCache();
        }
    }

    /**
     * Finds avaliable name for an object
     */
    private String getFreeName(FreeNameChecker checker, String defName) {
        String name = defName;

        for (int i = 0; !checker.isNameFree(name); name = COPY_PREFIX
                + defName
                + (i == 0 ? "" : " (" + i + ")"), i++)
            ;

        return name;
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
                    CayenneTransferable.CAYENNE_FLAVOR);

            if (content instanceof List) {
                content = ((List) content).get(0);
            }

            Object currentObject = getProjectController().getCurrentObject();

            if (currentObject == null) {
                return false;
            }

            /**
             * Checking all avaliable pairs source-pasting object
             */

            return ((currentObject instanceof DataChannelDescriptor || currentObject instanceof DataNodeDescriptor) && content instanceof DataMap)
                    ||

                    (currentObject instanceof DataMap && isTreeLeaf(content))
                    ||

                    (currentObject instanceof DbEntity && (content instanceof DbAttribute
                            || content instanceof DbRelationship || isTreeLeaf(content)))
                    ||

                    (currentObject instanceof ObjEntity && (content instanceof ObjAttribute
                            || content instanceof ObjRelationship || isTreeLeaf(content)))
                    ||

                    (currentObject instanceof Embeddable && (content instanceof EmbeddableAttribute || isTreeLeaf(content)))
                    ||

                    (currentObject instanceof Procedure
                            && (content instanceof ProcedureParameter || isTreeLeaf(content)) ||

                    (currentObject instanceof Query && isTreeLeaf(content)));
        }
        catch (Exception ex) {
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

    /**
     * Interface for checking that specified name is free in superior DataMap, Entity etc.
     * and therefore can be used for new object
     */
    interface FreeNameChecker {

        boolean isNameFree(String name);
    }

    /**
     * FreeNameChecker implementation for choosing DataMap names
     */
    class DataMapNameChecker implements FreeNameChecker {

        DataChannelDescriptor domain;

        public DataMapNameChecker(DataChannelDescriptor domain) {
            this.domain = domain;
        }

        public boolean isNameFree(String name) {
            return domain.getDataMap(name) == null;
        }
    }

    /**
     * FreeNameChecker implementation for choosing DbEntity names
     */
    class DbEntityNameChecker implements FreeNameChecker {

        DataChannelDescriptor domain;

        public DbEntityNameChecker(DataChannelDescriptor domain) {
            this.domain = domain;
        }

        public boolean isNameFree(String name) {
            /**
             * Name mast be unique through all DataDomain, for EntityResolver to work
             * correctlys
             */
            for (DataMap map : domain.getDataMaps()) {
                if (map.getDbEntity(name) != null) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * FreeNameChecker implementation for choosing ObjEntity names
     */
    class ObjEntityNameChecker implements FreeNameChecker {

        DataChannelDescriptor domain;

        public ObjEntityNameChecker(DataChannelDescriptor domain) {
            this.domain = domain;
        }

        public boolean isNameFree(String name) {
            /**
             * Name mast be unique through all DataDomain, for EntityResolver to work
             * correctly
             */
            for (DataMap map : domain.getDataMaps()) {
                if (map.getObjEntity(name) != null) {
                    return false;
                }
            }

            return true;
        }
    }

    class EmbeddableNameChecker implements FreeNameChecker {

        DataChannelDescriptor domain;

        public EmbeddableNameChecker(DataChannelDescriptor domain) {
            this.domain = domain;
        }

        public boolean isNameFree(String name) {
            /**
             * Name mast be unique through all DataDomain, for EntityResolver to work
             * correctly
             */
            for (DataMap map : domain.getDataMaps()) {
                if (map.getEmbeddable(name) != null) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * FreeNameChecker implementation for choosing Procedure names
     */
    class ProcedureNameChecker implements FreeNameChecker {

        DataChannelDescriptor domain;

        public ProcedureNameChecker(DataChannelDescriptor domain) {
            this.domain = domain;
        }

        public boolean isNameFree(String name) {
            /**
             * Name mast be unique through all DataDomain, for EntityResolver to work
             * correctly
             */
            for (DataMap map : domain.getDataMaps()) {
                if (map.getNamespace().getProcedure(name) != null) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * FreeNameChecker implementation for choosing Query names
     */
    class QueryNameChecker implements FreeNameChecker {

        DataChannelDescriptor domain;

        public QueryNameChecker(DataChannelDescriptor domain) {
            this.domain = domain;
        }

        public boolean isNameFree(String name) {
            /**
             * Name mast be unique through all DataDomain, for EntityResolver to work
             * correctly
             */
            for (DataMap map : domain.getDataMaps()) {
                if (map.getNamespace().getQuery(name) != null) {
                    return false;
                }
            }

            return true;
        }
    }
}
