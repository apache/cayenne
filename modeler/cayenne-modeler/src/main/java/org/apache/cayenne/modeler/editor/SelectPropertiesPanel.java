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

package org.apache.cayenne.modeler.editor;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A panel that supports editing the properties of a GenericSelectQuery.
 * 
 */
public abstract class SelectPropertiesPanel extends JPanel {

    private static Log logObj = LogFactory.getLog(SelectPropertiesPanel.class);

    private static final Integer ZERO = new Integer(0);

    private static final String NO_CACHE_LABEL = "No Result Caching";
    private static final String LOCAL_CACHE_LABEL = "Local Cache (per ObjectContext)";
    private static final String SHARED_CACHE_LABEL = "Shared Cache";

    protected static final Object[] CACHE_POLICIES = new Object[] {
            QueryCacheStrategy.NO_CACHE, QueryCacheStrategy.LOCAL_CACHE,
            QueryCacheStrategy.SHARED_CACHE
    };

    private static final Map<QueryCacheStrategy, String> cachePolicyLabels = new TreeMap<QueryCacheStrategy, String>();

    static {
        cachePolicyLabels.put(QueryCacheStrategy.NO_CACHE, NO_CACHE_LABEL);
        cachePolicyLabels.put(QueryCacheStrategy.LOCAL_CACHE, LOCAL_CACHE_LABEL);
        cachePolicyLabels.put(QueryCacheStrategy.SHARED_CACHE, SHARED_CACHE_LABEL);
    }

    protected TextAdapter fetchOffset;
    protected TextAdapter fetchLimit;
    protected TextAdapter pageSize;
    protected JComboBox cacheStrategy;
    protected TextAdapter cacheGroups;
    protected JComponent cacheGroupsLabel;

    protected ProjectController mediator;

    public SelectPropertiesPanel(ProjectController mediator) {
        this.mediator = mediator;
        initView();
        initController();
    }

    protected void initView() {
        fetchOffset = new TextAdapter(new JTextField(7)) {

            protected void updateModel(String text) {
                setFetchOffset(text);
            }
        };

        fetchLimit = new TextAdapter(new JTextField(7)) {

            protected void updateModel(String text) {
                setFetchLimit(text);
            }
        };

        pageSize = new TextAdapter(new JTextField(7)) {

            protected void updateModel(String text) {
                setPageSize(text);
            }
        };

        cacheStrategy = Application.getWidgetFactory().createUndoableComboBox();
        cacheStrategy.setRenderer(new CacheStrategyRenderer());
        cacheGroups = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setCacheGroups(text);
            }
        };
    }

    protected void initController() {
        cacheStrategy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                QueryCacheStrategy strategy = (QueryCacheStrategy) cacheStrategy
                        .getModel()
                        .getSelectedItem();
                setQueryProperty(QueryMetadata.CACHE_STRATEGY_PROPERTY, strategy.name());
                setCacheGroupsEnabled(strategy != null
                        && strategy != QueryCacheStrategy.NO_CACHE);
            }
        });
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    public void initFromModel(QueryDescriptor query) {
        DefaultComboBoxModel cacheModel = new DefaultComboBoxModel(CACHE_POLICIES);

        String selectedStrategyString = query.getProperty(QueryMetadata.CACHE_STRATEGY_PROPERTY);

        QueryCacheStrategy selectedStrategy = selectedStrategyString != null ?
                QueryCacheStrategy.valueOf(selectedStrategyString) : null;

        cacheModel.setSelectedItem(selectedStrategy != null ?
                selectedStrategy : QueryCacheStrategy.getDefaultStrategy());

        cacheStrategy.setModel(cacheModel);

        cacheGroups.setText(query.getProperty(QueryMetadata.CACHE_GROUPS_PROPERTY));
        setCacheGroupsEnabled(selectedStrategy != null
                && selectedStrategy != QueryCacheStrategy.NO_CACHE);

        String fetchOffsetStr = query.getProperty(QueryMetadata.FETCH_OFFSET_PROPERTY);
        String fetchLimitStr = query.getProperty(QueryMetadata.FETCH_LIMIT_PROPERTY);
        String pageSizeStr = query.getProperty(QueryMetadata.PAGE_SIZE_PROPERTY);

        fetchOffset.setText(fetchOffsetStr != null ? fetchOffsetStr : ZERO.toString());
        fetchLimit.setText(fetchLimitStr != null ? fetchLimitStr : ZERO.toString());
        pageSize.setText(pageSizeStr != null ? pageSizeStr : ZERO.toString());
    }

    protected String toCacheGroupsString(String[] groups) {

        StringBuilder buffer = new StringBuilder();
        if (groups != null && groups.length > 0) {

            for (int i = 0; i < groups.length; i++) {
                if (i > 0) {
                    buffer.append(", ");
                }

                buffer.append(groups[i]);
            }
        }

        return buffer.toString();
    }

    void setFetchOffset(String string) {
        string = (string == null) ? "" : string.trim();

        if (string.length() == 0) {
            setQueryProperty(QueryMetadata.FETCH_OFFSET_PROPERTY, ZERO.toString());
        }
        else {
            if (StringUtils.isNumeric(string)) {
                setQueryProperty(QueryMetadata.FETCH_OFFSET_PROPERTY, string);
            }
            else {
                throw new ValidationException("Fetch offset must be an integer: "
                        + string);
            }
        }
    }

    void setFetchLimit(String string) {
        string = (string == null) ? "" : string.trim();

        if (string.length() == 0) {
            setQueryProperty(QueryMetadata.FETCH_LIMIT_PROPERTY, ZERO.toString());
        }
        else {
            if (StringUtils.isNumeric(string)) {
                setQueryProperty(QueryMetadata.FETCH_LIMIT_PROPERTY, string);
            }
            else {
                throw new ValidationException("Fetch limit must be an integer: " + string);
            }
        }
    }

    void setPageSize(String string) {
        string = (string == null) ? "" : string.trim();

        if (string.length() == 0) {
            setQueryProperty(QueryMetadata.PAGE_SIZE_PROPERTY, ZERO.toString());
        }
        else {
            if (StringUtils.isNumeric(string)) {
                setQueryProperty(QueryMetadata.PAGE_SIZE_PROPERTY, string);
            }
            else {
                throw new ValidationException("Page size must be an integer: " + string);
            }
        }
    }

    void setCacheGroups(String string) {
        string = (string == null) ? "" : string.trim();
        setQueryProperty(QueryMetadata.CACHE_GROUPS_PROPERTY, string);
    }

    QueryDescriptor getQuery() {
        return mediator.getCurrentQuery();
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
        cacheGroups.getComponent().setEnabled(enabled);
        cacheGroupsLabel.setEnabled(enabled);
    }

    void setQueryProperty(String property, String value) {
        QueryDescriptor query = getQuery();
        if (query != null) {
            try {
                Object old = query.getProperty(property);
                if (Util.nullSafeEquals(value, old)) {
                    return;
                }
                query.setProperty(property, value);
                mediator.fireQueryEvent(new QueryEvent(this, query));
            }
            catch (Exception ex) {
                logObj.warn("Error setting property: " + property, ex);
            }
        }
    }

    final class CacheStrategyRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(
                JList list,
                Object object,
                int arg2,
                boolean arg3,
                boolean arg4) {

            if (object != null) {
                object = cachePolicyLabels.get(object);
            }

            if (object == null) {
                object = NO_CACHE_LABEL;
            }

            return super.getListCellRendererComponent(list, object, arg2, arg3, arg4);
        }
    }

}
