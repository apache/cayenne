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

package org.apache.cayenne.modeler.ui.project;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.Attribute;
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
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.event.model.DbAttributeEvent;
import org.apache.cayenne.modeler.event.model.DbEntityEvent;
import org.apache.cayenne.modeler.event.model.DbRelationshipEvent;
import org.apache.cayenne.modeler.event.model.ObjAttributeEvent;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.event.model.ObjRelationshipEvent;
import org.apache.cayenne.modeler.event.model.DbAttributeListener;
import org.apache.cayenne.modeler.event.model.DbEntityListener;
import org.apache.cayenne.modeler.event.model.DbRelationshipListener;
import org.apache.cayenne.modeler.event.model.EmbeddableAttributeEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableAttributeListener;
import org.apache.cayenne.modeler.event.model.EmbeddableEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableListener;
import org.apache.cayenne.modeler.event.model.ModelEvent;
import org.apache.cayenne.modeler.event.model.ObjAttributeListener;
import org.apache.cayenne.modeler.event.model.ObjEntityListener;
import org.apache.cayenne.modeler.event.model.ObjRelationshipListener;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.action.RevertAction;
import org.apache.cayenne.modeler.ui.action.SaveAction;
import org.apache.cayenne.modeler.ui.action.SaveAsAction;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.CallbackType;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.ObjCallbackMethod;
import org.apache.cayenne.modeler.event.display.*;
import org.apache.cayenne.modeler.event.model.*;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.pref.DataNodeDefaults;
import org.apache.cayenne.modeler.pref.ProjectStatePreferences;
import org.apache.cayenne.modeler.ui.ModelerController;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.project.ConfigurationNodeParentGetter;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.EventListenerList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * A controller that works with the project tree, tracking selection and dispatching project events.
 */
