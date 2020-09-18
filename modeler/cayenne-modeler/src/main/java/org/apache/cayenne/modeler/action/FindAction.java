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
package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.reverse.dbload.DbRelationshipDetected;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.dbsync.model.DetectedDbEntity;
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
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectTreeModel;
import org.apache.cayenne.modeler.ProjectTreeView;
import org.apache.cayenne.modeler.dialog.FindDialog;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.map.QueryDescriptor;

import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class FindAction extends CayenneAction {

    /**
     * Result sort priority based on result type
     */
    private static final Map<Class<?>, Integer> PRIORITY_BY_TYPE = new HashMap<>();
    static {
        PRIORITY_BY_TYPE.put(ObjEntity.class,                1);
        PRIORITY_BY_TYPE.put(DbEntity.class,                 2);
        PRIORITY_BY_TYPE.put(DetectedDbEntity.class,         2); // this one comes from db reverse engineering
        PRIORITY_BY_TYPE.put(ObjAttribute.class,             5);
        PRIORITY_BY_TYPE.put(DbAttribute.class,              6);
        PRIORITY_BY_TYPE.put(ObjRelationship.class,          7);
        PRIORITY_BY_TYPE.put(DbRelationship.class,           8);
        PRIORITY_BY_TYPE.put(DbRelationshipDetected.class,   8); // this one comes from db reverse engineering
        PRIORITY_BY_TYPE.put(QueryDescriptor.class,          9);
        PRIORITY_BY_TYPE.put(SelectQueryDescriptor.class,   10);
        PRIORITY_BY_TYPE.put(EJBQLQueryDescriptor.class,    11);
        PRIORITY_BY_TYPE.put(SQLTemplateDescriptor.class,   12);
        PRIORITY_BY_TYPE.put(ProcedureQueryDescriptor.class,13);
        PRIORITY_BY_TYPE.put(Embeddable.class,              14);
        PRIORITY_BY_TYPE.put(EmbeddableAttribute.class,     15);
        PRIORITY_BY_TYPE.put(Procedure.class,               16);
        PRIORITY_BY_TYPE.put(ProcedureParameter.class,      17);
    }

    public static String getActionName() {
        return "Find";
    }

    public FindAction(Application application) {
        super(getActionName(), application);
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

        if(searchStr.isEmpty()) {
            markEmptySearch(source);
            return;
        }

        List<SearchResultEntry> searchResults = search(searchStr);
        if(searchResults.isEmpty()){
            markEmptySearch(source);
        } else if(searchResults.size() == 1){
            jumpToResult(searchResults.iterator().next());
        } else {
            new FindDialog(getApplication().getFrameController(), searchResults).startupAction();
        }
    }

    private void markEmptySearch(JTextField source) {
        source.setBackground(Color.pink);
    }

    /**
     * Navigate to search result
     * Used also in {@link org.apache.cayenne.modeler.graph.action.EntityDisplayAction}
     */
    public static void jumpToResult(FindAction.SearchResultEntry searchResultEntry) {
        EditorView editor = ((CayenneModelerFrame) Application.getInstance().getFrameController().getView()).getView();
        DataChannelDescriptor domain = (DataChannelDescriptor) Application.getInstance().getProject().getRootNode();

        if (searchResultEntry.getObject() instanceof Entity) {
            jumpToEntityResult((Entity) searchResultEntry.getObject(), editor, domain);
        } else if (searchResultEntry.getObject() instanceof QueryDescriptor) {
            jumpToQueryResult((QueryDescriptor)searchResultEntry.getObject(), editor, domain);
        } else if (searchResultEntry.getObject() instanceof Embeddable) {
            jumpToEmbeddableResult((Embeddable)searchResultEntry.getObject(), editor, domain);
        } else if (searchResultEntry.getObject() instanceof EmbeddableAttribute) {
            jumpToEmbeddableAttributeResult((EmbeddableAttribute)searchResultEntry.getObject(), editor, domain);
        } else if (searchResultEntry.getObject() instanceof Attribute || searchResultEntry.getObject() instanceof Relationship) {
            jumpToAttributeResult(searchResultEntry, editor, domain);
        } else if (searchResultEntry.getObject() instanceof Procedure) {
            jumpToProcedureResult((Procedure)searchResultEntry.getObject(), editor, domain);
        } else if (searchResultEntry.getObject() instanceof ProcedureParameter) {
            jumpToProcedureResult((ProcedureParameter)searchResultEntry.getObject(), editor, domain);
        }
    }

    private List<SearchResultEntry> search(String searchStr) {
        Pattern pattern = Pattern.compile(searchStr, Pattern.CASE_INSENSITIVE);
        List<SearchResultEntry> result = new ArrayList<>();
        for (DataMap dataMap : ((DataChannelDescriptor) getProjectController().getProject().getRootNode()).getDataMaps()) {
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

            for(ProcedureParameter param : proc.getCallParameters()) {
                if(match(param.getName(), pattern)) {
                    result.add(new SearchResultEntry(param, proc.getName() + '.' + param.getName()));
                }
            }
        }
    }

    private void checkCatalogOrSchema(Pattern pattern, List<SearchResultEntry> paths, DbEntity ent, String catalogOrSchema) {
        if (catalogOrSchema != null && !catalogOrSchema.isEmpty()) {
            if (match(catalogOrSchema, pattern)) {
                SearchResultEntry entry = new SearchResultEntry(ent, ent.getName());
                if(!paths.contains(entry)) {
                    paths.add(entry);
                }
            }
        }
    }

    private boolean match(String entityName, Pattern pattern) {
        return pattern.matcher(entityName).find();
    }

    private static void jumpToAttributeResult(SearchResultEntry searchResultEntry, EditorView editor, DataChannelDescriptor domain) {
        DataMap map;
        Entity entity;
        if (searchResultEntry.getObject() instanceof Attribute) {
            map = ((Attribute) searchResultEntry.getObject()).getEntity().getDataMap();
            entity = ((Attribute) searchResultEntry.getObject()).getEntity();
        } else {
            map = ((Relationship) searchResultEntry.getObject()).getSourceEntity().getDataMap();
            entity = ((Relationship) searchResultEntry.getObject()).getSourceEntity();
        }
        buildAndSelectTreePath(map, entity, editor);

        if (searchResultEntry.getObject() instanceof Attribute) {
            AttributeDisplayEvent event = new AttributeDisplayEvent(editor.getProjectTreeView(),
                    (Attribute) searchResultEntry.getObject(), entity, map, domain);
            event.setMainTabFocus(true);
            if(searchResultEntry.getObject() instanceof DbAttribute) {
                editor.getDbDetailView().currentDbAttributeChanged(event);
                editor.getDbDetailView().repaint();
            } else {
                editor.getObjDetailView().currentObjAttributeChanged(event);
                editor.getObjDetailView().repaint();
            }
        } else if (searchResultEntry.getObject() instanceof Relationship) {
            RelationshipDisplayEvent event = new RelationshipDisplayEvent(editor.getProjectTreeView(),
                    (Relationship) searchResultEntry.getObject(), entity, map, domain);
            event.setMainTabFocus(true);
            if(searchResultEntry.getObject() instanceof DbRelationship) {
                editor.getDbDetailView().currentDbRelationshipChanged(event);
                editor.getDbDetailView().repaint();
            } else {
                editor.getObjDetailView().currentObjRelationshipChanged(event);
                editor.getObjDetailView().repaint();
            }
        }
    }

    private static void jumpToEmbeddableAttributeResult(EmbeddableAttribute attribute, EditorView editor, DataChannelDescriptor domain) {
        Embeddable embeddable = attribute.getEmbeddable();
        DataMap map = embeddable.getDataMap();
        buildAndSelectTreePath(map, embeddable, editor);
        EmbeddableAttributeDisplayEvent event = new EmbeddableAttributeDisplayEvent(editor.getProjectTreeView(),
                embeddable, attribute, map, domain);
        event.setMainTabFocus(true);
        editor.getEmbeddableView().currentEmbeddableAttributeChanged(event);
        editor.getEmbeddableView().repaint();
    }

    private static void jumpToEmbeddableResult(Embeddable embeddable, EditorView editor, DataChannelDescriptor domain) {
        DataMap map = embeddable.getDataMap();
        buildAndSelectTreePath(map, embeddable, editor);
        EmbeddableDisplayEvent event = new EmbeddableDisplayEvent(editor.getProjectTreeView(), embeddable, map, domain);
        event.setMainTabFocus(true);
        editor.currentEmbeddableChanged(event);
    }

    private static void jumpToQueryResult(QueryDescriptor queryDescriptor, EditorView editor, DataChannelDescriptor domain) {
        DataMap map = queryDescriptor.getDataMap();
        buildAndSelectTreePath(map, queryDescriptor, editor);
        QueryDisplayEvent event = new QueryDisplayEvent(editor.getProjectTreeView(), queryDescriptor, map, domain);
        editor.currentQueryChanged(event);
    }

    private static void jumpToEntityResult(Entity entity, EditorView editor, DataChannelDescriptor domain) {
        DataMap map = entity.getDataMap();
        buildAndSelectTreePath(map, entity, editor);
        EntityDisplayEvent event = new EntityDisplayEvent(editor.getProjectTreeView(), entity, map, domain);
        event.setMainTabFocus(true);

        if (entity instanceof ObjEntity) {
            editor.getObjDetailView().currentObjEntityChanged(event);
        } else if (entity instanceof DbEntity) {
            editor.getDbDetailView().currentDbEntityChanged(event);
        }
    }

    private static void jumpToProcedureResult(Procedure procedure, EditorView editor, DataChannelDescriptor domain) {
        DataMap map = procedure.getDataMap();
        buildAndSelectTreePath(map, procedure, editor);
        ProcedureDisplayEvent event = new ProcedureDisplayEvent(editor.getProjectTreeView(), procedure, map, domain);
        editor.getProcedureView().currentProcedureChanged(event);
        editor.getProcedureView().repaint();
    }

    private static void jumpToProcedureResult(ProcedureParameter parameter, EditorView editor, DataChannelDescriptor domain) {
        Procedure procedure = parameter.getProcedure();
        DataMap map = procedure.getDataMap();
        buildAndSelectTreePath(map, procedure, editor);
        ProcedureParameterDisplayEvent event =
                new ProcedureParameterDisplayEvent(editor.getProjectTreeView(), parameter, procedure, map, domain);
        editor.getProcedureView().currentProcedureParameterChanged(event);
        editor.getProcedureView().repaint();
    }

    /**
     * Builds a tree path for a given path and make selection in it
     */
    private static TreePath buildAndSelectTreePath(DataMap map, Object object, EditorView editor) {
        ProjectTreeView projectTreeView = editor.getProjectTreeView();
        ProjectTreeModel treeModel = (ProjectTreeModel) projectTreeView.getModel();

        DefaultMutableTreeNode[] mutableTreeNodes = new DefaultMutableTreeNode[] {
            treeModel.getRootNode(),
            treeModel.getNodeForObjectPath(new Object[]{map}),
            treeModel.getNodeForObjectPath(new Object[]{map, object})
        };

        TreePath treePath = new TreePath(mutableTreeNodes);
        if (!projectTreeView.isExpanded(treePath.getParentPath())) {
            projectTreeView.expandPath(treePath.getParentPath());
        }
        projectTreeView.getSelectionModel().setSelectionPath(treePath);
        return treePath;
    }

    /**
     * Search result holder
     */
    public static class SearchResultEntry implements Comparable<SearchResultEntry>{
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
            if(priority == null) {
                throw new NullPointerException("Unknown type: " + object.getClass().getCanonicalName());
            }
            return priority;
        }

        @Override
        public int compareTo(SearchResultEntry o) {
            int res = getPriority() - o.getPriority();
            if(res != 0) {
                return res;
            }
            return getName().compareTo(o.getName());
        }
    }
}
