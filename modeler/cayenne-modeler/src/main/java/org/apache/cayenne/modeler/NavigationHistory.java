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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.display.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.display.DisplayEvent;
import org.apache.cayenne.modeler.event.display.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.display.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.event.model.DataNodeEvent;
import org.apache.cayenne.modeler.event.model.DomainEvent;
import org.apache.cayenne.modeler.event.model.ProcedureEvent;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.ui.project.ProjectController;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class NavigationHistory {
    private final static int MAX_HISTORY_SIZE = 20;

    private final List<DisplayEvent> history;
    private final List<DisplayEvent> replayHistory;
    private DisplayEvent currentEvent;

    public NavigationHistory() {
        this.history = new ArrayList<>(MAX_HISTORY_SIZE);
        this.replayHistory = new ArrayList<>(MAX_HISTORY_SIZE);
    }

    public DisplayEvent getLastEvent() {
        return currentEvent;
    }

    public void recordEvent(DisplayEvent e) {
        recordEvent(history, e);
    }

    public void replayLastEvent(ProjectController controller) {

        if (history.isEmpty()) {
            return;
        }

        DisplayEvent e = history.remove(history.size() - 1);
        recordEvent(replayHistory, e);
        replay(controller, e);
    }


    public void replayNextEvent(ProjectController controller) {
        if (replayHistory.isEmpty()) {
            return;
        }

        DisplayEvent e = replayHistory.remove(replayHistory.size() - 1);
        recordEvent(history, e);
        replay(controller, e);
    }

    private void recordEvent(List<DisplayEvent> target, DisplayEvent e) {

        // "currentEvent" will be the same as "e" if this was caused by replay
        if (this.currentEvent != null && this.currentEvent != e) {
            if (target.size() >= MAX_HISTORY_SIZE) {
                target.remove(0);
            }

            target.add(this.currentEvent);
        }

        this.currentEvent = e;
    }

    private void replay(ProjectController controller, DisplayEvent e) {
        if (e instanceof EntityDisplayEvent) {
            EntityDisplayEvent ede = (EntityDisplayEvent) e;
            if (ede.getEntity() instanceof ObjEntity) {
                controller.displayObjEntity(ede);
            } else if (ede.getEntity() instanceof DbEntity) {
                controller.displayDbEntity(ede);
            }
        } else if (e instanceof EmbeddableDisplayEvent) {
            controller.displayEmbeddable((EmbeddableDisplayEvent) e);
        } else if (e instanceof ProcedureDisplayEvent) {
            controller.displayProcedure((ProcedureDisplayEvent) e);
        } else if (e instanceof QueryDisplayEvent) {
            controller.displayQuery((QueryDisplayEvent) e);
        } else if (e instanceof DataMapDisplayEvent) {
            controller.displayDataMap((DataMapDisplayEvent) e);
        } else if (e instanceof DataNodeDisplayEvent) {
            controller.displayDataNode((DataNodeDisplayEvent) e);
        } else if (e instanceof DomainDisplayEvent) {
            controller.displayDomain((DomainDisplayEvent) e);
        }
    }

    public void forgetObject(EventObject e) {

        Consumer<List<DisplayEvent>> remover = list -> {
            Iterator<DisplayEvent> it = list.iterator();
            while (it.hasNext()) {
                DisplayEvent de = it.next();
                if (e instanceof EntityEvent && de instanceof EntityDisplayEvent) {
                    if (((EntityEvent) e).getEntity() == ((EntityDisplayEvent) de).getEntity()) {
                        it.remove();
                    }
                } else if (e instanceof EmbeddableEvent && de instanceof EmbeddableDisplayEvent) {
                    if (((EmbeddableEvent) e).getEmbeddable() == ((EmbeddableDisplayEvent) de).getEmbeddable()) {
                        it.remove();
                    }
                } else if (e instanceof ProcedureEvent && de instanceof ProcedureDisplayEvent) {
                    if (((ProcedureEvent) e).getProcedure() == ((ProcedureDisplayEvent) de).getProcedure()) {
                        it.remove();
                    }
                } else if (e instanceof QueryEvent && de instanceof QueryDisplayEvent) {
                    if (((QueryEvent) e).getQuery() == ((QueryDisplayEvent) de).getQuery()) {
                        it.remove();
                    }
                } else if (e instanceof DataMapEvent && de instanceof DataMapDisplayEvent) {
                    if (((DataMapEvent) e).getDataMap() == ((DataMapDisplayEvent) de).getDataMap()) {
                        it.remove();
                    }
                } else if (e instanceof DataNodeEvent && de instanceof DataNodeDisplayEvent) {
                    if (((DataNodeEvent) e).getDataNode() == ((DataNodeDisplayEvent) de).getDataNode()) {
                        it.remove();
                    }
                } else if (e instanceof DomainEvent && de instanceof DomainDisplayEvent) {
                    if (((DomainEvent) e).getDomain() == ((DomainDisplayEvent) de).getDomain()) {
                        it.remove();
                    }
                }
            }
        };

        remover.accept(history);
        remover.accept(replayHistory);
    }

}
