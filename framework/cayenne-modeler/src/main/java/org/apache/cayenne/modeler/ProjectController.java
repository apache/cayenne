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

package org.apache.cayenne.modeler;

import java.awt.Component;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;

import javax.swing.event.EventListenerList;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.DataMapEvent;
import org.apache.cayenne.map.event.DataMapListener;
import org.apache.cayenne.map.event.DataNodeEvent;
import org.apache.cayenne.map.event.DataNodeListener;
import org.apache.cayenne.map.event.DbAttributeListener;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.DbRelationshipListener;
import org.apache.cayenne.map.event.DomainEvent;
import org.apache.cayenne.map.event.DomainListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.ObjAttributeListener;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.map.event.ObjRelationshipListener;
import org.apache.cayenne.map.event.ProcedureEvent;
import org.apache.cayenne.map.event.ProcedureListener;
import org.apache.cayenne.map.event.ProcedureParameterEvent;
import org.apache.cayenne.map.event.ProcedureParameterListener;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.map.event.QueryListener;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.action.NavigateBackwardAction;
import org.apache.cayenne.modeler.action.NavigateForwardAction;
import org.apache.cayenne.modeler.action.RevertAction;
import org.apache.cayenne.modeler.action.SaveAction;
import org.apache.cayenne.modeler.editor.CallbackType;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;
import org.apache.cayenne.modeler.event.CallbackMethodListener;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayListener;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayListener;
import org.apache.cayenne.modeler.event.DbAttributeDisplayListener;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.DbRelationshipDisplayListener;
import org.apache.cayenne.modeler.event.DisplayEvent;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.DomainDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.EntityListenerEvent;
import org.apache.cayenne.modeler.event.EntityListenerListener;
import org.apache.cayenne.modeler.event.ObjAttributeDisplayListener;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.ObjRelationshipDisplayListener;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayListener;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayListener;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayListener;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.pref.DataNodeDefaults;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.CircularArray;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.IDUtil;

/**
 * A controller that works with the project tree, tracking selection and dispatching
 * project events.
 * <p>
 * TODO: Refactor the event model, so that events are generic and contain "path" to a
 * project node in question. After this is done, EventController should no longer maintain
 * the selection model (currentXYZ ivars), rather it should update internal model.
 * </p>
 */
public class ProjectController extends CayenneController {

    /*
     * A snapshot of the current state of the project controller. This was added so that
     * we could support history of recent objects.
     */
    public class ControllerState {

        private boolean isRefiring;
        private DisplayEvent event;
        private DataDomain domain;
        private DataNode node;
        private DataMap map;
        private ObjEntity objEntity;
        private DbEntity dbEntity;
        private ObjAttribute objAttr;
        private DbAttribute dbAttr;
        private ObjRelationship objRel;
        private DbRelationship dbRel;
        private Procedure procedure;
        private ProcedureParameter procedureParameter;
        private Query query;

        /**
         * currently selecte entity listener class
         */
        private String listenerClass;
        /**
         * currently selected callback type
         */
        private CallbackType callbackType;
        /**
         * currently selected callback method
         */
        private String callbackMethod;

        public ControllerState() {
            domain = null;
            node = null;
            map = null;

            objEntity = null;
            objAttr = null;
            objRel = null;

            dbEntity = null;
            dbAttr = null;
            dbRel = null;

            procedure = null;
            procedureParameter = null;

            query = null;

            event = null;
            isRefiring = false;
        }

        /*
         * Used to determine if the val ControllerState is equivalent, which means if the
         * event is refired again, will it end up in the same place on the screen. This
         * get's a bit messy at the end, because of inheritance heirarchy issues.
         */
        public boolean isEquivalent(ControllerState val) {
            if (val == null)
                return false;

            if (event instanceof EntityDisplayEvent
                    && val.event instanceof EntityDisplayEvent) {
                if (((EntityDisplayEvent) val.event).getEntity() instanceof ObjEntity) {
                    return objEntity == val.objEntity;
                }
                else {
                    return dbEntity == val.dbEntity;
                }
            }
            else if (event instanceof ProcedureDisplayEvent
                    && val.event instanceof ProcedureDisplayEvent) {
                return procedure == val.procedure;
            }
            else if (event instanceof QueryDisplayEvent
                    && val.event instanceof QueryDisplayEvent) {
                return query == val.query;
            }
            else if (event.getClass() == DataMapDisplayEvent.class
                    && event.getClass() == val.event.getClass()) {
                return map == val.map;
            }
            else if (event.getClass() == DataNodeDisplayEvent.class
                    && event.getClass() == val.event.getClass()) {
                return node == val.node;
            }
            else if (event.getClass() == DomainDisplayEvent.class
                    && event.getClass() == val.event.getClass()) {
                return domain == val.domain;
            }
            return false;
        }
    }

    protected EventListenerList listenerList;
    protected boolean dirty;

    protected Project project;
    protected Domain projectPreferences;

    protected ControllerState currentState;
    protected CircularArray controllerStateHistory;
    protected int maxHistorySize = 20;

