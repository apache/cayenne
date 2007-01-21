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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.validation.ValidationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A panel that supports editing the properties of a GenericSelectQuery.
 * 
 * @author Andrus Adamchik
 */
public abstract class SelectPropertiesPanel extends JPanel {

    private static Log logObj = LogFactory.getLog(SelectPropertiesPanel.class);

    private static final Integer ZERO = new Integer(0);

    private static final String NO_CACHE_LABEL = "No Result Caching";
    private static final String LOCAL_CACHE_LABEL = "DataContext Cache";
    private static final String SHARED_CACHE_LABEL = "Shared Cache";

    private static final Object[] CACHE_POLICIES = new Object[] {
            QueryMetadata.NO_CACHE, QueryMetadata.LOCAL_CACHE, QueryMetadata.SHARED_CACHE
    };

    private static final Map cachePolicyLabels = new TreeMap();

    static {
        cachePolicyLabels.put(QueryMetadata.NO_CACHE, NO_CACHE_LABEL);
        cachePolicyLabels.put(QueryMetadata.LOCAL_CACHE, LOCAL_CACHE_LABEL);
        cachePolicyLabels.put(QueryMetadata.SHARED_CACHE, SHARED_CACHE_LABEL);
    }

    protected TextAdapter fetchLimit;
    protected TextAdapter pageSize;
    protected JComboBox cachePolicy;
    protected JCheckBox refreshesResults;

    protected ProjectController mediator;

    public SelectPropertiesPanel(ProjectController mediator) {
        this.mediator = mediator;
        initView();
        initController();
    }

    protected void initView() {
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

        cachePolicy = CayenneWidgetFactory.createComboBox();
        cachePolicy.setRenderer(new CachePolicyRenderer());
        refreshesResults = new JCheckBox();
    }

    protected void initController() {
        cachePolicy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Object policy = cachePolicy.getModel().getSelectedItem();
                setQueryProperty("cachePolicy", policy);
            }
        });

        refreshesResults.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Boolean b = refreshesResults.isSelected() ? Boolean.TRUE : Boolean.FALSE;
                setQueryProperty("refreshingObjects", b);
            }
        });
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    public void initFromModel(Query query) {
        EntityResolver resolver = mediator.getCurrentDataDomain().getEntityResolver();
        DefaultComboBoxModel cacheModel = new DefaultComboBoxModel(CACHE_POLICIES);
        cacheModel.setSelectedItem(query.getMetaData(resolver).getCachePolicy());
        cachePolicy.setModel(cacheModel);

        fetchLimit.setText(String.valueOf(query.getMetaData(resolver).getFetchLimit()));
        pageSize.setText(String.valueOf(query.getMetaData(resolver).getPageSize()));
        refreshesResults.setSelected(query.getMetaData(resolver).isRefreshingObjects());
    }

    void setFetchLimit(String string) {
        string = (string == null) ? "" : string.trim();

        if (string.length() == 0) {
            setQueryProperty("fetchLimit", ZERO);
        }
        else {
            try {
                setQueryProperty("fetchLimit", new Integer(string));
            }
            catch (NumberFormatException nfex) {
                throw new ValidationException("Fetch limit must be an integer: " + string);
            }
        }
    }

    void setPageSize(String string) {
        string = (string == null) ? "" : string.trim();

        if (string.length() == 0) {
            setQueryProperty("pageSize", ZERO);
        }
        else {
            try {
                setQueryProperty("pageSize", new Integer(string));
            }
            catch (NumberFormatException nfex) {
                throw new ValidationException("Page size must be an integer: " + string);
            }
        }
    }

    Query getQuery() {
        return mediator.getCurrentQuery();
    }

    public void setEnabled(boolean flag) {
        super.setEnabled(flag);

        // propagate to children

        Container mainPanel = (Container) getComponent(0);
        Component[] children = mainPanel.getComponents();
        for (int i = 0; i < children.length; i++) {
            children[i].setEnabled(flag);
        }
    }

    void setQueryProperty(String property, Object value) {
        Query query = getQuery();
        if (query != null) {
            try {
                PropertyUtils.setProperty(query, property, value);
                mediator.fireQueryEvent(new QueryEvent(this, query));
            }
            catch (Exception ex) {
                logObj.warn("Error setting property: " + property, ex);
            }
        }
    }

    final class CachePolicyRenderer extends DefaultListCellRenderer {

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
