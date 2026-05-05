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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.model.DetectedDbEntity;
import org.apache.cayenne.dbsync.reverse.dbload.DbRelationshipDetected;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EJBQLQueryDescriptor;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.ProcedureQueryDescriptor;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.display.DbAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbRelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjRelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.event.display.QueryDisplayEvent;
import org.apache.cayenne.modeler.ui.find.FindDialog;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.ProjectView;
import org.apache.cayenne.modeler.ui.project.tree.ProjectTreeModel;
import org.apache.cayenne.modeler.ui.project.tree.ProjectTree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class FindAction extends ModelerAbstractAction {

    /**
     * Result sort priority based on result type
     */
    private static final Map<Class<?>, Integer> PRIORITY_BY_TYPE = new HashMap<>();

    static {
        PRIORITY_BY_TYPE.put(ObjEntity.class, 1);
        PRIORITY_BY_TYPE.put(DbEntity.class, 2);
        PRIORITY_BY_TYPE.put(DetectedDbEntity.class, 2); // this one comes from db reverse engineering
        PRIORITY_BY_TYPE.put(ObjAttribute.class, 5);
        PRIORITY_BY_TYPE.put(DbAttribute.class, 6);
        PRIORITY_BY_TYPE.put(ObjRelationship.class, 7);
        PRIORITY_BY_TYPE.put(DbRelationship.class, 8);
        PRIORITY_BY_TYPE.put(DbRelationshipDetected.class, 8); // this one comes from db reverse engineering
        PRIORITY_BY_TYPE.put(QueryDescriptor.class, 9);
        PRIORITY_BY_TYPE.put(SelectQueryDescriptor.class, 10);
        PRIORITY_BY_TYPE.put(EJBQLQueryDescriptor.class, 11);
        PRIORITY_BY_TYPE.put(SQLTemplateDescriptor.class, 12);
        PRIORITY_BY_TYPE.put(ProcedureQueryDescriptor.class, 13);
        PRIORITY_BY_TYPE.put(Embeddable.class, 14);
        PRIORITY_BY_TYPE.put(EmbeddableAttribute.class, 15);
        PRIORITY_BY_TYPE.put(Procedure.class, 16);
        PRIORITY_BY_TYPE.put(ProcedureParameter.class, 17);
    }

    public FindAction(Application application) {
        super("Find", application);
    }

    /**
     * All entities that contain a pattern substring (case-indifferent) in the name are produced.
     */
    public void performAction(ActionEvent e) {
        JTextField source = (JTextField) e.getSource();
        String searchStr = source.getText().trim();
        if (searchStr.startsWith("*")) {
            searchStr = searchStr.substring(1);
        }

        if (searchStr.isEmpty()) {
            markEmptySearch(source);
            return;
        }

        List<SearchResultEntry> searchResults = search(searchStr);
        if (searchResults.isEmpty()) {
            markEmptySearch(source);
        } else if (searchResults.size() == 1) {
            jumpToResult(searchResults.iterator().next(), app);
        } else {
            new FindDialog(app, app.getFrame(), searchResults).open();
        }
    }

    private void markEmptySearch(JTextField source) {
        source.setBackground(Color.pink);
    }

    /**
     * Navigate to search result
     */
    public static void jumpToResult(FindAction.SearchResultEntry searchResultEntry, Application application) {
        ProjectSession session = application.getFrame().getProjectSession();
        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        ProjectView projectView = application.getFrame().getProjectView();

        if (searchResultEntry.getObject() instanceof Entity) {
            jumpToEntityResult((Entity<?, ?, ?>) searchResultEntry.getObject(), projectView, domain, session);
        } else if (searchResultEntry.getObject() instanceof QueryDescriptor) {
            jumpToQueryResult((QueryDescriptor) searchResultEntry.getObject(), projectView, domain, session);
        } else if (searchResultEntry.getObject() instanceof Embeddable) {
            jumpToEmbeddableResult((Embeddable) searchResultEntry.getObject(), projectView, domain, session);
        } else if (searchResultEntry.getObject() instanceof EmbeddableAttribute) {
            jumpToEmbeddableAttributeResult((EmbeddableAttribute) searchResultEntry.getObject(), projectView, domain, session);
        } else if (searchResultEntry.getObject() instanceof Attribute || searchResultEntry.getObject() instanceof Relationship) {
            jumpToAttributeResult(searchResultEntry, projectView, domain, session);
        } else if (searchResultEntry.getObject() instanceof Procedure) {
            jumpToProcedureResult((Procedure) searchResultEntry.getObject(), projectView, domain, session);
        } else if (searchResultEntry.getObject() instanceof ProcedureParameter) {
            jumpToProcedureResult((ProcedureParameter) searchResultEntry.getObject(), projectView, domain, session);
        }
    }

    private List<SearchResultEntry> search(String searchStr) {
        Pattern pattern = Pattern.compile(searchStr, Pattern.CASE_INSENSITIVE);
        List<SearchResultEntry> result = new ArrayList<>();
        for (DataMap dataMap : ((DataChannelDescriptor) getProjectSession().project().getRootNode()).getDataMaps()) {
            searchInQueryDescriptors(pattern, result, dataMap);
            searchInEmbeddables(pattern, result, dataMap);
            searchInDbEntities(pattern, result, dataMap);
            searchInObjEntities(pattern, result, dataMap);
            searchInProcedures(pattern, result, dataMap);
        }
        Collections.sort(result);
        return result;
    }

    private void searchInQueryDescriptors(Pattern pattern, List<SearchResultEntry> result, DataMap dataMap) {
        for (QueryDescriptor q : dataMap.getQueryDescriptors()) {
            if (match(q.getName(), pattern)) {
                result.add(new SearchResultEntry(q, q.getName()));
            }
        }
    }

    private void searchInEmbeddables(Pattern pattern, List<SearchResultEntry> result, DataMap dataMap) {
        for (Embeddable emb : dataMap.getEmbeddables()) {
            if (match(emb.getClassName(), pattern)) {
                result.add(new SearchResultEntry(emb, emb.getClassName()));
            }
            for (EmbeddableAttribute attr : emb.getAttributes()) {
                if (match(attr.getName(), pattern)) {
                    result.add(new SearchResultEntry(attr, emb.getClassName() + "." + attr.getName()));
                }
            }
        }
    }

    private void searchInObjEntities(Pattern pattern, List<SearchResultEntry> result, DataMap dataMap) {
        for (ObjEntity ent : dataMap.getObjEntities()) {
            if (match(ent.getName(), pattern)) {
                result.add(new SearchResultEntry(ent, ent.getName()));
            }
            for (ObjAttribute attr : ent.getAttributes()) {
                if (match(attr.getName(), pattern)) {
                    result.add(new SearchResultEntry(attr, ent.getName() + "." + attr.getName()));
                }
            }
            for (ObjRelationship rel : ent.getRelationships()) {
                if (match(rel.getName(), pattern)) {
                    result.add(new SearchResultEntry(rel, ent.getName() + "." + rel.getName()));
                }
            }
        }
    }

    private void searchInDbEntities(Pattern pattern, List<SearchResultEntry> result, DataMap dataMap) {
        for (DbEntity ent : dataMap.getDbEntities()) {
            if (match(ent.getName(), pattern)) {
                result.add(new SearchResultEntry(ent, ent.getName()));
            }
            for (DbAttribute attr : ent.getAttributes()) {
                if (match(attr.getName(), pattern)) {
                    result.add(new SearchResultEntry(attr, ent.getName() + "." + attr.getName()));
                }
            }
            for (DbRelationship rel : ent.getRelationships()) {
                if (match(rel.getName(), pattern)) {
                    result.add(new SearchResultEntry(rel, ent.getName() + "." + rel.getName()));
                }
            }

            checkCatalogOrSchema(pattern, result, ent, ent.getCatalog());
            checkCatalogOrSchema(pattern, result, ent, ent.getSchema());
        }
    }

    private void searchInProcedures(Pattern pattern, List<SearchResultEntry> result, DataMap dataMap) {
        for (Procedure proc : dataMap.getProcedures()) {
            if (match(proc.getName(), pattern)) {
                result.add(new SearchResultEntry(proc, proc.getName()));
            }

            for (ProcedureParameter param : proc.getCallParameters()) {
                if (match(param.getName(), pattern)) {
                    result.add(new SearchResultEntry(param, proc.getName() + '.' + param.getName()));
                }
            }
        }
    }

    private void checkCatalogOrSchema(Pattern pattern, List<SearchResultEntry> paths, DbEntity ent, String catalogOrSchema) {
        if (catalogOrSchema != null && !catalogOrSchema.isEmpty()) {
            if (match(catalogOrSchema, pattern)) {
                SearchResultEntry entry = new SearchResultEntry(ent, ent.getName());
                if (!paths.contains(entry)) {
                    paths.add(entry);
                }
            }
        }
    }

    private boolean match(String entityName, Pattern pattern) {
        return pattern.matcher(entityName).find();
    }

    private static void jumpToAttributeResult(SearchResultEntry searchResultEntry, ProjectView projectView, DataChannelDescriptor domain,
                                              ProjectSession session) {
        DataMap map;
        Entity<?, ?, ?> entity;
        if (searchResultEntry.getObject() instanceof Attribute) {
            map = ((Attribute<?, ?, ?>) searchResultEntry.getObject()).getEntity().getDataMap();
            entity = ((Attribute<?, ?, ?>) searchResultEntry.getObject()).getEntity();
        } else {
            map = ((Relationship<?, ?, ?>) searchResultEntry.getObject()).getSourceEntity().getDataMap();
            entity = ((Relationship<?, ?, ?>) searchResultEntry.getObject()).getSourceEntity();
        }
        buildAndSelectTreePath(map, entity, projectView);

        if (searchResultEntry.getObject() instanceof DbAttribute) {
            DbAttributeDisplayEvent event = new DbAttributeDisplayEvent(
                    projectView.getProjectTreeView(), domain, map, (DbEntity) entity,
                    (DbAttribute) searchResultEntry.getObject());
            session.displayDbAttribute(event);
            projectView.getEditorPanel().getDbDetailView().repaint();
        } else if (searchResultEntry.getObject() instanceof ObjAttribute) {
            ObjAttributeDisplayEvent event = new ObjAttributeDisplayEvent(
                    projectView.getProjectTreeView(), domain, map, (ObjEntity) entity,
                    (ObjAttribute) searchResultEntry.getObject());
            session.displayObjAttribute(event);
            projectView.getEditorPanel().getObjDetailView().repaint();
        } else if (searchResultEntry.getObject() instanceof DbRelationship) {
            DbRelationshipDisplayEvent event = new DbRelationshipDisplayEvent(
                    projectView.getProjectTreeView(), domain, map, (DbEntity) entity,
                    (DbRelationship) searchResultEntry.getObject());
            session.displayDbRelationship(event);
            projectView.getEditorPanel().getDbDetailView().repaint();
        } else if (searchResultEntry.getObject() instanceof ObjRelationship) {
            ObjRelationshipDisplayEvent event = new ObjRelationshipDisplayEvent(
                    projectView.getProjectTreeView(), domain, map, (ObjEntity) entity,
                    (ObjRelationship) searchResultEntry.getObject());
            session.displayObjRelationship(event);
            projectView.getEditorPanel().getObjDetailView().repaint();
        }
    }

    private static void jumpToEmbeddableAttributeResult(
            EmbeddableAttribute attribute,
            ProjectView projectView,
            DataChannelDescriptor domain,
            ProjectSession session) {

        Embeddable embeddable = attribute.getEmbeddable();
        DataMap map = embeddable.getDataMap();
        buildAndSelectTreePath(map, embeddable, projectView);
        EmbeddableAttributeDisplayEvent event = new EmbeddableAttributeDisplayEvent(
                projectView.getProjectTreeView(), domain, map, embeddable, attribute);
        session.displayEmbeddableAttribute(event);
        projectView.getEditorPanel().getEmbeddableView().repaint();
    }

    private static void jumpToEmbeddableResult(Embeddable embeddable, ProjectView projectView, DataChannelDescriptor domain,
                                               ProjectSession session) {
        DataMap map = embeddable.getDataMap();
        buildAndSelectTreePath(map, embeddable, projectView);
        EmbeddableDisplayEvent event = new EmbeddableDisplayEvent(
                projectView.getProjectTreeView(), domain, map, embeddable, true);
        session.displayEmbeddable(event);
    }

    private static void jumpToQueryResult(QueryDescriptor queryDescriptor, ProjectView projectView, DataChannelDescriptor domain, ProjectSession session) {
        DataMap map = queryDescriptor.getDataMap();
        buildAndSelectTreePath(map, queryDescriptor, projectView);
        QueryDisplayEvent event = new QueryDisplayEvent(projectView.getProjectTreeView(), domain, map, queryDescriptor);
        session.displayQuery(event);
    }

    private static void jumpToEntityResult(Entity<?, ?, ?> entity, ProjectView projectView, DataChannelDescriptor domain, ProjectSession session) {
        DataMap map = entity.getDataMap();
        buildAndSelectTreePath(map, entity, projectView);

        if (entity instanceof ObjEntity) {
            ObjEntityDisplayEvent event = new ObjEntityDisplayEvent(
                    projectView.getProjectTreeView(), domain, map, (ObjEntity) entity, true, false);
            session.displayObjEntity(event);
        } else if (entity instanceof DbEntity) {
            DbEntityDisplayEvent event = new DbEntityDisplayEvent(
                    projectView.getProjectTreeView(), domain, map, (DbEntity) entity, true, false);
            session.displayDbEntity(event);
        }
    }

    private static void jumpToProcedureResult(Procedure procedure, ProjectView projectView, DataChannelDescriptor domain,
                                              ProjectSession session) {
        DataMap map = procedure.getDataMap();
        buildAndSelectTreePath(map, procedure, projectView);
        ProcedureDisplayEvent event = new ProcedureDisplayEvent(projectView.getProjectTreeView(), domain, map, procedure);
        session.displayProcedure(event);
        projectView.getEditorPanel().getProcedureView().repaint();
    }

    private static void jumpToProcedureResult(ProcedureParameter parameter, ProjectView projectView, DataChannelDescriptor domain,
                                              ProjectSession session) {
        Procedure procedure = parameter.getProcedure();
        DataMap map = procedure.getDataMap();
        buildAndSelectTreePath(map, procedure, projectView);
        ProcedureParameterDisplayEvent event =
                new ProcedureParameterDisplayEvent(projectView.getProjectTreeView(), domain, map, procedure, parameter);
        session.displayProcedureParameter(event);
        projectView.getEditorPanel().getProcedureView().repaint();
    }

    /**
     * Builds a tree path for a given path and make selection in it
     */
    private static void buildAndSelectTreePath(DataMap map, Object object, ProjectView projectView) {
        ProjectTree projectTree = projectView.getProjectTreeView();
        ProjectTreeModel treeModel = (ProjectTreeModel) projectTree.getModel();

        DefaultMutableTreeNode[] mutableTreeNodes = new DefaultMutableTreeNode[]{
                treeModel.getRootNode(),
                treeModel.getNodeForObjectPath(map),
                treeModel.getNodeForObjectPath(map, object)
        };

        TreePath treePath = new TreePath(mutableTreeNodes);
        if (!projectTree.isExpanded(treePath.getParentPath())) {
            projectTree.expandPath(treePath.getParentPath());
        }
        projectTree.getSelectionModel().setSelectionPath(treePath);
    }

    /**
     * Search result holder
     */
    public static class SearchResultEntry implements Comparable<SearchResultEntry> {
        private final Object object;
        private final String name;

        public SearchResultEntry(Object object, String name) {
            this.object = Objects.requireNonNull(object);
            this.name = Objects.requireNonNull(name);
        }

        public String getName() {
            return name;
        }

        public Object getObject() {
            return object;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchResultEntry entry = (SearchResultEntry) o;
            return name.equals(entry.name) && object.getClass().equals(entry.object.getClass());
        }

        @Override
        public int hashCode() {
            int result = object.getClass().hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        private int getPriority() {
            Integer priority = PRIORITY_BY_TYPE.get(object.getClass());
            if (priority == null) {
                throw new NullPointerException("Unknown type: " + object.getClass().getCanonicalName());
            }
            return priority;
        }

        @Override
        public int compareTo(SearchResultEntry o) {
            int res = getPriority() - o.getPriority();
            if (res != 0) {
                return res;
            }
            return getName().compareTo(o.getName());
        }
    }
}