    public ProjectController(CayenneModelerController parent) {
        super(parent);
        this.listenerList = new EventListenerList();
        controllerStateHistory = new CircularArray(maxHistorySize);
        currentState = new ControllerState();
    }

    public Component getView() {
        return parent.getView();
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project currentProject) {
        this.project = currentProject;
        this.projectPreferences = null;
    }

    /**
     * Returns top preferences Domain for the application.
     */
    public Domain getApplicationPreferenceDomain() {
        return getApplication().getPreferenceDomain();
    }

    /**
     * Returns top preferences Domain for the current project, throwing an exception if no
     * project is selected.
     */
    public Domain getPreferenceDomainForProject() {
        if (getProject() == null) {
            throw new CayenneRuntimeException("No Project selected");
        }

        if (projectPreferences == null) {
            String key = getProject().isLocationUndefined() ? new String(IDUtil
                    .pseudoUniqueByteSequence16()) : project
                    .getMainFile()
                    .getAbsolutePath();

            projectPreferences = getApplicationPreferenceDomain().getSubdomain(
                    Project.class).getSubdomain(key);
        }

        return projectPreferences;
    }

    /**
     * Returns top preferences Domain for the current project, throwing an exception if no
     * project is selected.
     */
    public Domain getPreferenceDomainForDataDomain() {
        DataDomain dataDomain = getCurrentDataDomain();
        if (dataDomain == null) {
            throw new CayenneRuntimeException("No DataDomain selected");
        }

        return getPreferenceDomainForProject()
                .getSubdomain(DataDomain.class)
                .getSubdomain(dataDomain.getName());
    }

    /**
     * Returns preferences object for the current DataMap. If no preferences exist for the
     * current DataMap, a new Preferences object is created. If no DataMap is currently
     * selected, an exception is thrown. An optional nameSuffix allows to address more
     * than one defaults instance for a single DataMap.
     */
    public DataMapDefaults getDataMapPreferences(String nameSuffix) {
        DataMap map = getCurrentDataMap();
        if (map == null) {
            throw new CayenneRuntimeException("No DataMap selected");
        }

        if (nameSuffix == null) {
            nameSuffix = "";
        }

        return (DataMapDefaults) getPreferenceDomainForDataDomain().getDetail(
                map.getName() + nameSuffix,
                DataMapDefaults.class,
                true);
    }

    /**
     * Returns preferences object for the current DataMap, throwing an exception if no
     * DataMap is selected.
     */
    public DataNodeDefaults getDataNodePreferences() {
        DataNode node = getCurrentDataNode();
        if (node == null) {
            throw new CayenneRuntimeException("No DataNode selected");
        }

        return (DataNodeDefaults) getPreferenceDomainForDataDomain().getDetail(
                node.getName(),
                DataNodeDefaults.class,
                true);
    }

    public void projectOpened() {
        CayenneModelerFrame frame = (CayenneModelerFrame) getView();
        addDataNodeDisplayListener(frame);
        addDataMapDisplayListener(frame);
        addObjEntityDisplayListener(frame);
        addDbEntityDisplayListener(frame);
        addQueryDisplayListener(frame);
        addProcedureDisplayListener(frame);
    }

    public void reset() {
        clearState();
        setDirty(false);
        listenerList = new EventListenerList();
        controllerStateHistory.clear();
    }

    /*
     * Allow the user to change the default history size. TODO When a user changes their
     * preferences it should call this method. I don't know how the preferences work, so I
     * will leave this to someone else to do. Garry
     */
    public void setHistorySize(int newSize) {
        controllerStateHistory.resize(newSize);
    }

    public boolean isDirty() {
        return dirty;
    }

    /** Resets all current models to null. */
    private void clearState() {
        // don't clear if we are refiring events for history navigation
        if (currentState.isRefiring)
            return;

        currentState = new ControllerState();
    }

    private void saveState(DisplayEvent e) {
        if (!controllerStateHistory.contains(currentState)) {
            currentState.event = e;
            controllerStateHistory.add(currentState);
        }
    }

    protected void refreshNamespace() {
        DataDomain domain = getCurrentDataDomain();
        if (domain != null) {
            domain.getEntityResolver().clearCache();
        }
    }