public class ProjectController extends ChildController<ModelerController> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectController.class);

    private ProjectNavigationHistory navigationHistory;
    private EventListenerList listeners;
    private boolean dirty;
    private Project project;
    private ProjectView projectView;
    private Preferences preferences;
    private ControllerState state;
    private EntityResolver entityResolver;
    private ProjectFileChangeTracker fileChangeTracker;

    public ProjectController(ModelerController parent) {
        super(parent);
    }

    @Override
    public ProjectView getView() {
        return projectView;
    }

    public Project getProject() {
        return project;
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

    public Preferences getPreferences() {
        if (preferences == null) {
            this.preferences = buildPreferences();
        }

        return preferences;
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

        return getPreferences().node(dataDomain.getName());
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
        Preferences pref = getDataDomainPreferences().node("DataMap").node(dataMap.getName());
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

            ConfigurationNodeParentGetter parentGetter = getApplication().getConfigurationNodeParentGetter();
            Object parent = parentGetter.getParent(paths[0]);

            List<ConfigurationNode> result = new ArrayList<>(Arrays.asList(paths));

            /*
             * Here we sort the list of objects to minimize the risk that
             * objects will be pasted incorrectly. For instance, ObjEntity
             * should go before Query, to increase chances that Query's root
             * would be set.
             */
            result.sort(parent instanceof DataMap
                    ? Comparators.forDataMapChildren()
                    : Comparators.forDataDomainChildren());

            return result;
        }

        return null;
    }

    public void projectOpened(Project project) {

        this.project = project;

        this.navigationHistory = new ProjectNavigationHistory();
        this.state = new ControllerState();
        this.listeners = new EventListenerList();

        if (fileChangeTracker != null) {
            fileChangeTracker.interrupt();
        }

        fileChangeTracker = new ProjectFileChangeTracker(this);
        fileChangeTracker.setDaemon(true);
        fileChangeTracker.start();
        fileChangeTracker.reset();

        this.entityResolver = new EntityResolver();
        updateEntityResolver();

        GlobalActions globalActions = application.getActionManager();

        addDataNodeDisplayListener(e -> globalActions.dataNodeSelected());
        addDataMapDisplayListener(e -> globalActions.dataMapSelected());
        addObjEntityDisplayListener(e -> globalActions.objEntitySelected());
        addDbEntityDisplayListener(e -> globalActions.dbEntitySelected());
        addQueryDisplayListener(e -> globalActions.querySelected());
        addProcedureDisplayListener(e -> globalActions.procedureSelected());
        addMultipleObjectsDisplayListener(e -> globalActions.objectsSelected(e.getNodes()));
        addEmbeddableDisplayListener(e -> globalActions.embeddableSelected());

        this.projectView = new ProjectView(this);
    }

    public void projectClosed() {

        setDirty(false);

        this.project = null;
        this.projectView = null;
        this.preferences = null;
        this.entityResolver = null;

        if (fileChangeTracker != null) {
            fileChangeTracker.interrupt();
            fileChangeTracker = null;
        }

        this.state = null;
        this.listeners = null;
        this.navigationHistory = null;
    }

    public void saveSelectionToPrefs() {
        if (project == null) {
            return;
        }
        getProjectStatePreferences().saveSelection(this);
    }

    public void restoreSelectionFromPrefs() {
        getProjectStatePreferences().restoreSelection(this);
    }

    public boolean isDirty() {
        return dirty;
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

    public void displayDomain(DomainDisplayEvent e) {
        boolean changed = e.getDomain() != state.dataDomain || (
                state.dataNode != null || state.dataMap != null || state.dbEntity != null || state.objEntity != null || state.procedure != null || state.query != null || state.embeddable != null
        );
        LOGGER.debug("displayDomain: {}{}", e.getDomain() != null ? e.getDomain().getName() : null, changed ? "" : ", ignored unchanged");

        if (changed) {
            state = new ControllerState();
            state.dataDomain = e.getDomain();
            navigationHistory.recordEvent(e);

            for (DomainDisplayListener listener : listeners.getListeners(DomainDisplayListener.class)) {
                listener.domainSelected(e);
            }

            if (e.getDomain() == null) {
                getApplication().getActionManager().projectOpened();
            } else {
                getApplication().getActionManager().domainSelected();
            }
        }
    }


    /**
     * Informs all listeners of the DomainEvent. Does not send the event to its
     * originator.
     */
    public void fireDomainEvent(DomainEvent e) {
        LOGGER.debug("fireDomainEvent: {}", e.getDomain().getName());
        setDirty(true);

        if (e.getType() == ModelEvent.Type.REMOVE) {
            navigationHistory.forgetObject(e);
        }

        for (DomainListener listener : listeners.getListeners(DomainListener.class)) {
            switch (e.getType()) {
                case CHANGE:
                    listener.domainChanged(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DomainEvent type: " + e.getType());
            }
        }
    }

    public void displayDataNode(DataNodeDisplayEvent e) {
        boolean changed = e.getDataNode() != state.dataNode || (
                state.dataMap != null || state.dbEntity != null || state.objEntity != null || state.procedure != null || state.query != null || state.embeddable != null
        );
        LOGGER.debug("displayDataNode: {}{}", e.getDataNode().getName(), changed ? "" : ", ignored unchanged");

        if (changed) {
            state = new ControllerState();
            state.dataDomain = e.getDomain();
            state.dataNode = e.getDataNode();
            navigationHistory.recordEvent(e);

            for (DataNodeDisplayListener listener : listeners.getListeners(DataNodeDisplayListener.class)) {
                listener.dataNodeSlected(e);
            }
        }
    }

    public void fireDataNodeEvent(DataNodeEvent e) {
        LOGGER.debug("fireDataNodeEvent: {}", e.getDataNode().getName());
        setDirty(true);

        if (e.getType() == ModelEvent.Type.REMOVE) {
            navigationHistory.forgetObject(e);
        }

        for (DataNodeListener listener : listeners.getListeners(DataNodeListener.class)) {
            switch (e.getType()) {
                case ADD:
                    listener.dataNodeAdded(e);
                    break;
                case CHANGE:
                    listener.dataNodeChanged(e);
                    break;
                case REMOVE:
                    listener.dataNodeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DataNodeEvent type: " + e.getType());
            }
        }
    }

    public void displayDataMap(DataMapDisplayEvent e) {
        boolean changed = e.getDataMap() != state.dataMap || (
                state.dbEntity != null || state.objEntity != null || state.procedure != null || state.query != null || state.embeddable != null
        );

        LOGGER.debug("displayDataMap: {}{}", e.getDataMap().getName(), changed ? "" : (e.isMainTabFocus() ? ", unchanged but main-tab-focus" : ", ignored unchanged"));
        if (changed) {
            state = new ControllerState();
            state.dataDomain = e.getDomain();
            state.dataNode = e.getDataNode();
            state.dataMap = e.getDataMap();

            navigationHistory.recordEvent(e);
        }

        // Always deliver events that explicitly request main tab focus (e.g. "Create DataMap"),
        // even when selection is unchanged — otherwise the tab switch would be silently skipped.
        if (changed || e.isMainTabFocus()) {
            for (DataMapDisplayListener listener : listeners.getListeners(DataMapDisplayListener.class)) {
                listener.dataMapSelected(e);
            }
        }
    }

    public void pauseFileChangeTracking() {
        fileChangeTracker.pauseTracking();
    }

    public void fireProjectSavedEvent(ProjectSavedEvent e) {

        fileChangeTracker.reset();

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

        if (e.getType() == ModelEvent.Type.REMOVE) {
            navigationHistory.forgetObject(e);
        }

        for (DataMapListener eventListener : listeners.getListeners(DataMapListener.class)) {
            switch (e.getType()) {
                case ADD:
                    eventListener.dataMapAdded(e);
                    break;
                case CHANGE:
                    eventListener.dataMapChanged(e);
                    break;
                case REMOVE:
                    eventListener.dataMapRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DataMapEvent type: " + e.getType());
            }
        }
    }

    public void fireObjEntityEvent(ObjEntityEvent e) {
        LOGGER.debug("fireObjEntityEvent: {}", e.getEntity().getName());
        setDirty(true);

        if (e.getType() == ModelEvent.Type.REMOVE) {
            navigationHistory.forgetObject(e);
        }

        for (ObjEntityListener listener : listeners.getListeners(ObjEntityListener.class)) {
            switch (e.getType()) {
                case ADD:
                    listener.objEntityAdded(e);
                    break;
                case CHANGE:
                    listener.objEntityChanged(e);
                    break;
                case REMOVE:
                    listener.objEntityRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ObjEntityEvent type: " + e.getType());
            }
        }
    }

    public void fireDbEntityEvent(DbEntityEvent e) {
        LOGGER.debug("fireDbEntityEvent: {}", e.getEntity().getName());
        setDirty(true);

        if (e.getType() == ModelEvent.Type.REMOVE) {
            navigationHistory.forgetObject(e);
        }

        for (DbEntityListener listener : listeners.getListeners(DbEntityListener.class)) {
            switch (e.getType()) {
                case ADD:
                    listener.dbEntityAdded(e);
                    break;
                case CHANGE:
                    listener.dbEntityChanged(e);
                    break;
                case REMOVE:
                    listener.dbEntityRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DbEntityEvent type: " + e.getType());
            }
        }
    }

    public void fireQueryEvent(QueryEvent e) {
        LOGGER.debug("fireQueryEvent: {}", e.getQuery().getName());
        setDirty(true);

        if (e.getType() == ModelEvent.Type.REMOVE) {
            navigationHistory.forgetObject(e);
        }

        for (QueryListener eventListener : listeners.getListeners(QueryListener.class)) {
            switch (e.getType()) {
                case ADD:
                    eventListener.queryAdded(e);
                    break;
                case CHANGE:
                    eventListener.queryChanged(e);
                    break;
                case REMOVE:
                    eventListener.queryRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ProcedureEvent type: " + e.getType());
            }
        }
    }

    public void fireProcedureEvent(ProcedureEvent e) {
        LOGGER.debug("fireProcedureEvent: {}", e.getProcedure().getName());
        setDirty(true);

        if (e.getType() == ModelEvent.Type.REMOVE) {
            navigationHistory.forgetObject(e);
        }

        for (ProcedureListener eventListener : listeners.getListeners(ProcedureListener.class)) {
            switch (e.getType()) {
                case ADD:
                    eventListener.procedureAdded(e);
                    break;
                case CHANGE:
                    eventListener.procedureChanged(e);
                    break;
                case REMOVE:
                    eventListener.procedureRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ProcedureEvent type: " + e.getType());
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
            switch (e.getType()) {
                case ADD:
                    listener.procedureParameterAdded(e);
                    break;
                case CHANGE:
                    listener.procedureParameterChanged(e);
                    break;
                case REMOVE:
                    listener.procedureParameterRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ProcedureParameterEvent type: " + e.getType());
            }
        }
    }


    public void rewindBackwards() {
        navigationHistory.replayLastEvent(this);
    }

    public void rewindForward() {
        navigationHistory.replayNextEvent(this);
    }

    public void displayObjEntity(EntityDisplayEvent e) {
        boolean changed = e.getEntity() != state.objEntity;

        LOGGER.debug("displayObjEntity: {}{}", e.getEntity().getName(), changed ? "" : (e.isMainTabFocus() ? ", unchanged but main-tab-focus" : ", ignored unchanged"));

        if (changed) {
            state = new ControllerState();
            state.dataDomain = e.getDomain();
            state.dataNode = e.getDataNode();
            state.dataMap = e.getDataMap();
            state.objEntity = (ObjEntity) e.getEntity();

            navigationHistory.recordEvent(e);
        }

        // Always deliver events that explicitly request main tab focus (e.g. "Create ObjEntity"),
        // even when selection is unchanged — otherwise the tab switch would be silently skipped.
        if (changed || e.isMainTabFocus()) {
            for (ObjEntityDisplayListener l : listeners.getListeners(ObjEntityDisplayListener.class)) {
                l.objEntitySelected(e);
            }
        }
    }

    public void displayEmbeddable(EmbeddableDisplayEvent e) {
        boolean changed = e.getEmbeddable() != state.embeddable;
        LOGGER.debug("displayEmbeddable: {}{}", e.getEmbeddable().getClassName(), changed ? "" : (e.isMainTabFocus() ? ", unchanged but main-tab-focus" : ", ignored unchanged"));

        if (changed) {
            state = new ControllerState();
            state.dataDomain = e.getDomain();
            state.dataNode = e.getDataNode();
            state.dataMap = e.getDataMap();
            state.embeddable = e.getEmbeddable();
            navigationHistory.recordEvent(e);
        }

        // Always deliver events that explicitly request main tab focus (e.g. "Create Embeddable"),
        // even when selection is unchanged — otherwise the tab switch would be silently skipped.
        if (changed || e.isMainTabFocus()) {
            for (EmbeddableDisplayListener l : listeners.getListeners(EmbeddableDisplayListener.class)) {
                l.embeddableSelected(e);
            }
        }
    }

    public void displayQuery(QueryDisplayEvent e) {
        boolean changed = e.getQuery() != state.query;
        LOGGER.debug("displayQuery: {}{}", e.getQuery().getName(), changed ? "" : ", ignored unchanged");

        if (changed) {
            state = new ControllerState();
            state.dataDomain = e.getDomain();
            state.dataMap = e.getDataMap();
            state.query = e.getQuery();
            navigationHistory.recordEvent(e);

            for (QueryDisplayListener l : listeners.getListeners(QueryDisplayListener.class)) {
                l.querySelected(e);
            }
        }
    }

    public void displayProcedure(ProcedureDisplayEvent e) {
        boolean changed = e.getProcedure() != state.procedure;
        LOGGER.debug("displayProcedure: {}{}", e.getProcedure().getName(), changed ? "" : (e.isTabReset() ? ", unchanged but tab-reset" : ", ignored unchanged"));

        if (changed) {
            state = new ControllerState();
            state.dataDomain = e.getDomain();
            state.dataMap = e.getDataMap();
            state.procedure = e.getProcedure();
            navigationHistory.recordEvent(e);
        }

        // Always deliver events that explicitly request a tab reset (e.g. validator error click),
        // even when selection is unchanged — otherwise the tab switch would be silently skipped.
        if (changed || e.isTabReset()) {
            for (ProcedureDisplayListener l : listeners.getListeners(ProcedureDisplayListener.class)) {
                l.procedureSelected(e);
            }
        }
    }

    public void displayProcedureParameter(ProcedureParameterDisplayEvent e) {
        boolean changed = !Arrays.equals(e.getProcedureParameters(), state.procedureParameters);
        LOGGER.debug("displayProcedureParameter: {}{}", e.getProcedure().getName(), changed ? "" : ", ignored unchanged");

        if (changed) {
            if (state.procedure != e.getProcedure()) {
                state = new ControllerState();
                state.dataDomain = e.getDomain();
                state.dataMap = e.getDataMap();
                state.procedure = e.getProcedure();
            }
            state.procedureParameters = e.getProcedureParameters();

            for (ProcedureParameterDisplayListener l : listeners.getListeners(ProcedureParameterDisplayListener.class)) {
                l.procedureParameterSelected(e);
            }
        }
    }

    public void displayDbEntity(EntityDisplayEvent e) {
        boolean changed = e.getEntity() != state.dbEntity;
        LOGGER.debug("displayDbEntity: {}{}", e.getEntity().getName(), changed ? "" : (e.isMainTabFocus() ? ", unchanged but main-tab-focus" : ", ignored unchanged"));

        if (changed) {
            state = new ControllerState();
            state.dataDomain = e.getDomain();
            state.dataNode = e.getDataNode();
            state.dataMap = e.getDataMap();
            state.dbEntity = (DbEntity) e.getEntity();
            navigationHistory.recordEvent(e);
        }

        // Always deliver events that explicitly request main tab focus (e.g. "Create DbEntity"),
        // even when selection is unchanged — otherwise the tab switch would be silently skipped.
        if (changed || e.isMainTabFocus()) {
            for (DbEntityDisplayListener l : listeners.getListeners(DbEntityDisplayListener.class)) {
                l.dbEntitySelected(e);
            }
        }
    }

    /**
     * Notifies all listeners of the change(add, remove) and does the change.
     */
    public void fireDbAttributeEvent(DbAttributeEvent e) {
        LOGGER.debug("fireDbAttributeEvent: {}", e.getAttribute().getName());
        setDirty(true);

        for (DbAttributeListener l : listeners.getListeners(DbAttributeListener.class)) {
            switch (e.getType()) {
                case ADD:
                    l.dbAttributeAdded(e);
                    break;
                case CHANGE:
                    l.dbAttributeChanged(e);
                    break;
                case REMOVE:
                    l.dbAttributeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DbAttributeEvent type: " + e.getType());
            }
        }
    }

    public void displayDbAttribute(AttributeDisplayEvent e) {
        boolean changed = !Arrays.equals(e.getAttributes(), state.dbAttributes);
        LOGGER.debug("displayDbAttribute: {}{}", Arrays.stream(e.getAttributes()).map(Attribute::getName).toArray(), changed ? "" : ", ignored unchanged");

        if (changed) {
            if (e.getEntity() != state.dbEntity) {
                state = new ControllerState();
                state.dataDomain = e.getDomain();
                state.dataMap = e.getDataMap();
                state.dbEntity = (DbEntity) e.getEntity();
            }
            state.dbAttributes = new DbAttribute[e.getAttributes().length];
            System.arraycopy(e.getAttributes(), 0, state.dbAttributes, 0, state.dbAttributes.length);

            for (DbAttributeDisplayListener l : listeners.getListeners(DbAttributeDisplayListener.class)) {
                l.dbAttributeSelected(e);
            }
        }
    }

    /**
     * Notifies all listeners of the change (add, remove) and does the change.
     */
    public void fireObjAttributeEvent(ObjAttributeEvent e) {
        LOGGER.debug("fireObjAttributeEvent: {}", e.getAttribute().getName());
        setDirty(true);

        for (ObjAttributeListener listener : listeners.getListeners(ObjAttributeListener.class)) {
            switch (e.getType()) {
                case ADD:
                    listener.objAttributeAdded(e);
                    break;
                case CHANGE:
                    listener.objAttributeChanged(e);
                    break;
                case REMOVE:
                    listener.objAttributeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ObjAttributeEvent type: " + e.getType());
            }
        }
    }

    public void displayObjAttribute(AttributeDisplayEvent e) {
        boolean changed = !Arrays.equals(e.getAttributes(), state.objAttributes);
        LOGGER.debug("displayObjAttribute: {}{}", Arrays.stream(e.getAttributes()).map(Attribute::getName).toArray(), changed ? "" : ", ignored unchanged");

        if (changed) {
            if (e.getEntity() != state.objEntity) {
                state = new ControllerState();
                state.dataDomain = e.getDomain();
                state.dataMap = e.getDataMap();
                state.objEntity = (ObjEntity) e.getEntity();
            }
            state.objAttributes = new ObjAttribute[e.getAttributes().length];
            System.arraycopy(e.getAttributes(), 0, state.objAttributes, 0, state.objAttributes.length);

            for (ObjAttributeDisplayListener l : listeners.getListeners(ObjAttributeDisplayListener.class)) {
                l.objAttributeSelected(e);
            }
        }
    }

    public void displayEmbeddableAttribute(EmbeddableAttributeDisplayEvent ev) {
        boolean changed = !Arrays.equals(ev.getEmbeddableAttributes(), state.embeddableAttributes);
        LOGGER.debug("displayEmbeddableAttribute: {}{}", Arrays.stream(ev.getEmbeddableAttributes()).map(EmbeddableAttribute::getName).toArray(), changed ? "" : ", ignored unchanged");

        if (changed) {
            if (ev.getEmbeddable() != state.embeddable) {
                state = new ControllerState();
                state.dataDomain = ev.getDomain();
                state.dataMap = ev.getDataMap();
                state.embeddable = ev.getEmbeddable();
            }
            state.embeddableAttributes = new EmbeddableAttribute[ev.getEmbeddableAttributes().length];
            System.arraycopy(ev.getEmbeddableAttributes(), 0, state.embeddableAttributes, 0, state.embeddableAttributes.length);

            for (EmbeddableAttributeDisplayListener l : listeners.getListeners(EmbeddableAttributeDisplayListener.class)) {
                l.embeddableAttributeSelected(ev);
            }
        }
    }

    /**
     * Notifies all listeners of the change(add, remove) and does the change.
     */
    public void fireDbRelationshipEvent(DbRelationshipEvent e) {
        LOGGER.debug("fireDbRelationshipEvent: {}", e.getRelationship().getName());
        setDirty(true);

        for (DbRelationshipListener listener : listeners.getListeners(DbRelationshipListener.class)) {
            switch (e.getType()) {
                case ADD:
                    listener.dbRelationshipAdded(e);
                    break;
                case CHANGE:
                    listener.dbRelationshipChanged(e);
                    break;
                case REMOVE:
                    listener.dbRelationshipRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DbRelationshipEvent type: " + e.getType());
            }
        }
    }

    public void displayDbRelationship(RelationshipDisplayEvent e) {
        boolean changed = !Arrays.equals(e.getRelationships(), state.dbRelationships);
        LOGGER.debug("displayDbRelationship: {}{}", Arrays.stream(e.getRelationships()).map(Relationship::getName).toArray(), changed ? "" : ", ignored unchanged");

        if (changed) {
            if (e.getEntity() != state.dbEntity) {
                state = new ControllerState();
                state.dataDomain = e.getDomain();
                state.dataMap = e.getDataMap();
                state.dbEntity = (DbEntity) e.getEntity();
            }
            state.dbRelationships = new DbRelationship[e.getRelationships().length];
            System.arraycopy(e.getRelationships(), 0, state.dbRelationships, 0, state.dbRelationships.length);

            for (DbRelationshipDisplayListener listener : listeners.getListeners(DbRelationshipDisplayListener.class)) {
                listener.dbRelationshipSelected(e);
            }
        }
    }

    /**
     * Notifies all listeners of the change(add, remove) and does the change.
     */
    public void fireObjRelationshipEvent(ObjRelationshipEvent e) {
        LOGGER.debug("fireObjRelationshipEvent: {}", e.getRelationship().getName());
        setDirty(true);

        for (ObjRelationshipListener listener : listeners.getListeners(ObjRelationshipListener.class)) {
            switch (e.getType()) {
                case ADD:
                    listener.objRelationshipAdded(e);
                    break;
                case CHANGE:
                    listener.objRelationshipChanged(e);
                    break;
                case REMOVE:
                    listener.objRelationshipRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ObjRelationshipEvent type: " + e.getType());
            }
        }
    }

    public void displayMultipleObjects(MultipleObjectsDisplayEvent e) {
        LOGGER.debug("displayMultipleObjects");
        state = new ControllerState();
        state.paths = e.getNodes();
        state.parentPath = e.getParentNode();

        for (MultipleObjectsDisplayListener l : listeners.getListeners(MultipleObjectsDisplayListener.class)) {
            l.multipleObjectsSelected(e);
        }
    }

    public void displayObjRelationship(RelationshipDisplayEvent e) {
        boolean changed = !Arrays.equals(e.getRelationships(), state.objRelationships);
        LOGGER.debug("displayObjRelationship: {}{}", Arrays.stream(e.getRelationships()).map(Relationship::getName).toArray(), changed ? "" : ", ignored unchanged");

        if (changed) {
            if (e.getEntity() != state.objEntity) {
                state = new ControllerState();
                state.dataDomain = e.getDomain();
                state.dataMap = e.getDataMap();
                state.objEntity = (ObjEntity) e.getEntity();
            }
            state.objRelationships = new ObjRelationship[e.getRelationships().length];
            System.arraycopy(e.getRelationships(), 0, state.objRelationships, 0, state.objRelationships.length);

            for (ObjRelationshipDisplayListener l : listeners.getListeners(ObjRelationshipDisplayListener.class)) {
                l.currentObjRelationshipChanged(e);
            }
        }
    }

    public void displayValidationConfig(ValidationConfigDisplayEvent event) {
        LOGGER.debug("displayValidationConfig");
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
                parent.onProjectModified();
            } else {
                this.preferences = buildPreferences();
            }
        }
    }

    public CallbackType getSelectedCallbackType() {
        return state.callbackType;
    }

    public void addCallbackTypeSelectionListener(CallbackTypeSelectionListener listener) {
        listeners.add(CallbackTypeSelectionListener.class, listener);
    }

    public void displayCallbackType(CallbackTypeDisplayEvent e) {
        LOGGER.debug("displayCallbackType: {}", e.getCallbackType());
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

    public void displayCallbackMethod(CallbackMethodDisplayEvent e) {
        LOGGER.debug("displayCallbackMethod");
        state.callbackMethods = e.getCallbackMethods();
        for (CallbackMethodDisplayListener l : listeners.getListeners(CallbackMethodDisplayListener.class)) {
            l.callbackMethodSelected(e);
        }
    }

    public void addCallbackMethodListener(CallbackMethodListener listener) {
        listeners.add(CallbackMethodListener.class, listener);
    }

    public void fireCallbackMethodEvent(CallbackMethodEvent e) {
        LOGGER.debug("fireCallbackMethodEvent: {}", e.getNewName());
        setDirty(true);

        for (CallbackMethodListener listener : listeners.getListeners(CallbackMethodListener.class)) {
            switch (e.getType()) {
                case ADD:
                    listener.callbackMethodAdded(e);
                    break;
                case CHANGE:
                    listener.callbackMethodChanged(e);
                    break;
                case REMOVE:
                    listener.callbackMethodRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid CallbackEvent type: " + e.getType());
            }
        }
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

            switch (e.getType()) {
                case ADD:
                    listener.embeddableAdded(e, map);
                    break;
                case CHANGE:
                    listener.embeddableChanged(e, map);
                    break;
                case REMOVE:
                    listener.embeddableRemoved(e, map);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid RelationshipEvent type: " + e.getType());
            }
        }
    }

    public void fireEmbeddableAttributeEvent(EmbeddableAttributeEvent e) {
        LOGGER.debug("fireEmbeddableAttributeEvent: {}", e.getEmbeddableAttribute().getName());
        setDirty(true);
        for (EmbeddableAttributeListener listener : listeners.getListeners(EmbeddableAttributeListener.class)) {

            switch (e.getType()) {
                case ADD:
                    listener.embeddableAttributeAdded(e);
                    break;
                case CHANGE:
                    listener.embeddableAttributeChanged(e);
                    break;
                case REMOVE:
                    listener.embeddableAttributeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid RelationshipEvent type: " + e.getType());
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
            switch (e.getType()) {
                case ADD:
                    listener.callbackDataSourceAdded(e);
                    break;
                // Change event not supported for now
                // There is no good place to catch data source modification
                /*case MapEvent.CHANGE:
                    listener.callbackDataSourceChanged(e);
                    break;*/
                case REMOVE:
                    listener.callbackDataSourceRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid RelationshipEvent type: " + e.getType());
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

    private Preferences buildPreferences() {

        Preferences root = Preferences.userNodeForPackage(Project.class);
        if (project == null) {
            return root;
        }

        String resourcePath = project.getConfigurationResource() == null
                ? null
                : project.getConfigurationResource().getURL().getPath();

        if (resourcePath == null || resourcePath.isEmpty() || !resourcePath.contains(".xml")) {
            return root.node(getApplication().getNewProjectTemporaryName());
        }

        return root.node(root.absolutePath() + resourcePath.replace(".xml", ""));
    }


    static class ControllerState {

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
    }
}
