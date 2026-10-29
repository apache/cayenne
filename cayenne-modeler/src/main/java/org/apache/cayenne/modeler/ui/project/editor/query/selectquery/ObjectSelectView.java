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
package org.apache.cayenne.modeler.ui.project.editor.query.selectquery;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.query.ObjectQueryPropertiesPanel;
import org.apache.cayenne.modeler.ui.project.editor.query.TextQueryView;

/**
 * An editor of an ObjectSelect query: its name, its query String, and the properties that are not a part of the String.
 */
public class ObjectSelectView extends TextQueryView<SelectQueryDescriptor> {

    // the first "from" clause, outside of the quoted string literals, and the entity name that follows it, if any
    private static final Pattern FROM = Pattern.compile(
            "'(?:[^'\\\\]|\\\\.)*'|\"(?:[^\"\\\\]|\\\\.)*\"|(?<![\\w$.])from(?![\\w$.])\\s*(db:\\s*)?([\\w$]*)",
            Pattern.DOTALL);

    private final QLEditor editor;
    private final QLStatusLine statusLine;
    private final QLPathBrowser pathBrowser;
    private final ObjectQueryPropertiesPanel properties;

    // the query that the path browser was last populated for
    private SelectQueryDescriptor browserQuery;

    public ObjectSelectView(ProjectSession session) {
        super(session);
        this.editor = new QLEditor(session, this::queryChecked);
        this.statusLine = new QLStatusLine();
        this.pathBrowser = new QLPathBrowser(editor::insert);
        this.properties = new ObjectQueryPropertiesPanel(session);
        initLayout(editor, statusLine, pathBrowser, "objectSelect/editorSplit", properties);
        initBindings();
    }

    private void initBindings() {
        session.addQueryDisplayListener(e -> initFromModel());
    }

    private void initFromModel() {
        SelectQueryDescriptor query = getQuery();
        if (query == null) {
            editor.setQuery(null);
            statusLine.update(null, List.of());
            pathBrowser.setEntity(null);
            browserQuery = null;
            return;
        }

        initNameAndComment(query);
        properties.initFromModel(query);
        editor.setQuery(query);

        // a tree kept from another query would insert paths of a wrong entity, so a different query starts with its
        // own root, or with no tree if its String can't be parsed
        if (query != browserQuery) {
            browserQuery = query;
            pathBrowser.setEntity(rootEntity(query));
        }

        // the entity may have changed since the tree was built
        pathBrowser.refresh();
    }

    // a reaction to each check of the query String by the editor
    private void queryChecked(List<String> problems) {
        SelectQueryDescriptor query = getQuery();
        statusLine.update(query, problems);
        if (query != null) {
            if (query != browserQuery || query.isValid()) {
                browserQuery = query;
                pathBrowser.setEntity(rootEntity(query));
            } else {
                RootRef root = rootRef(query.getQl());
                if (root == null) {
                    pathBrowser.setEntity(null);
                } else {
                    Entity<?, ?, ?> entity = root.resolve(query.getDataMap());
                    if (entity != null) {
                        pathBrowser.setEntity(entity);
                    }
                }
            }
            properties.updateDataRowsEnabled(query);
        }
    }

    private static Entity<?, ?, ?> rootEntity(SelectQueryDescriptor query) {
        return query.getRoot() instanceof Entity<?, ?, ?> entity ? entity : null;
    }

    /**
     * Returns the entity named in the first "from" clause of a query String, that may not be parseable, or null if
     * there's no "from" clause, or no name in it.
     */
    static RootRef rootRef(String ql) {
        if (ql == null) {
            return null;
        }

        Matcher matcher = FROM.matcher(ql);
        while (matcher.find()) {
            // a match with no groups is a string literal
            if (matcher.group(2) != null) {
                return matcher.group(2).isEmpty() ? null : new RootRef(matcher.group(2), matcher.group(1) != null);
            }
        }
        return null;
    }

    /**
     * A name of the root entity of a query, either an ObjEntity, or a DbEntity.
     */
    record RootRef(String name, boolean db) {

        Entity<?, ?, ?> resolve(DataMap dataMap) {
            if (dataMap == null) {
                return null;
            }
            return db ? dataMap.getDbEntity(name) : dataMap.getObjEntity(name);
        }
    }

    @Override
    protected SelectQueryDescriptor getQuery() {
        return session.getSelectedQuery() instanceof SelectQueryDescriptor query ? query : null;
    }
}