    private void removeFromHistory(EventObject e) {

        synchronized (controllerStateHistory) {
            int count = controllerStateHistory.size();
            ArrayList removeList = new ArrayList();

            for (int i = 0; i < count; i++) {
                ControllerState cs = (ControllerState) controllerStateHistory.get(i);

                EventObject csEvent = cs.event;

                if (csEvent == null) {
                    continue;
                }

                if (e instanceof EntityEvent && csEvent instanceof EntityDisplayEvent) {
                    if (((EntityEvent) e).getEntity() == ((EntityDisplayEvent) csEvent)
                            .getEntity()) {
                        removeList.add(cs);
                    }
                }
                else if (e instanceof ProcedureEvent
                        && csEvent instanceof ProcedureDisplayEvent) {
                    if (((ProcedureEvent) e).getProcedure() == ((ProcedureDisplayEvent) csEvent)
                            .getProcedure()) {
                        removeList.add(cs);
                    }
                }
                else if (e instanceof QueryEvent && csEvent instanceof QueryDisplayEvent) {
                    if (((QueryEvent) e).getQuery() == ((QueryDisplayEvent) csEvent)
                            .getQuery()) {
                        removeList.add(cs);
                    }
                }
                else if (e instanceof DataMapEvent
                        && csEvent instanceof DataMapDisplayEvent) {
                    if (((DataMapEvent) e).getDataMap() == ((DataMapDisplayEvent) csEvent)
                            .getDataMap()) {
                        removeList.add(cs);
                    }
                }
                else if (e instanceof DataNodeEvent
                        && csEvent instanceof DataNodeDisplayEvent) {
                    if (((DataNodeEvent) e).getDataNode() == ((DataNodeDisplayEvent) csEvent)
                            .getDataNode()) {
                        removeList.add(cs);
                    }
                }
                else if (e instanceof DomainEvent
                        && csEvent instanceof DomainDisplayEvent) {
                    if (((DomainEvent) e).getDomain() == ((DomainDisplayEvent) csEvent)
                            .getDomain()) {
                        removeList.add(cs);
                    }
                }
            }
            Iterator it = removeList.iterator();
            while (it.hasNext()) {
                controllerStateHistory.remove(it.next());
            }

        }
    }

    public DataNode getCurrentDataNode() {
        return currentState.node;
    }

    public DataDomain getCurrentDataDomain() {
        return currentState.domain;
    }

    public DataMap getCurrentDataMap() {
        return currentState.map;
    }

    public ObjEntity getCurrentObjEntity() {
        return currentState.objEntity;
    }

    public DbEntity getCurrentDbEntity() {
        return currentState.dbEntity;
    }

    public ObjAttribute getCurrentObjAttribute() {
        return currentState.objAttr;
    }

    public DbAttribute getCurrentDbAttribute() {
        return currentState.dbAttr;
    }

    public ObjRelationship getCurrentObjRelationship() {
        return currentState.objRel;
    }

    public DbRelationship getCurrentDbRelationship() {
        return currentState.dbRel;
    }

    public Query getCurrentQuery() {
        return currentState.query;
    }

    public Procedure getCurrentProcedure() {
        return currentState.procedure;
    }

    public ProcedureParameter getCurrentProcedureParameter() {
        return currentState.procedureParameter;
    }

    public void addDomainDisplayListener(DomainDisplayListener listener) {
        listenerList.add(DomainDisplayListener.class, listener);
    }

    public void addDomainListener(DomainListener listener) {
        listenerList.add(DomainListener.class, listener);
    }

    public void addDataNodeDisplayListener(DataNodeDisplayListener listener) {
        listenerList.add(DataNodeDisplayListener.class, listener);
    }

    public void addDataNodeListener(DataNodeListener listener) {
        listenerList.add(DataNodeListener.class, listener);
    }

    public void addDataMapDisplayListener(DataMapDisplayListener listener) {
        listenerList.add(DataMapDisplayListener.class, listener);
    }

    public void addDataMapListener(DataMapListener listener) {
        listenerList.add(DataMapListener.class, listener);
    }

    public void addDbEntityListener(DbEntityListener listener) {
        listenerList.add(DbEntityListener.class, listener);
    }

    public void addObjEntityListener(ObjEntityListener listener) {
        listenerList.add(ObjEntityListener.class, listener);
    }

    public void addDbEntityDisplayListener(DbEntityDisplayListener listener) {
        listenerList.add(DbEntityDisplayListener.class, listener);
    }

    public void addObjEntityDisplayListener(ObjEntityDisplayListener listener) {
        listenerList.add(ObjEntityDisplayListener.class, listener);
    }

    public void addDbAttributeListener(DbAttributeListener listener) {
        listenerList.add(DbAttributeListener.class, listener);
    }

    public void addDbAttributeDisplayListener(DbAttributeDisplayListener listener) {
        listenerList.add(DbAttributeDisplayListener.class, listener);
    }

    public void addObjAttributeListener(ObjAttributeListener listener) {
        listenerList.add(ObjAttributeListener.class, listener);
    }

    public void addObjAttributeDisplayListener(ObjAttributeDisplayListener listener) {
        listenerList.add(ObjAttributeDisplayListener.class, listener);
    }

    public void addDbRelationshipListener(DbRelationshipListener listener) {
        listenerList.add(DbRelationshipListener.class, listener);
    }

    public void addDbRelationshipDisplayListener(DbRelationshipDisplayListener listener) {
        listenerList.add(DbRelationshipDisplayListener.class, listener);
    }

    public void addObjRelationshipListener(ObjRelationshipListener listener) {
        listenerList.add(ObjRelationshipListener.class, listener);
    }

    public void addObjRelationshipDisplayListener(ObjRelationshipDisplayListener listener) {
        listenerList.add(ObjRelationshipDisplayListener.class, listener);
    }

    public void addQueryDisplayListener(QueryDisplayListener listener) {
        listenerList.add(QueryDisplayListener.class, listener);
    }

    public void addQueryListener(QueryListener listener) {
        listenerList.add(QueryListener.class, listener);
    }

