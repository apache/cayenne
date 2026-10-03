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

package org.apache.cayenne.modeler.ui.project.editor.query;

import java.awt.Component;
import java.awt.Container;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;

import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.combobox.CMUndoableComboBox;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.map.QueryDescriptor;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.cayenne.validation.ValidationException;

/**
 * A panel that supports editing the properties of a GenericSelectQuery.
 * 
 */
public abstract class SelectPropertiesPanel<Q extends QueryDescriptor> extends ProjectPanel {

    private static final String NO_CACHE_LABEL = "No Result Caching";
    private static final String LOCAL_CACHE_LABEL = "Local Cache (per ObjectContext)";
    private static final String SHARED_CACHE_LABEL = "Shared Cache";

    protected static final QueryCacheStrategy[] CACHE_POLICIES = new QueryCacheStrategy[] {
            QueryCacheStrategy.NO_CACHE,
            QueryCacheStrategy.LOCAL_CACHE,
            QueryCacheStrategy.SHARED_CACHE
    };

    private static final Map<QueryCacheStrategy, String> cachePolicyLabels = new TreeMap<>();

    static {
        cachePolicyLabels.put(QueryCacheStrategy.NO_CACHE,      NO_CACHE_LABEL);
        cachePolicyLabels.put(QueryCacheStrategy.LOCAL_CACHE,   LOCAL_CACHE_LABEL);
        cachePolicyLabels.put(QueryCacheStrategy.SHARED_CACHE,  SHARED_CACHE_LABEL);
    }

    protected CMUndoableTextField fetchOffset;
    protected CMUndoableTextField fetchLimit;
    protected CMUndoableTextField pageSize;
    protected JComboBox<QueryCacheStrategy> cacheStrategy;
    protected CMUndoableTextField cacheGroups;
    protected JComponent cacheGroupsLabel;

    private final Class<Q> queryType;

    public SelectPropertiesPanel(ProjectSession session, Class<Q> queryType) {
        super(session);
        this.queryType = queryType;
        initView();
        initController();
    }

    protected void initView() {
        fetchOffset = new CMUndoableTextField(session.app().getUndoManager(), 7);
        fetchOffset.addCommitListener(this::setFetchOffset);

        fetchLimit = new CMUndoableTextField(session.app().getUndoManager(), 7);
        fetchLimit.addCommitListener(this::setFetchLimit);

        pageSize = new CMUndoableTextField(session.app().getUndoManager(), 7);
        pageSize.addCommitListener(this::setPageSize);

        cacheStrategy = new CMUndoableComboBox<>(session.app().getUndoManager());
        cacheStrategy.setRenderer(new CacheStrategyRenderer());
        cacheGroups = new CMUndoableTextField(session.app().getUndoManager());
        cacheGroups.addCommitListener(this::setCacheGroups);
    }

    protected void initController() {
        cacheStrategy.addActionListener(event -> {
            QueryCacheStrategy strategy = (QueryCacheStrategy) cacheStrategy.getModel().getSelectedItem();
            setQueryProperty(QueryDescriptor::getCacheStrategy, QueryDescriptor::setCacheStrategy, strategy);
            setCacheGroupsEnabled(strategy != QueryCacheStrategy.NO_CACHE);
        });
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    public void initFromModel(Q query) {
        DefaultComboBoxModel<QueryCacheStrategy> cacheModel = new DefaultComboBoxModel<>(CACHE_POLICIES);

        QueryCacheStrategy selectedStrategy = query.getCacheStrategy();
        cacheModel.setSelectedItem(selectedStrategy);
        cacheStrategy.setModel(cacheModel);

        cacheGroups.setText(query.getCacheGroup());
        setCacheGroupsEnabled(selectedStrategy != QueryCacheStrategy.NO_CACHE);

        pageSize.setText(String.valueOf(query.getPageSize()));
    }

    void setFetchOffset(String string) {
        setFetchOffset(intValue(string, "Fetch offset"));
    }

    void setFetchLimit(String string) {
        setFetchLimit(intValue(string, "Fetch limit"));
    }

    void setPageSize(String string) {
        setQueryProperty(QueryDescriptor::getPageSize, QueryDescriptor::setPageSize, intValue(string, "Page size"));
    }

    void setCacheGroups(String string) {
        string = (string == null) ? "" : string.trim();
        setQueryProperty(QueryDescriptor::getCacheGroup, QueryDescriptor::setCacheGroup,
                string.isEmpty() ? null : string);
    }

    /**
     * Sets the fetch offset of the query. Does nothing by default, as not every type of query has an offset.
     */
    protected void setFetchOffset(int fetchOffset) {
    }

    /**
     * Sets the fetch limit of the query. Does nothing by default, as not every type of query has a limit.
     */
    protected void setFetchLimit(int fetchLimit) {
    }

    /**
     * Returns the selected query, or null if there's no selection, or the selected query is of a different type.
     */
    protected Q getQuery() {
        QueryDescriptor query = session.getSelectedQuery();
        return queryType.isInstance(query) ? queryType.cast(query) : null;
    }

    public void setEnabled(boolean flag) {
        super.setEnabled(flag);

        // propagate to children
        Container mainPanel = (Container) getComponent(0);
        Component[] children = mainPanel.getComponents();
        for (Component child : children) {
            child.setEnabled(flag);
        }
    }

    protected void setCacheGroupsEnabled(boolean enabled) {
        cacheGroups.setEnabled(enabled);
        cacheGroupsLabel.setEnabled(enabled);
    }

    /**
     * Changes a property of the query, and notifies the listeners, unless the property already has this value.
     */
    protected <T> void setQueryProperty(Function<? super Q, T> getter, BiConsumer<? super Q, T> setter, T value) {

        Q query = getQuery();
        if (query == null || Objects.equals(value, getter.apply(query))) {
            return;
        }

        setter.accept(query, value);
        session.fireQueryEvent(QueryEvent.ofChange(this, query));
    }

    private static int intValue(String string, String label) {
        string = (string == null) ? "" : string.trim();
        if (string.isEmpty()) {
            return 0;
        }

        if (!isNumeric(string)) {
            throw new ValidationException("%s must be an integer: %s", label, string);
        }

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new ValidationException("%s is too large: %s", label, string);
        }
    }

    private static boolean isNumeric(String s) {
        return s.chars().allMatch(Character::isDigit);
    }

    static final class CacheStrategyRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(
                JList list,
                Object object,
                int arg2,
                boolean arg3,
                boolean arg4) {

            if (object != null) {
                object = cachePolicyLabels.get((QueryCacheStrategy)object);
            }

            if (object == null) {
                object = NO_CACHE_LABEL;
            }

            return super.getListCellRendererComponent(list, object, arg2, arg3, arg4);
        }
    }

}
