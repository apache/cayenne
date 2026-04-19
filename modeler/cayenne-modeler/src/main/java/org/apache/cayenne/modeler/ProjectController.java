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

package org.apache.cayenne.modeler;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.modeler.event.display.*;
import org.apache.cayenne.modeler.event.model.*;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.DbAttributeListener;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.DbRelationshipListener;
import org.apache.cayenne.map.event.EmbeddableAttributeEvent;
import org.apache.cayenne.map.event.EmbeddableAttributeListener;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EmbeddableListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.ObjAttributeListener;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.map.event.ObjRelationshipListener;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.action.RevertAction;
import org.apache.cayenne.modeler.action.SaveAction;
import org.apache.cayenne.modeler.action.SaveAsAction;
import org.apache.cayenne.modeler.editor.CallbackType;
import org.apache.cayenne.modeler.editor.ObjCallbackMethod;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.pref.DataNodeDefaults;
import org.apache.cayenne.modeler.pref.ProjectStatePreferences;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.CircularArray;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.project.ConfigurationNodeParentGetter;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.util.IDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.EventListenerList;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * A controller that works with the project tree, tracking selection and dispatching project events.
 */
public class ProjectController extends CayenneController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectController.class);

    private final static int MAX_HISTORY_SIZE = 20;

    private EventListenerList listeners;
    private boolean dirty;
    private Project project;
    private Preferences projectControllerPreferences;
    private ControllerState state;
    private final CircularArray<ControllerState> controllerStateHistory;

    private EntityResolver entityResolver;
    private ProjectFileChangeTracker fileChangeTracker;

    public ProjectController(CayenneModelerController parent) {
        super(parent);
        this.listeners = new EventListenerList();
        this.controllerStateHistory = new CircularArray<>(MAX_HISTORY_SIZE);
        this.state = new ControllerState();
    }

    @Override
    public Component getView() {
        return parent.getView();
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project currentProject) {
        if (this.project != currentProject) {

            this.project = currentProject;
            this.projectControllerPreferences = null;

            if (project == null) {
                this.entityResolver = null;

                if (fileChangeTracker != null) {
                    fileChangeTracker.interrupt();
                    fileChangeTracker = null;
                }
            } else {
                if (fileChangeTracker == null) {
                    fileChangeTracker = new ProjectFileChangeTracker(this);
                    fileChangeTracker.setDaemon(true);
                    fileChangeTracker.start();
                }

                fileChangeTracker.reconfigure();

                entityResolver = new EntityResolver();

                updateEntityResolver();
            }
        }
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void updateEntityResolver() {

        Collection<DataMap> dataMaps = ((DataChannelDescriptor) project.getRootNode()).getDataMaps();

        for (DataMap dataMap : dataMaps) {
            entityResolver.addDataMap(dataMap);
            dataMap.setNamespace(entityResolver);
        }
    }

    public Preferences getProjectPreferences() {
        if (getProject() == null) {
            throw new CayenneRuntimeException("No Project selected");
        }
        if (projectControllerPreferences == null) {
            updateProjectControllerPreferences();
        }

        return projectControllerPreferences;
    }

    public ProjectStatePreferences getProjectStatePreferences() {
        return (ProjectStatePreferences) application.getCayenneProjectPreferences().getProjectDetailObject(
                ProjectStatePreferences.class, getDataDomainPreferences());
    }

    /**
     * Returns top preferences for the current project, throwing an exception if no project is selected.
     */
    public Preferences getDataDomainPreferences() {

        DataChannelDescriptor dataDomain = (DataChannelDescriptor) getProject().getRootNode();
        if (dataDomain == null) {
            throw new CayenneRuntimeException("No DataDomain selected");
        }

        return getProjectPreferences().node(dataDomain.getName());
    }

    /**
     * Returns preferences object for the current DataMap. If no preferences
     * exist for the current DataMap, a new Preferences object is created. If no
     * DataMap is currently selected, an exception is thrown. An optional
     * nameSuffix allows to address more than one defaults instance for a single
     * DataMap.
     */
    public DataMapDefaults getSelectedDataMapPreferences(String nameSuffix) {
        DataMap map = getSelectedDataMap();
        if (map == null) {
            throw new CayenneRuntimeException("No DataMap selected");
        }

        Preferences pref;
        if (nameSuffix == null || nameSuffix.isEmpty()) {
            pref = getDataDomainPreferences().node("DataMap").node(map.getName());
        } else {
            pref = getDataDomainPreferences().node("DataMap").node(map.getName()).node(nameSuffix);
        }
        return (DataMapDefaults) application.getCayenneProjectPreferences().getProjectDetailObject(
                DataMapDefaults.class, pref);
    }

    public DataMapDefaults getSelectedDataMapPreferences(DataMap dataMap) {
        Preferences pref;
        pref = getDataDomainPreferences().node("DataMap").node(dataMap.getName());

        return (DataMapDefaults) application.getCayenneProjectPreferences().getProjectDetailObject(DataMapDefaults.class, pref);
    }

    /**
     * Returns preferences object for the current DataMap, throwing an exception
     * if no DataMap is selected.
     */
    public DataNodeDefaults getSelectedDataNodePreferences() {
        DataNodeDescriptor node = getSelectedDataNode();
        if (node == null) {
            throw new CayenneRuntimeException("No DataNode selected");
        }

        return (DataNodeDefaults) application.getCayenneProjectPreferences().getProjectDetailObject(
                DataNodeDefaults.class, getDataDomainPreferences().node("DataNode").node(node.getName()));

    }

    /**
     * Returns currently selected object, null if there are none, List if there are several
     */
    public Object getSelectedObject() {
        if (getSelectedObjEntity() != null) {
            return getSelectedObjEntity();
        } else if (getSelectedDbEntity() != null) {
            return getSelectedDbEntity();
        } else if (getSelectedEmbeddable() != null) {
            return getSelectedEmbeddable();
        } else if (getSelectedQuery() != null) {
            return getSelectedQuery();
        } else if (getSelectedProcedure() != null) {
            return getSelectedProcedure();
        } else if (getSelectedDataMap() != null) {
            return getSelectedDataMap();
        } else if (getSelectedDataNode() != null) {
            return getSelectedDataNode();
        } else if (getSelectedDataDomain() != null) {
            return getSelectedDataDomain();
        } else if (getSelectedPaths() != null) { // multiple objects
            ConfigurationNode[] paths = getSelectedPaths();

            ConfigurationNodeParentGetter parentGetter = getApplication().getInjector()
                    .getInstance(ConfigurationNodeParentGetter.class);
            Object parent = parentGetter.getParent(paths[0]);

            List<ConfigurationNode> result = new ArrayList<>(Arrays.asList(paths));

            /*
             * Here we sort the list of objects to minimize the risk that
             * objects will be pasted incorrectly. For instance, ObjEntity
             * should go before Query, to increase chances that Query's root
             * would be set.
             */
            result.sort(parent instanceof DataMap
                    ? Comparators.getDataMapChildrenComparator()
                    : Comparators.getDataDomainChildrenComparator());

            return result;
        }

        return null;
    }

    public void projectOpened() {
        CayenneModelerFrame frame = (CayenneModelerFrame) getView();
        addDataNodeDisplayListener(frame);
        addDataMapDisplayListener(frame);
        addObjEntityDisplayListener(frame);
        addDbEntityDisplayListener(frame);
        addQueryDisplayListener(frame);
        addProcedureDisplayListener(frame);
        addMultipleObjectsDisplayListener(frame);
        addEmbeddableDisplayListener(frame);
    }

    public void reset() {
        clearState();
        setDirty(false);
        listeners = new EventListenerList();
        controllerStateHistory.clear();
    }

    public boolean isDirty() {
        return dirty;
    }

    /**
     * Resets all current models to null.
     */
    private void clearState() {
        state = new ControllerState();
    }

    private void saveState(DisplayEvent e) {
        if (!controllerStateHistory.contains(state)) {
            state.event = e;
            controllerStateHistory.add(state);
        }
    }

    private void removeFromHistory(EventObject e) {

        int count = controllerStateHistory.size();
        List<ControllerState> removeList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ControllerState cs = controllerStateHistory.get(i);
            if (cs == null || cs.event == null) {
                continue;
            }
            EventObject csEvent = cs.event;

            if (e instanceof EntityEvent && csEvent instanceof EntityDisplayEvent) {
                if (((EntityEvent) e).getEntity() == ((EntityDisplayEvent) csEvent).getEntity()) {
                    removeList.add(cs);
                }
            } else if (e instanceof EmbeddableEvent && csEvent instanceof EmbeddableDisplayEvent) {
                if (((EmbeddableEvent) e).getEmbeddable() == ((EmbeddableDisplayEvent) csEvent).getEmbeddable()) {
                    removeList.add(cs);
                }
            } else if (e instanceof ProcedureEvent && csEvent instanceof ProcedureDisplayEvent) {
                if (((ProcedureEvent) e).getProcedure() == ((ProcedureDisplayEvent) csEvent).getProcedure()) {
                    removeList.add(cs);
                }
            } else if (e instanceof QueryEvent && csEvent instanceof QueryDisplayEvent) {
                if (((QueryEvent) e).getQuery() == ((QueryDisplayEvent) csEvent).getQuery()) {
                    removeList.add(cs);
                }
            } else if (e instanceof DataMapEvent && csEvent instanceof DataMapDisplayEvent) {
                if (((DataMapEvent) e).getDataMap() == ((DataMapDisplayEvent) csEvent).getDataMap()) {
                    removeList.add(cs);
                }
            } else if (e instanceof DataNodeEvent && csEvent instanceof DataNodeDisplayEvent) {
                if (((DataNodeEvent) e).getDataNode() == ((DataNodeDisplayEvent) csEvent).getDataNode()) {
                    removeList.add(cs);
                }
            } else if (e instanceof DomainEvent && csEvent instanceof DomainDisplayEvent) {
                if (((DomainEvent) e).getDomain() == ((DomainDisplayEvent) csEvent).getDomain()) {
                    removeList.add(cs);
                }
            }
        }

        for (ControllerState o : removeList) {
            controllerStateHistory.remove(o);
        }
    }

    public DataChannelDescriptor getSelectedDataDomain() {
        return state.dataDomain;
    }

    public DataNodeDescriptor getSelectedDataNode() {
        return state.dataNode;
    }

    public DataMap getSelectedDataMap() {
        return state.dataMap;
    }


    public ObjEntity getSelectedObjEntity() {
        return state.objEntity;
    }

    public Embeddable getSelectedEmbeddable() {
        return state.embeddable;
    }

    public DbEntity getSelectedDbEntity() {
        return state.dbEntity;
    }

    public ObjAttribute[] getSelectedObjAttributes() {
        return state.objAttributes;
    }

    public DbAttribute[] getSelectedDbAttributes() {
        return state.dbAttributes;
    }

    public EmbeddableAttribute[] getSelectedEmbeddableAttributes() {
        return state.embeddableAttributes;
    }

    public ObjRelationship[] getSelectedObjRelationships() {
        return state.objRelationships;
    }

    public DbRelationship[] getSelectedDbRelationships() {
        return state.dbRelationships;
    }

    public QueryDescriptor getSelectedQuery() {
        return state.query;
    }

    public Procedure getSelectedProcedure() {
        return state.procedure;
    }

    public ProcedureParameter[] getSelectedProcedureParameters() {
        return state.procedureParameters;
    }

    public ConfigurationNode[] getSelectedPaths() {
        return state.paths;
    }

    public ConfigurationNode getSelectedParentPath() {
        return state.parentPath;
    }

    public DisplayEvent getLastDisplayEvent() {
        return state.event;
    }

    public void addDomainDisplayListener(DomainDisplayListener listener) {
        listeners.add(DomainDisplayListener.class, listener);
    }

    public void addDomainListener(DomainListener listener) {
        listeners.add(DomainListener.class, listener);
    }

    public void removeDomainListener(DomainListener listener) {
        listeners.remove(DomainListener.class, listener);
    }

    public void addValidationConfigDisplayListener(ValidationConfigDisplayListener listener) {
        listeners.add(ValidationConfigDisplayListener.class, listener);
    }

    public void removeValidationConfigDisplayListener(ValidationConfigDisplayListener listener) {
        listeners.remove(ValidationConfigDisplayListener.class, listener);
    }

    public void addDataNodeDisplayListener(DataNodeDisplayListener listener) {
        listeners.add(DataNodeDisplayListener.class, listener);
    }

    public void addDataNodeListener(DataNodeListener listener) {
        listeners.add(DataNodeListener.class, listener);
    }

    public void addDataMapDisplayListener(DataMapDisplayListener listener) {
        listeners.add(DataMapDisplayListener.class, listener);
    }

    public void addDataMapListener(DataMapListener listener) {
        listeners.add(DataMapListener.class, listener);
    }

    public void removeDataMapListener(DataMapListener listener) {
        listeners.remove(DataMapListener.class, listener);
    }

    public void addDbEntityListener(DbEntityListener listener) {
        listeners.add(DbEntityListener.class, listener);
    }

    public void removeDbEntityListener(DbEntityListener listener) {
        listeners.remove(DbEntityListener.class, listener);
    }

    public void addProjectSavedListener(ProjectSavedListener listener) {
        listeners.add(ProjectSavedListener.class, listener);
    }

    public void removeProjectSavedListener(ProjectSavedListener listener) {
        listeners.remove(ProjectSavedListener.class, listener);
    }

    public void addObjEntityListener(ObjEntityListener listener) {
        listeners.add(ObjEntityListener.class, listener);
    }

    public void removeObjEntityListener(ObjEntityListener listener) {
        listeners.remove(ObjEntityListener.class, listener);
    }

    public void addDbEntityDisplayListener(DbEntityDisplayListener listener) {
        listeners.add(DbEntityDisplayListener.class, listener);
    }

    public void addObjEntityDisplayListener(ObjEntityDisplayListener listener) {
        listeners.add(ObjEntityDisplayListener.class, listener);
    }

    public void addEmbeddableDisplayListener(EmbeddableDisplayListener listener) {
        listeners.add(EmbeddableDisplayListener.class, listener);
    }

    public void addEmbeddableAttributeDisplayListener(EmbeddableAttributeDisplayListener listener) {
        listeners.add(EmbeddableAttributeDisplayListener.class, listener);
    }

    public void addDbAttributeListener(DbAttributeListener listener) {
        listeners.add(DbAttributeListener.class, listener);
    }

    public void removeDbAttributeListener(DbAttributeListener listener) {
        listeners.remove(DbAttributeListener.class, listener);
    }

    public void addDbAttributeDisplayListener(DbAttributeDisplayListener listener) {
        listeners.add(DbAttributeDisplayListener.class, listener);
    }

    public void addObjAttributeListener(ObjAttributeListener listener) {
        listeners.add(ObjAttributeListener.class, listener);
    }

    public void removeObjAttributeListener(ObjAttributeListener listener) {
        listeners.remove(ObjAttributeListener.class, listener);
    }

    public void addObjAttributeDisplayListener(ObjAttributeDisplayListener listener) {
        listeners.add(ObjAttributeDisplayListener.class, listener);
    }

    public void addDbRelationshipListener(DbRelationshipListener listener) {
        listeners.add(DbRelationshipListener.class, listener);
    }

    public void removeDbRelationshipListener(DbRelationshipListener listener) {
        listeners.add(DbRelationshipListener.class, listener);
    }

    public void addDbRelationshipDisplayListener(DbRelationshipDisplayListener listener) {
        listeners.add(DbRelationshipDisplayListener.class, listener);
    }

    public void addObjRelationshipListener(ObjRelationshipListener listener) {
        listeners.add(ObjRelationshipListener.class, listener);
    }

    public void removeObjRelationshipListener(ObjRelationshipListener listener) {
        listeners.remove(ObjRelationshipListener.class, listener);
    }

    public void addObjRelationshipDisplayListener(ObjRelationshipDisplayListener listener) {
        listeners.add(ObjRelationshipDisplayListener.class, listener);
    }

    public void addQueryDisplayListener(QueryDisplayListener listener) {
        listeners.add(QueryDisplayListener.class, listener);
    }

    public void addQueryListener(QueryListener listener) {
        listeners.add(QueryListener.class, listener);
    }

    public void addProcedureDisplayListener(ProcedureDisplayListener listener) {
        listeners.add(ProcedureDisplayListener.class, listener);
    }

    public void addProcedureListener(ProcedureListener listener) {
        listeners.add(ProcedureListener.class, listener);
    }

    public void addProcedureParameterListener(ProcedureParameterListener listener) {
        listeners.add(ProcedureParameterListener.class, listener);
    }

    public void addProcedureParameterDisplayListener(ProcedureParameterDisplayListener listener) {
        listeners.add(ProcedureParameterDisplayListener.class, listener);
    }

    public void addMultipleObjectsDisplayListener(MultipleObjectsDisplayListener listener) {
        listeners.add(MultipleObjectsDisplayListener.class, listener);
    }

    public void fireDomainSelected(DomainDisplayEvent e) {
        LOGGER.debug("fireDomainSelected: {}", e.getDomain() != null ? e.getDomain().getName() : null);

        if (e.getDomain() != state.dataDomain || (
                state.dataNode != null || state.dataMap != null || state.dbEntity != null || state.objEntity != null || state.procedure != null || state.query != null || state.embeddable != null
        )) {
            clearState();
            state.dataDomain = e.getDomain();
            saveState(e);
        }

        for (DomainDisplayListener listener : listeners.getListeners(DomainDisplayListener.class)) {
            listener.currentDomainChanged(e);
        }

        // call different methods depending on whether domain was opened or closed
        if (e.getDomain() == null) {
            getApplication().getActionManager().projectOpened();
        } else {
            getApplication().getActionManager().domainSelected();
        }
    }


    /**
     * Informs all listeners of the DomainEvent. Does not send the event to its
     * originator.
     */
    public void fireDomainEvent(DomainEvent e) {
        LOGGER.debug("fireDomainEvent: {}", e.getDomain().getName());
        setDirty(true);

        if (e.getId() == MapEvent.REMOVE) {
            removeFromHistory(e);
        }

        for (DomainListener listener : listeners.getListeners(DomainListener.class)) {
            switch (e.getId()) {
                case MapEvent.CHANGE:
                    listener.domainChanged(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DomainEvent type: " + e.getId());
            }
        }
    }

    public void fireDataNodeSelected(DataNodeDisplayEvent e) {
        LOGGER.debug("fireDataNodeSelected: {}", e.getDataNode().getName());

        if (e.getDataNode() != state.dataNode || (
                state.dataMap != null || state.dbEntity != null || state.objEntity != null || state.procedure != null || state.query != null || state.embeddable != null
        )) {
            clearState();
            state.dataDomain = e.getDomain();
            state.dataNode = e.getDataNode();
            saveState(e);
        }

        for (DataNodeDisplayListener listener : listeners.getListeners(DataNodeDisplayListener.class)) {
            listener.currentDataNodeChanged(e);
        }
    }

    public void fireDataNodeEvent(DataNodeEvent e) {
        LOGGER.debug("fireDataNodeEvent: {}", e.getDataNode().getName());
        setDirty(true);

        if (e.getId() == MapEvent.REMOVE) {
            removeFromHistory(e);
        }

        for (DataNodeListener listener : listeners.getListeners(DataNodeListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.dataNodeAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.dataNodeChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.dataNodeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DataNodeEvent type: " + e.getId());
            }
        }
    }

    public void fireDataMapSelected(DataMapDisplayEvent e) {
        LOGGER.debug("fireDataMapSelected: {}", e.getDataMap().getName());
        if (e.getDataMap() != state.dataMap || (
                state.dbEntity != null || state.objEntity != null || state.procedure != null || state.query != null || state.embeddable != null
        )) {
            clearState();
            state.dataDomain = e.getDomain();
            state.dataNode = e.getDataNode();
            state.dataMap = e.getDataMap();

            saveState(e);
        }

        for (DataMapDisplayListener listener : listeners.getListeners(DataMapDisplayListener.class)) {
            listener.currentDataMapChanged(e);
        }
    }

    public void fireProjectSavedEvent(ProjectSavedEvent e) {
        LOGGER.debug("fireProjectSavedEvent");
        for (ProjectSavedListener eventListener : listeners.getListeners(ProjectSavedListener.class)) {
            eventListener.onProjectSaved(e);
        }
    }

    /**
     * Informs all listeners of the DataMapEvent. Does not send the event to its
     * originator.
     */
    public void fireDataMapEvent(DataMapEvent e) {
        LOGGER.debug("fireDataMapEvent: {}", e.getDataMap().getName());
        setDirty(true);

        if (e.getId() == MapEvent.REMOVE) {
            removeFromHistory(e);
        }

        for (DataMapListener eventListener : listeners.getListeners(DataMapListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    eventListener.dataMapAdded(e);
                    break;
                case MapEvent.CHANGE:
                    eventListener.dataMapChanged(e);
                    break;
                case MapEvent.REMOVE:
                    eventListener.dataMapRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DataMapEvent type: " + e.getId());
            }
        }
    }

    public void fireObjEntityEvent(EntityEvent e) {
        LOGGER.debug("fireObjEntityEvent: {}", e.getEntity().getName());
        setDirty(true);

        if (e.getEntity().getDataMap() != null && e.getId() == MapEvent.CHANGE) {
            e.getEntity().getDataMap().objEntityChanged(e);
        }

        if (e.getId() == MapEvent.REMOVE) {
            removeFromHistory(e);
        }

        for (ObjEntityListener listener : listeners.getListeners(ObjEntityListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.objEntityAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.objEntityChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.objEntityRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid EntityEvent type: " + e.getId());
            }
        }
    }

    public void fireDbEntityEvent(EntityEvent e) {
        LOGGER.debug("fireDbEntityEvent: {}", e.getEntity().getName());
        setDirty(true);

        if (e.getEntity().getDataMap() != null && e.getId() == MapEvent.CHANGE) {
            e.getEntity().getDataMap().dbEntityChanged(e);
        }

        if (e.getId() == MapEvent.REMOVE) {
            removeFromHistory(e);
        }

        for (DbEntityListener listener : listeners.getListeners(DbEntityListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.dbEntityAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.dbEntityChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.dbEntityRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid EntityEvent type: " + e.getId());
            }
        }
    }

    public void fireQueryEvent(QueryEvent e) {
        LOGGER.debug("fireQueryEvent: {}", e.getQuery().getName());
        setDirty(true);

        if (e.getId() == MapEvent.REMOVE) {
            removeFromHistory(e);
        }

        for (QueryListener eventListener : listeners.getListeners(QueryListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    eventListener.queryAdded(e);
                    break;
                case MapEvent.CHANGE:
                    eventListener.queryChanged(e);
                    break;
                case MapEvent.REMOVE:
                    eventListener.queryRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ProcedureEvent type: " + e.getId());
            }
        }
    }

    public void fireProcedureEvent(ProcedureEvent e) {
        LOGGER.debug("fireProcedureEvent: {}", e.getProcedure().getName());
        setDirty(true);

        if (e.getId() == MapEvent.REMOVE) {
            removeFromHistory(e);
        }

        for (ProcedureListener eventListener : listeners.getListeners(ProcedureListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    eventListener.procedureAdded(e);
                    break;
                case MapEvent.CHANGE:
                    eventListener.procedureChanged(e);
                    break;
                case MapEvent.REMOVE:
                    eventListener.procedureRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ProcedureEvent type: " + e.getId());
            }
        }
    }

    /**
     * Informs all listeners of the ProcedureEvent. Does not send the event to
     * its originator.
     */
    public void fireProcedureParameterEvent(ProcedureParameterEvent e) {
        LOGGER.debug("fireProcedureParameterEvent: {}", e.getParameter().getName());
        setDirty(true);

        EventListener[] list = listeners.getListeners(ProcedureParameterListener.class);
        for (EventListener eventListener : list) {
            ProcedureParameterListener listener = (ProcedureParameterListener) eventListener;
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.procedureParameterAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.procedureParameterChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.procedureParameterRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ProcedureParameterEvent type: " + e.getId());
            }
        }
    }

    public void rewindForward() {
        int size = controllerStateHistory.size();
        if (size < 2) {
            return;
        }

        int i = controllerStateHistory.indexOf(state);
        ControllerState cs;

        int counter = 0;
        while (true) {
            if (i < 0) {
                // A new state was created without it being saved. Move to the beginning of the list
                cs = controllerStateHistory.get(0);
            } else if (i + 1 < size) {
                // move forward
                cs = controllerStateHistory.get(i + 1);
            } else {
                // wrap around
                cs = controllerStateHistory.get(0);
            }

            if (cs.isNotSame(state)) {
                break;
            }

            // if it doesn't find it within 5 tries it is probably stuck in a loop
            if (++counter > 5) {
                break;
            }

            i++;
        }

        replayNavigationState(cs);
    }

    public void rewindBackwards() {
        int size = controllerStateHistory.size();
        if (size < 2) {
            return;
        }

        int i = controllerStateHistory.indexOf(state);
        ControllerState cs;

        int counter = 0;
        while (true) {
            if (i < 0) {
                // a new state got created without it being saved.
                try {
                    cs = controllerStateHistory.get(size - 2);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    cs = controllerStateHistory.get(size - 1);
                }
            } else if (i - 1 >= 0) {
                // move to the previous one
                cs = controllerStateHistory.get(i - 1);
            } else {
                // wrap around
                cs = controllerStateHistory.get(size - 1);
            }

            if (cs.isNotSame(state)) {
                break;
            }

            // if it doesn't find it within 5 tries it is probably stuck in a loop
            if (++counter > 5) {
                break;
            }

            i--;

        }

        replayNavigationState(cs);
    }

    private void replayNavigationState(ControllerState cs) {
        state = cs;
        DisplayEvent de = cs.event;
        if (de == null) {
            return;
        }

        if (de instanceof EntityDisplayEvent) {
            EntityDisplayEvent ede = (EntityDisplayEvent) de;
            if (ede.getEntity() instanceof ObjEntity) {
                fireObjEntitySelected(ede);
            } else if (ede.getEntity() instanceof DbEntity) {
                fireDbEntitySelected(ede);
            }
        } else if (de instanceof EmbeddableDisplayEvent) {
            fireEmbeddableSelected((EmbeddableDisplayEvent) de);
        } else if (de instanceof ProcedureDisplayEvent) {
            fireProcedureSelected((ProcedureDisplayEvent) de);
        } else if (de instanceof QueryDisplayEvent) {
            fireQuerySelected((QueryDisplayEvent) de);
        } else if (de instanceof DataMapDisplayEvent) {
            fireDataMapSelected((DataMapDisplayEvent) de);
        } else if (de instanceof DataNodeDisplayEvent) {
            fireDataNodeSelected((DataNodeDisplayEvent) de);
        } else if (de instanceof DomainDisplayEvent) {
            fireDomainSelected((DomainDisplayEvent) de);
        }
    }

    public void fireObjEntitySelected(EntityDisplayEvent e) {
        LOGGER.debug("fireObjEntitySelected: {}", e.getEntity().getName());

        if (e.getEntity() != state.objEntity) {
            clearState();
            state.dataDomain = e.getDomain();
            state.dataNode = e.getDataNode();
            state.dataMap = e.getDataMap();
            state.objEntity = (ObjEntity) e.getEntity();

            saveState(e);
        }

        for (ObjEntityDisplayListener l : listeners.getListeners(ObjEntityDisplayListener.class)) {
            l.currentObjEntityChanged(e);
        }
    }

    public void fireEmbeddableSelected(EmbeddableDisplayEvent e) {
        LOGGER.debug("fireEmbeddableSelected: {}", e.getEmbeddable().getClassName());

        if (e.getEmbeddable() != state.embeddable) {
            clearState();
            state.dataDomain = e.getDomain();
            state.dataNode = e.getDataNode();
            state.dataMap = e.getDataMap();
            state.embeddable = e.getEmbeddable();
            saveState(e);
        }

        for (EmbeddableDisplayListener l : listeners.getListeners(EmbeddableDisplayListener.class)) {
            l.currentEmbeddableChanged(e);
        }
    }

    public void fireQuerySelected(QueryDisplayEvent e) {
        LOGGER.debug("fireQuerySelected: {}", e.getQuery().getName());

        if (e.getQuery() != state.query) {
            clearState();
            state.dataDomain = e.getDomain();
            state.dataMap = e.getDataMap();
            state.query = e.getQuery();

            saveState(e);
        }

        for (QueryDisplayListener l : listeners.getListeners(QueryDisplayListener.class)) {
            l.currentQueryChanged(e);
        }
    }

    public void fireProcedureSelected(ProcedureDisplayEvent e) {
        LOGGER.debug("fireProcedureSelected: {}", e.getProcedure().getName());

        if (e.getProcedure() != state.procedure) {
            clearState();
            state.dataDomain = e.getDomain();
            state.dataMap = e.getDataMap();
            state.procedure = e.getProcedure();
            saveState(e);
        }

        for (ProcedureDisplayListener l : listeners.getListeners(ProcedureDisplayListener.class)) {
            l.currentProcedureChanged(e);
        }
    }

    public void fireProcedureParameterSelected(ProcedureParameterDisplayEvent e) {
        LOGGER.debug("fireProcedureParameterSelected: {}", e.getProcedure().getName());

        if (!Arrays.equals(e.getProcedureParameters(), state.procedureParameters)) {
            if (state.procedure != e.getProcedure()) {
                clearState();
                state.dataDomain = e.getDomain();
                state.dataMap = e.getDataMap();
                state.procedure = e.getProcedure();
            }
            state.procedureParameters = e.getProcedureParameters();
        }

        for (ProcedureParameterDisplayListener l : listeners.getListeners(ProcedureParameterDisplayListener.class)) {
            l.currentProcedureParameterChanged(e);
        }
    }

    public void fireDbEntitySelected(EntityDisplayEvent e) {
        LOGGER.debug("fireDbEntitySelected: {}", e.getEntity().getName());

        if (e.getEntity() != state.dbEntity) {
            clearState();
            state.dataDomain = e.getDomain();
            state.dataNode = e.getDataNode();
            state.dataMap = e.getDataMap();
            state.dbEntity = (DbEntity) e.getEntity();
            saveState(e);
        }

        for (DbEntityDisplayListener l : listeners.getListeners(DbEntityDisplayListener.class)) {
            l.currentDbEntityChanged(e);
        }
    }

    /**
     * Notifies all listeners of the change(add, remove) and does the change.
     */
    public void fireDbAttributeEvent(AttributeEvent e) {
        LOGGER.debug("fireDbAttributeEvent: {}", e.getAttribute().getName());
        setDirty(true);

        for (DbAttributeListener l : listeners.getListeners(DbAttributeListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    l.dbAttributeAdded(e);
                    break;
                case MapEvent.CHANGE:
                    l.dbAttributeChanged(e);
                    break;
                case MapEvent.REMOVE:
                    l.dbAttributeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid AttributeEvent type: " + e.getId());
            }
        }
    }

    public void fireDbAttributeSelected(AttributeDisplayEvent e) {
        LOGGER.debug("fireDbAttributeSelected: {}", Arrays.stream(e.getAttributes()).map(a -> a.getName()).toArray());

        if (!Arrays.equals(e.getAttributes(), state.dbAttributes)) {
            if (e.getEntity() != state.dbEntity) {
                clearState();
                state.dataDomain = e.getDomain();
                state.dataMap = e.getDataMap();
                state.dbEntity = (DbEntity) e.getEntity();
            }
            state.dbAttributes = new DbAttribute[e.getAttributes().length];
            System.arraycopy(e.getAttributes(), 0, state.dbAttributes, 0, state.dbAttributes.length);
        }

        for (DbAttributeDisplayListener l : listeners.getListeners(DbAttributeDisplayListener.class)) {
            l.currentDbAttributeChanged(e);
        }
    }

    /**
     * Notifies all listeners of the change (add, remove) and does the change.
     */
    public void fireObjAttributeEvent(AttributeEvent e) {
        LOGGER.debug("fireObjAttributeEvent: {}", e.getAttribute().getName());
        setDirty(true);

        for (ObjAttributeListener listener : listeners.getListeners(ObjAttributeListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.objAttributeAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.objAttributeChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.objAttributeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid AttributeEvent type: " + e.getId());
            }
        }
    }

    public void fireObjAttributeSelected(AttributeDisplayEvent e) {
        LOGGER.debug("fireObjAttributeSelected: {}", Arrays.stream(e.getAttributes()).map(a -> a.getName()).toArray());

        if (!Arrays.equals(e.getAttributes(), state.objAttributes)) {
            if (e.getEntity() != state.objEntity) {
                clearState();
                state.dataDomain = e.getDomain();
                state.dataMap = e.getDataMap();
                state.objEntity = (ObjEntity) e.getEntity();
            }
            state.objAttributes = new ObjAttribute[e.getAttributes().length];
            System.arraycopy(e.getAttributes(), 0, state.objAttributes, 0, state.objAttributes.length);
        }

        for (ObjAttributeDisplayListener l : listeners.getListeners(ObjAttributeDisplayListener.class)) {
            l.currentObjAttributeChanged(e);
        }
    }

    public void fireEmbeddableAttributeSelected(EmbeddableAttributeDisplayEvent ev) {
        LOGGER.debug("fireEmbeddableAttributeSelected: {}", Arrays.stream(ev.getEmbeddableAttributes()).map(a -> a.getName()).toArray());

        if (!Arrays.equals(ev.getEmbeddableAttributes(), state.embeddableAttributes)) {
            if (ev.getEmbeddable() != state.embeddable) {
                clearState();
                state.dataDomain = ev.getDomain();
                state.dataMap = ev.getDataMap();
                state.embeddable = ev.getEmbeddable();
            }
            state.embeddableAttributes = new EmbeddableAttribute[ev.getEmbeddableAttributes().length];
            System.arraycopy(ev.getEmbeddableAttributes(), 0, state.embeddableAttributes, 0, state.embeddableAttributes.length);
        }

        for (EmbeddableAttributeDisplayListener l : listeners.getListeners(EmbeddableAttributeDisplayListener.class)) {
            l.currentEmbeddableAttributeChanged(ev);
        }
    }

    /**
     * Notifies all listeners of the change(add, remove) and does the change.
     */
    public void fireDbRelationshipEvent(RelationshipEvent e) {
        LOGGER.debug("fireDbRelationshipEvent: {}", e.getRelationship().getName());
        setDirty(true);

        if (e.getId() == MapEvent.CHANGE && e.getEntity() instanceof DbEntity) {
            ((DbEntity) e.getEntity()).dbRelationshipChanged(e);
        }

        for (DbRelationshipListener listener : listeners.getListeners(DbRelationshipListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.dbRelationshipAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.dbRelationshipChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.dbRelationshipRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid RelationshipEvent type: " + e.getId());
            }
        }
    }

    public void fireDbRelationshipSelected(RelationshipDisplayEvent e) {
        LOGGER.debug("fireDbRelationshipSelected: {}", Arrays.stream(e.getRelationships()).map(r -> r.getName()).toArray());
        if (!Arrays.equals(e.getRelationships(), state.dbRelationships)) {
            if (e.getEntity() != state.dbEntity) {
                clearState();
                state.dataDomain = e.getDomain();
                state.dataMap = e.getDataMap();
                state.dbEntity = (DbEntity) e.getEntity();
            }
            state.dbRelationships = new DbRelationship[e.getRelationships().length];
            System.arraycopy(e.getRelationships(), 0, state.dbRelationships, 0, state.dbRelationships.length);
        }

        for (DbRelationshipDisplayListener listener : listeners.getListeners(DbRelationshipDisplayListener.class)) {
            listener.currentDbRelationshipChanged(e);
        }
    }

    /**
     * Notifies all listeners of the change(add, remove) and does the change.
     */
    public void fireObjRelationshipEvent(RelationshipEvent e) {
        LOGGER.debug("fireObjRelationshipEvent: {}", e.getRelationship().getName());
        setDirty(true);

        for (ObjRelationshipListener listener : listeners.getListeners(ObjRelationshipListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.objRelationshipAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.objRelationshipChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.objRelationshipRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid RelationshipEvent type: " + e.getId());
            }
        }
    }

    public void fireMultipleObjectsSelected(MultipleObjectsDisplayEvent e) {
        LOGGER.debug("fireMultipleObjectsSelected");
        clearState();
        state.paths = e.getNodes();
        state.parentPath = e.getParentNode();

        for (MultipleObjectsDisplayListener l : listeners.getListeners(MultipleObjectsDisplayListener.class)) {
            l.currentObjectsChanged(e, getApplication());
        }
    }

    public void fireObjRelationshipSelected(RelationshipDisplayEvent e) {
        LOGGER.debug("fireObjRelationshipSelected: {}", Arrays.stream(e.getRelationships()).map(r -> r.getName()).toArray());
        if (!Arrays.equals(e.getRelationships(), state.objRelationships)) {
            if (e.getEntity() != state.objEntity) {
                clearState();
                state.dataDomain = e.getDomain();
                state.dataMap = e.getDataMap();
                state.objEntity = (ObjEntity) e.getEntity();
            }
            state.objRelationships = new ObjRelationship[e.getRelationships().length];
            System.arraycopy(e.getRelationships(), 0, state.objRelationships, 0, state.objRelationships.length);
        }

        for (ObjRelationshipDisplayListener l : listeners.getListeners(ObjRelationshipDisplayListener.class)) {
            l.currentObjRelationshipChanged(e);
        }
    }

    public void fireValidationConfigSelected(ValidationConfigDisplayEvent event) {
        LOGGER.debug("fireValidationConfigSelected");
        for (ValidationConfigDisplayListener l : listeners.getListeners(ValidationConfigDisplayListener.class)) {
            l.validationOptionChanged(event);
        }
    }

    public void setDirty(boolean dirty) {
        if (this.dirty != dirty) {
            this.dirty = dirty;

            application.getActionManager().getAction(SaveAction.class).setEnabled(dirty);
            application.getActionManager().getAction(SaveAsAction.class).setEnabled(dirty);
            application.getActionManager().getAction(RevertAction.class).setEnabled(dirty);

            if (dirty) {
                ((CayenneModelerController) getParent()).onProjectModified();
            }
        }
    }

    public CallbackType getSelectedCallbackType() {
        return state.callbackType;
    }

    public void addCallbackTypeSelectionListener(CallbackTypeSelectionListener listener) {
        listeners.add(CallbackTypeSelectionListener.class, listener);
    }

    public void fireCallbackTypeSelected(CallbackTypeDisplayEvent e) {
        LOGGER.debug("fireCallbackTypeSelected: {}", e.getCallbackType());
        state.callbackType = e.getCallbackType();
        for (CallbackTypeSelectionListener l : listeners.getListeners(CallbackTypeSelectionListener.class)) {
            l.callbackTypeSelected(e);
        }
    }

    public ObjCallbackMethod[] getSelectedCallbackMethods() {
        return state.callbackMethods;
    }

    public void addCallbackMethodDisplayListener(CallbackMethodDisplayListener listener) {
        listeners.add(CallbackMethodDisplayListener.class, listener);
    }

    public void fireCallbackMethodSelected(CallbackMethodDisplayEvent e) {
        LOGGER.debug("fireCallbackMethodSelected");
        state.callbackMethods = e.getCallbackMethods();
        for (CallbackMethodDisplayListener l : listeners.getListeners(CallbackMethodDisplayListener.class)) {
            l.currentCallbackMethodChanged(e);
        }
    }

    public void addCallbackMethodListener(CallbackMethodListener listener) {
        listeners.add(CallbackMethodListener.class, listener);
    }

    public void fireCallbackMethodEvent(CallbackMethodEvent e) {
        LOGGER.debug("fireCallbackMethodEvent: {}", e.getNewName());
        setDirty(true);

        for (CallbackMethodListener listener : listeners.getListeners(CallbackMethodListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.callbackMethodAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.callbackMethodChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.callbackMethodRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid CallbackEvent type: " + e.getId());
            }
        }
    }

    public ProjectFileChangeTracker getFileChangeTracker() {
        return fileChangeTracker;
    }

    public void addEmbeddableAttributeListener(EmbeddableAttributeListener listener) {
        listeners.add(EmbeddableAttributeListener.class, listener);
    }

    public void addEmbeddableListener(EmbeddableListener listener) {
        listeners.add(EmbeddableListener.class, listener);
    }

    public void fireEmbeddableEvent(EmbeddableEvent e, DataMap map) {
        LOGGER.debug("fireEmbeddableEvent: {}", e.getEmbeddable().getClassName());
        setDirty(true);
        for (EmbeddableListener listener : listeners.getListeners(EmbeddableListener.class)) {

            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.embeddableAdded(e, map);
                    break;
                case MapEvent.CHANGE:
                    listener.embeddableChanged(e, map);
                    break;
                case MapEvent.REMOVE:
                    listener.embeddableRemoved(e, map);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid RelationshipEvent type: " + e.getId());
            }
        }
    }

    public void fireEmbeddableAttributeEvent(EmbeddableAttributeEvent e) {
        LOGGER.debug("fireEmbeddableAttributeEvent: {}", e.getEmbeddableAttribute().getName());
        setDirty(true);
        for (EmbeddableAttributeListener listener : listeners.getListeners(EmbeddableAttributeListener.class)) {

            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.embeddableAttributeAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.embeddableAttributeChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.embeddableAttributeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid RelationshipEvent type: " + e.getId());
            }
        }
    }

    public void fireProjectOnSaveEvent(ProjectOnSaveEvent e) {
        LOGGER.debug("fireProjectOnSaveEvent");
        for (ProjectOnSaveListener listener : listeners.getListeners(ProjectOnSaveListener.class)) {
            listener.beforeSaveChanges(e);
        }
    }

    public void addDataSourceListener(DataSourceListener listener) {
        listeners.add(DataSourceListener.class, listener);
    }

    public void removeDataSourceListener(DataSourceListener listener) {
        listeners.remove(DataSourceListener.class, listener);
    }

    public void fireDataSourceEvent(DataSourceEvent e) {
        LOGGER.debug("fireDataSourceEvent: {}", e.getDataSourceName());
        for (DataSourceListener listener : listeners.getListeners(DataSourceListener.class)) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.callbackDataSourceAdded(e);
                    break;
                // Change event not supported for now
                // There is no good place to catch data source modification
                /*case MapEvent.CHANGE:
                    listener.callbackDataSourceChanged(e);
                    break;*/
                case MapEvent.REMOVE:
                    listener.callbackDataSourceRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid RelationshipEvent type: " + e.getId());
            }
        }
    }

    public ArrayList<Embeddable> getEmbeddablesInCurrentDataDomain() {
        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) getProject().getRootNode();
        Collection<DataMap> maps = dataChannelDescriptor.getDataMaps();
        Iterator<DataMap> it = maps.iterator();
        ArrayList<Embeddable> embs = new ArrayList<>();
        while (it.hasNext()) {
            embs.addAll(it.next().getEmbeddables());
        }
        return embs;
    }

    public Set<String> getEmbeddableNamesInCurrentDataDomain() {
        ArrayList<Embeddable> embs = getEmbeddablesInCurrentDataDomain();
        Set<String> embNames = new HashSet<>(embs.size());
        for (Embeddable emb : embs) {
            embNames.add(emb.getClassName());
        }
        return embNames;
    }

    public void updateProjectControllerPreferences() {
        String key = getProject().getConfigurationResource() == null ? new String(IDUtil.pseudoUniqueByteSequence16())
                : project.getConfigurationResource().getURL().getPath();

        projectControllerPreferences = Preferences.userNodeForPackage(Project.class);

        if (!key.trim().isEmpty()) {
            if (key.contains(".xml")) {
                projectControllerPreferences = projectControllerPreferences.node(projectControllerPreferences
                        .absolutePath() + key.replace(".xml", ""));
            } else {
                projectControllerPreferences = projectControllerPreferences.node(
                                projectControllerPreferences.absolutePath())
                        .node(getApplication().getNewProjectTemporaryName());
            }
        }
    }

    static class ControllerState {

        private DisplayEvent event;
        private DataChannelDescriptor dataDomain;
        private DataNodeDescriptor dataNode;
        private DataMap dataMap;
        private ObjEntity objEntity;
        private DbEntity dbEntity;
        private Embeddable embeddable;

        private EmbeddableAttribute[] embeddableAttributes;

        private ObjAttribute[] objAttributes;
        private DbAttribute[] dbAttributes;
        private ObjRelationship[] objRelationships;
        private DbRelationship[] dbRelationships;

        private Procedure procedure;
        private ProcedureParameter[] procedureParameters;
        private QueryDescriptor query;
        private ConfigurationNode[] paths;
        private ConfigurationNode parentPath;
        private CallbackType callbackType;
        private ObjCallbackMethod[] callbackMethods;

        public ControllerState() {

            // life is much easier if these guys are never null
            embeddableAttributes = new EmbeddableAttribute[0];
            dbAttributes = new DbAttribute[0];
            dbRelationships = new DbRelationship[0];
            procedureParameters = new ProcedureParameter[0];
            objAttributes = new ObjAttribute[0];
            objRelationships = new ObjRelationship[0];

            callbackMethods = new ObjCallbackMethod[0];
        }

        /*
         * Used to determine if the val ControllerState is equivalent, which means if the event is refired again, will
         * it end up in the same place on the screen. This gets a bit messy at the end, because of inheritance heirarchy
         * issues.
         */
        public boolean isNotSame(ControllerState val) {

            if (val == null)
                return true;

            if (event instanceof EntityDisplayEvent && val.event instanceof EntityDisplayEvent) {
                if (((EntityDisplayEvent) val.event).getEntity() instanceof ObjEntity) {
                    return objEntity != val.objEntity;
                } else {
                    return dbEntity != val.dbEntity;
                }
            } else if (event instanceof ProcedureDisplayEvent && val.event instanceof ProcedureDisplayEvent) {
                return procedure != val.procedure;
            } else if (event instanceof QueryDisplayEvent && val.event instanceof QueryDisplayEvent) {
                return query != val.query;
            } else if (event instanceof EmbeddableDisplayEvent && val.event instanceof EmbeddableDisplayEvent) {
                return embeddable != val.embeddable;
            } else if (event.getClass() == DataMapDisplayEvent.class && event.getClass() == val.event.getClass()) {
                return dataMap != val.dataMap;
            } else if (event.getClass() == DataNodeDisplayEvent.class && event.getClass() == val.event.getClass()) {
                return dataNode != val.dataNode;
            } else if (event.getClass() == DomainDisplayEvent.class && event.getClass() == val.event.getClass()) {
                return dataDomain != val.dataDomain;
            }

            return true;
        }
    }
}