    public void addProcedureDisplayListener(ProcedureDisplayListener listener) {
        listenerList.add(ProcedureDisplayListener.class, listener);
    }

    public void addProcedureListener(ProcedureListener listener) {
        listenerList.add(ProcedureListener.class, listener);
    }

    public void addProcedureParameterListener(ProcedureParameterListener listener) {
        listenerList.add(ProcedureParameterListener.class, listener);
    }

    public void addProcedureParameterDisplayListener(
            ProcedureParameterDisplayListener listener) {
        listenerList.add(ProcedureParameterDisplayListener.class, listener);
    }

    public void fireDomainDisplayEvent(DomainDisplayEvent e) {
        boolean changed = e.getDomain() != currentState.domain;
        if (!changed) {
            changed = currentState.node != null
                    || currentState.map != null
                    || currentState.dbEntity != null
                    || currentState.objEntity != null
                    || currentState.procedure != null
                    || currentState.query != null;
        }

        if (!e.isRefired()) {
            e.setDomainChanged(changed);
            if (changed) {
                clearState();
                currentState.domain = e.getDomain();
            }
        }

        if (changed) {
            saveState(e);
        }

        EventListener[] list = listenerList.getListeners(DomainDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            DomainDisplayListener temp = (DomainDisplayListener) list[i];
            temp.currentDomainChanged(e);
        }

        // call different methods depending on whether domain was opened or closed
        if (e.getDomain() == null) {
            getApplication().getActionManager().projectOpened();
        }
        else {
            getApplication().getActionManager().domainSelected();
        }
    }

    /**
     * Informs all listeners of the DomainEvent. Does not send the event to its
     * originator.
     */
    public void fireDomainEvent(DomainEvent e) {
        setDirty(true);

        if (e.getId() == MapEvent.REMOVE) {
            refreshNamespace();
            removeFromHistory(e);
        }

        EventListener[] list = listenerList.getListeners(DomainListener.class);
        for (int i = 0; i < list.length; i++) {
            DomainListener temp = (DomainListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    temp.domainAdded(e);
                    break;
                case MapEvent.CHANGE:
                    temp.domainChanged(e);
                    break;
                case MapEvent.REMOVE:
                    temp.domainRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DomainEvent type: "
                            + e.getId());
            }
        }
    }

    public void fireDataNodeDisplayEvent(DataNodeDisplayEvent e) {
        boolean changed = e.getDataNode() != currentState.node;

        if (!changed) {
            changed = currentState.map != null
                    || currentState.dbEntity != null
                    || currentState.objEntity != null
                    || currentState.procedure != null
                    || currentState.query != null;
        }

        if (!e.isRefired()) {
            e.setDataNodeChanged(changed);

            if (changed) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.node = e.getDataNode();
            }
        }

        if (changed) {
            saveState(e);
        }

        EventListener[] list = listenerList.getListeners(DataNodeDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            ((DataNodeDisplayListener) list[i]).currentDataNodeChanged(e);
        }
    }

    /**
     * Informs all listeners of the DataNodeEvent. Does not send the event to its
     * originator.
     */
    public void fireDataNodeEvent(DataNodeEvent e) {
        setDirty(true);

        if (e.getId() == MapEvent.REMOVE) {
            refreshNamespace();
            removeFromHistory(e);
        }

        EventListener[] list = listenerList.getListeners(DataNodeListener.class);
        for (int i = 0; i < list.length; i++) {
            DataNodeListener temp = (DataNodeListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    temp.dataNodeAdded(e);
                    break;
                case MapEvent.CHANGE:
                    temp.dataNodeChanged(e);
                    break;
                case MapEvent.REMOVE:
                    temp.dataNodeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DataNodeEvent type: "
                            + e.getId());
            }
        }
    }

    public void fireDataMapDisplayEvent(DataMapDisplayEvent e) {
        boolean changed = e.getDataMap() != currentState.map;
        if (!changed) {
            changed = currentState.dbEntity != null
                    || currentState.objEntity != null
                    || currentState.procedure != null
                    || currentState.query != null;
        }

        if (!e.isRefired()) {
            e.setDataMapChanged(changed);

            if (changed) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.node = e.getDataNode();
                currentState.map = e.getDataMap();
            }
        }

        if (changed) {
            saveState(e);
        }

        EventListener[] list = listenerList.getListeners(DataMapDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            DataMapDisplayListener temp = (DataMapDisplayListener) list[i];
            temp.currentDataMapChanged(e);
        }
    }

    /**
     * Informs all listeners of the DataMapEvent. Does not send the event to its
     * originator.
     */
    public void fireDataMapEvent(DataMapEvent e) {
        setDirty(true);

        if (e.getId() == MapEvent.REMOVE) {
            refreshNamespace();
            removeFromHistory(e);
        }

        EventListener[] list = listenerList.getListeners(DataMapListener.class);
        for (int i = 0; i < list.length; i++) {
            DataMapListener listener = (DataMapListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.dataMapAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.dataMapChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.dataMapRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid DataMapEvent type: "
                            + e.getId());
            }
        }
    }

    /**
     * Informs all listeners of the EntityEvent. Does not send the event to its
     * originator.
     */
    public void fireObjEntityEvent(EntityEvent e) {
        setDirty(true);

        if (currentState.map != null && e.getId() == MapEvent.CHANGE) {
            currentState.map.objEntityChanged(e);
        }

        if (e.getId() == MapEvent.REMOVE) {
            refreshNamespace();
            removeFromHistory(e);
        }

        EventListener[] list = listenerList.getListeners(ObjEntityListener.class);
        for (int i = 0; i < list.length; i++) {
            ObjEntityListener temp = (ObjEntityListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    temp.objEntityAdded(e);
                    break;
                case MapEvent.CHANGE:
                    temp.objEntityChanged(e);
                    break;
                case MapEvent.REMOVE:
                    temp.objEntityRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid EntityEvent type: "
                            + e.getId());
            }
        }
    }

    /**
     * Informs all listeners of the EntityEvent. Does not send the event to its
     * originator.
     */
    public void fireDbEntityEvent(EntityEvent e) {
        setDirty(true);

        if (currentState.map != null && e.getId() == MapEvent.CHANGE) {
            currentState.map.dbEntityChanged(e);
        }

        if (e.getId() == MapEvent.REMOVE) {
            refreshNamespace();
            removeFromHistory(e);
        }

        EventListener[] list = listenerList.getListeners(DbEntityListener.class);
        for (int i = 0; i < list.length; i++) {
            DbEntityListener temp = (DbEntityListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    temp.dbEntityAdded(e);
                    break;
                case MapEvent.CHANGE:
                    temp.dbEntityChanged(e);
                    break;
                case MapEvent.REMOVE:
                    temp.dbEntityRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid EntityEvent type: "
                            + e.getId());
            }
        }
    }

    /**
     * Informs all listeners of the ProcedureEvent. Does not send the event to its
     * originator.
     */
    public void fireQueryEvent(QueryEvent e) {
        setDirty(true);

        if (e.getId() == MapEvent.REMOVE) {
            refreshNamespace();
            removeFromHistory(e);
        }

        EventListener[] list = listenerList.getListeners(QueryListener.class);
        for (int i = 0; i < list.length; i++) {
            QueryListener listener = (QueryListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.queryAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.queryChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.queryRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ProcedureEvent type: "
                            + e.getId());
            }
        }
    }

    /**
     * Informs all listeners of the ProcedureEvent. Does not send the event to its
     * originator.
     */
    public void fireProcedureEvent(ProcedureEvent e) {
        setDirty(true);

        if (e.getId() == MapEvent.REMOVE) {
            refreshNamespace();
            removeFromHistory(e);
        }

        EventListener[] list = listenerList.getListeners(ProcedureListener.class);
        for (int i = 0; i < list.length; i++) {
            ProcedureListener listener = (ProcedureListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    listener.procedureAdded(e);
                    break;
                case MapEvent.CHANGE:
                    listener.procedureChanged(e);
                    break;
                case MapEvent.REMOVE:
                    listener.procedureRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid ProcedureEvent type: "
                            + e.getId());
            }
        }
    }

    /**
     * Informs all listeners of the ProcedureEvent. Does not send the event to its
     * originator.
     */
    public void fireProcedureParameterEvent(ProcedureParameterEvent e) {
        setDirty(true);

        EventListener[] list = listenerList
                .getListeners(ProcedureParameterListener.class);
        for (int i = 0; i < list.length; i++) {
            ProcedureParameterListener listener = (ProcedureParameterListener) list[i];
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
                    throw new IllegalArgumentException(
                            "Invalid ProcedureParameterEvent type: " + e.getId());
            }
        }
    }

    public void fireNavigationEvent(EventObject e) {
        Object source = e.getSource();
        if (source == null)
            return;

        int size = controllerStateHistory.size();
        if (size == 0)
            return;

        int i = controllerStateHistory.indexOf(currentState);
        ControllerState cs = null;
        if (size == 1) {
            cs = (ControllerState) controllerStateHistory.get(0);
        }
        else if (source instanceof NavigateForwardAction) {
            int counter = 0;
            while (true) {
                if (i < 0) {
                    // a new state got created without it being saved.
                    // just move to the beginning of the list
                    cs = (ControllerState) controllerStateHistory.get(0);
                }
                else if (i + 1 < size) {
                    // move forward
                    cs = (ControllerState) controllerStateHistory.get(i + 1);
                }
                else {
                    // wrap around
                    cs = (ControllerState) controllerStateHistory.get(0);
                }
                if (!cs.isEquivalent(currentState)) {
                    break;
                }

                // if it doesn't find it within 5 tries it is probably stuck in a loop
                if (++counter > 5) {
                    break;
                }
                i++;
            }
        }
        else if (source instanceof NavigateBackwardAction) {
            int counter = 0;
            while (true) {
                if (i < 0) {
                    // a new state got created without it being saved.
                    try {
                        cs = (ControllerState) controllerStateHistory.get(size - 2);
                    }
                    catch (ArrayIndexOutOfBoundsException ex) {
                        cs = (ControllerState) controllerStateHistory.get(size - 1);
                    }
                }
                else if (i - 1 >= 0) {
                    // move to the previous one
                    cs = (ControllerState) controllerStateHistory.get(i - 1);
                }
                else {
                    // wrap around
                    cs = (ControllerState) controllerStateHistory.get(size - 1);
                }
                if (!cs.isEquivalent(currentState)) {
                    break;
                }
                // if it doesn't find it within 5 tries it is probably stuck in a loop
                if (++counter > 5) {
                    break;
                }
                i--;
            }
        }

        // reset the current state to the one we just navigated to
        currentState = cs;
        DisplayEvent de = cs.event;
        if (de == null)
            return;

        // make sure that isRefiring is turned off prior to exiting this routine
        // this flag is used to tell the controller to not create new states
        // when we are refiring the event that we saved earlier
        currentState.isRefiring = true;

        // the order of the following is checked in most specific to generic because
        // of the inheritance heirarchy
        de.setRefired(true);
        if (de instanceof EntityDisplayEvent) {
            EntityDisplayEvent ede = (EntityDisplayEvent) de;
            ede.setEntityChanged(true);
            if (ede.getEntity() instanceof ObjEntity) {
                fireObjEntityDisplayEvent(ede);
            }
            else if (ede.getEntity() instanceof DbEntity) {
                fireDbEntityDisplayEvent(ede);
            }
        }
        else if (de instanceof ProcedureDisplayEvent) {
            ProcedureDisplayEvent pde = (ProcedureDisplayEvent) de;
            pde.setProcedureChanged(true);
            fireProcedureDisplayEvent(pde);
        }
        else if (de instanceof QueryDisplayEvent) {
            QueryDisplayEvent qde = (QueryDisplayEvent) de;
            qde.setQueryChanged(true);
            fireQueryDisplayEvent(qde);
        }
        else if (de instanceof DataMapDisplayEvent) {
            DataMapDisplayEvent dmde = (DataMapDisplayEvent) de;
            dmde.setDataMapChanged(true);
            fireDataMapDisplayEvent(dmde);
        }
        else if (de instanceof DataNodeDisplayEvent) {
            DataNodeDisplayEvent dnde = (DataNodeDisplayEvent) de;
            dnde.setDataNodeChanged(true);
            fireDataNodeDisplayEvent(dnde);
        }
        else if (de instanceof DomainDisplayEvent) {
            DomainDisplayEvent dde = (DomainDisplayEvent) de;
            dde.setDomainChanged(true);
            fireDomainDisplayEvent(dde);
        }

        // turn off refiring
        currentState.isRefiring = false;
    }

    public void fireObjEntityDisplayEvent(EntityDisplayEvent e) {
        boolean changed = e.getEntity() != currentState.objEntity;

        if (!e.isRefired()) {
            e.setEntityChanged(changed);

            if (changed) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.node = e.getDataNode();
                currentState.map = e.getDataMap();
                currentState.objEntity = (ObjEntity) e.getEntity();
            }
        }

        if (changed) {
            saveState(e);
        }

        EventListener[] list = listenerList.getListeners(ObjEntityDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            ObjEntityDisplayListener temp = (ObjEntityDisplayListener) list[i];
            temp.currentObjEntityChanged(e);
        }
    }

    public void fireQueryDisplayEvent(QueryDisplayEvent e) {
        boolean changed = e.getQuery() != currentState.query;

        if (!e.isRefired()) {
            e.setQueryChanged(changed);

            if (changed) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.map = e.getDataMap();
                currentState.query = e.getQuery();
            }
        }

        if (changed) {
            saveState(e);
        }

        EventListener[] list = listenerList.getListeners(QueryDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            QueryDisplayListener listener = (QueryDisplayListener) list[i];
            listener.currentQueryChanged(e);
        }
    }

    public void fireProcedureDisplayEvent(ProcedureDisplayEvent e) {
        boolean changed = e.getProcedure() != currentState.procedure;

        if (!e.isRefired()) {
            e.setProcedureChanged(changed);

            if (changed) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.map = e.getDataMap();
                currentState.procedure = e.getProcedure();
            }
        }

        if (changed) {
            saveState(e);
        }

        EventListener[] list = listenerList.getListeners(ProcedureDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            ProcedureDisplayListener listener = (ProcedureDisplayListener) list[i];
            listener.currentProcedureChanged(e);
        }
    }

    public void fireProcedureParameterDisplayEvent(ProcedureParameterDisplayEvent e) {
        boolean changed = e.getProcedureParameter() != currentState.procedureParameter;

        if (changed) {
            if (currentState.procedure != e.getProcedure()) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.map = e.getDataMap();
                currentState.procedure = e.getProcedure();
            }
            currentState.procedureParameter = e.getProcedureParameter();
        }

        EventListener[] list = listenerList
                .getListeners(ProcedureParameterDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            ProcedureParameterDisplayListener listener = (ProcedureParameterDisplayListener) list[i];
            listener.currentProcedureParameterChanged(e);
        }
    }

    public void fireDbEntityDisplayEvent(EntityDisplayEvent e) {
        boolean changed = e.getEntity() != currentState.dbEntity;
        if (!e.isRefired()) {
            e.setEntityChanged(changed);

            if (changed) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.node = e.getDataNode();
                currentState.map = e.getDataMap();
                currentState.dbEntity = (DbEntity) e.getEntity();
            }
        }

        if (changed) {
            saveState(e);
        }

        EventListener[] list = listenerList.getListeners(DbEntityDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            DbEntityDisplayListener temp = (DbEntityDisplayListener) list[i];
            temp.currentDbEntityChanged(e);
        }
    }

    /** Notifies all listeners of the change(add, remove) and does the change. */
    public void fireDbAttributeEvent(AttributeEvent e) {
        setDirty(true);

        if (currentState.map != null && e.getId() == MapEvent.CHANGE) {
            currentState.map.dbAttributeChanged(e);
        }

        EventListener[] list = listenerList.getListeners(DbAttributeListener.class);
        for (int i = 0; i < list.length; i++) {
            DbAttributeListener temp = (DbAttributeListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    temp.dbAttributeAdded(e);
                    break;
                case MapEvent.CHANGE:
                    temp.dbAttributeChanged(e);
                    break;
                case MapEvent.REMOVE:
                    temp.dbAttributeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid AttributeEvent type: "
                            + e.getId());
            }
        }
    }

    public void fireDbAttributeDisplayEvent(AttributeDisplayEvent e) {
        boolean changed = e.getAttribute() != currentState.dbAttr;

        if (changed) {
            if (e.getEntity() != currentState.dbEntity) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.map = e.getDataMap();
                currentState.dbEntity = (DbEntity) e.getEntity();
            }
            currentState.dbAttr = (DbAttribute) e.getAttribute();
        }

        EventListener[] list = listenerList
                .getListeners(DbAttributeDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            DbAttributeDisplayListener temp = (DbAttributeDisplayListener) list[i];
            temp.currentDbAttributeChanged(e);
        }
    }

    /** Notifies all listeners of the change (add, remove) and does the change. */
    public void fireObjAttributeEvent(AttributeEvent e) {
        setDirty(true);

        if (currentState.map != null && e.getId() == MapEvent.CHANGE) {
            currentState.map.objAttributeChanged(e);
        }

        EventListener[] list = listenerList.getListeners(ObjAttributeListener.class);
        for (int i = 0; i < list.length; i++) {
            ObjAttributeListener temp = (ObjAttributeListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    temp.objAttributeAdded(e);
                    break;
                case MapEvent.CHANGE:
                    temp.objAttributeChanged(e);
                    break;
                case MapEvent.REMOVE:
                    temp.objAttributeRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid AttributeEvent type: "
                            + e.getId());
            }
        }
    }

    public void fireObjAttributeDisplayEvent(AttributeDisplayEvent e) {
        boolean changed = e.getAttribute() != currentState.objAttr;

        if (changed) {
            if (e.getEntity() != currentState.objEntity) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.map = e.getDataMap();
                currentState.objEntity = (ObjEntity) e.getEntity();
            }
            currentState.objAttr = (ObjAttribute) e.getAttribute();
        }

        EventListener[] list = listenerList
                .getListeners(ObjAttributeDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            ObjAttributeDisplayListener temp = (ObjAttributeDisplayListener) list[i];
            temp.currentObjAttributeChanged(e);
        }
    }

    /** Notifies all listeners of the change(add, remove) and does the change. */
    public void fireDbRelationshipEvent(RelationshipEvent e) {
        setDirty(true);
        
        if(e.getId() == MapEvent.CHANGE && e.getEntity() instanceof DbEntity) {
            ((DbEntity) e.getEntity()).dbRelationshipChanged(e);
        }

        EventListener[] list = listenerList.getListeners(DbRelationshipListener.class);
        for (int i = 0; i < list.length; i++) {
            DbRelationshipListener temp = (DbRelationshipListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    temp.dbRelationshipAdded(e);
                    break;
                case MapEvent.CHANGE:
                    temp.dbRelationshipChanged(e);
                    break;
                case MapEvent.REMOVE:
                    temp.dbRelationshipRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid RelationshipEvent type: "
                            + e.getId());
            }
        }
    }

    public void fireDbRelationshipDisplayEvent(RelationshipDisplayEvent e) {
        boolean changed = e.getRelationship() != currentState.dbRel;
        e.setRelationshipChanged(changed);

        if (changed) {
            if (e.getEntity() != currentState.dbEntity) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.map = e.getDataMap();
                currentState.dbEntity = (DbEntity) e.getEntity();
            }
            currentState.dbRel = (DbRelationship) e.getRelationship();
        }

        EventListener[] list = listenerList
                .getListeners(DbRelationshipDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            DbRelationshipDisplayListener temp = (DbRelationshipDisplayListener) list[i];
            temp.currentDbRelationshipChanged(e);
        }
    }

    /** Notifies all listeners of the change(add, remove) and does the change. */
    public void fireObjRelationshipEvent(RelationshipEvent e) {
        setDirty(true);

        if (currentState.map != null && e.getId() == MapEvent.CHANGE) {
            currentState.map.objRelationshipChanged(e);
        }

        EventListener[] list = listenerList.getListeners(ObjRelationshipListener.class);
        for (int i = 0; i < list.length; i++) {
            ObjRelationshipListener temp = (ObjRelationshipListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    temp.objRelationshipAdded(e);
                    break;
                case MapEvent.CHANGE:
                    temp.objRelationshipChanged(e);
                    break;
                case MapEvent.REMOVE:
                    temp.objRelationshipRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid RelationshipEvent type: "
                            + e.getId());
            }
        }
    }

    public void fireObjRelationshipDisplayEvent(RelationshipDisplayEvent e) {
        boolean changed = e.getRelationship() != currentState.objRel;
        e.setRelationshipChanged(changed);

        if (changed) {
            if (e.getEntity() != currentState.objEntity) {
                clearState();
                currentState.domain = e.getDomain();
                currentState.map = e.getDataMap();
                currentState.objEntity = (ObjEntity) e.getEntity();
            }
            currentState.objRel = (ObjRelationship) e.getRelationship();
        }

        EventListener[] list = listenerList
                .getListeners(ObjRelationshipDisplayListener.class);
        for (int i = 0; i < list.length; i++) {
            ObjRelationshipDisplayListener temp = (ObjRelationshipDisplayListener) list[i];
            temp.currentObjRelationshipChanged(e);
        }
    }

    public void addDataMap(Object src, DataMap map) {
        addDataMap(src, map, true);
    }

    public void addDataMap(Object src, DataMap map, boolean makeCurrent) {

        // new map was added.. link it to domain (and node if possible)
        currentState.domain.addMap(map);

        if (currentState.node != null && !currentState.node.getDataMaps().contains(map)) {
            currentState.node.addDataMap(map);
            fireDataNodeEvent(new DataNodeEvent(this, currentState.node));
            currentState.domain.reindexNodes();
        }

        fireDataMapEvent(new DataMapEvent(src, map, MapEvent.ADD));
        if (makeCurrent) {
            fireDataMapDisplayEvent(new DataMapDisplayEvent(
                    src,
                    map,
                    currentState.domain,
                    currentState.node));
        }
    }

    public void setDirty(boolean dirty) {
        if (this.dirty != dirty) {
            this.dirty = dirty;

            application.getAction(SaveAction.getActionName()).setEnabled(dirty);
            application.getAction(RevertAction.getActionName()).setEnabled(dirty);

            if (dirty) {
                ((CayenneModelerController) getParent()).projectModifiedAction();
            }
        }
    }

    /**
     * @return currently selecte entity listener class
     */
    public String getCurrentListenerClass() {
        return currentState.listenerClass;
    }

    /**
     * @return currently selected callback type
     */
    public CallbackType getCurrentCallbackType() {
        return currentState.callbackType;
    }

    /**
     * @return currently selected callback method
     */
    public String getCurrentCallbackMethod() {
        return currentState.callbackMethod;
    }


    /**
     * @return currently selecte entity listener class
     */
    public void setCurrentListenerClass(String listenerClass) {
        currentState.listenerClass = listenerClass;
    }

    /**
     * @return currently selected callback type
     */
    public void setCurrentCallbackType(CallbackType callbackType) {
        currentState.callbackType = callbackType;
    }

    /**
     * @return currently selected callback method
     */
    public void setCurrentCallbackMethod(String callbackMethod) {
        currentState.callbackMethod = callbackMethod;
    }

    /**
     * adds callback method manipulation listener
     * @param listener listener
     */
    public void addCallbackMethodListener(CallbackMethodListener listener) {
        listenerList.add(CallbackMethodListener.class, listener);
    }
    
    /**
     * fires callback method manipulation event
     * @param e event
     */
    public void fireCallbackMethodEvent(CallbackMethodEvent e) {
        setDirty(true);

        EventListener[] list = listenerList.getListeners(CallbackMethodListener.class);
        for (int i = 0; i < list.length; i++) {
            CallbackMethodListener temp = (CallbackMethodListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    temp.callbackMethodAdded(e);
                    break;
                case MapEvent.CHANGE:
                    temp.callbackMethodChanged(e);
                    break;
                case MapEvent.REMOVE:
                    temp.callbackMethodRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid CallbackEvent type: "
                            + e.getId());
            }
        }
    }

    /**
     * adds listener class manipulation listener
     * @param listener listener
     */
    public void addEntityListenerListener(EntityListenerListener listener) {
        listenerList.add(EntityListenerListener.class, listener);
    }

    /**
     * fires entity listener manipulation event
     * @param e event
     */
    public void fireEntityListenerEvent(EntityListenerEvent e) {
        setDirty(true);

        EventListener[] list = listenerList.getListeners(EntityListenerListener.class);
        for (int i = 0; i < list.length; i++) {
            EntityListenerListener temp = (EntityListenerListener) list[i];
            switch (e.getId()) {
                case MapEvent.ADD:
                    temp.entityListenerAdded(e);
                    break;
                case MapEvent.CHANGE:
                    temp.entityListenerChanged(e);
                    break;
                case MapEvent.REMOVE:
                    temp.entityListenerRemoved(e);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid CallbackEvent type: "
                            + e.getId());
            }
        }
    }
}
