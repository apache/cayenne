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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.ExpressionConvertor;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.query.AbstractQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A tabbed pane that contains editors for various SelectQuery parts.
 * 
 * @author Andrus Adamchik
 */
public class SelectQueryMainTab extends JPanel {

    protected ProjectController mediator;

    protected TextAdapter name;
    protected JComboBox queryRoot;
    protected TextAdapter qualifier;
    protected JCheckBox distinct;
    protected ObjectQueryPropertiesPanel properties;

    public SelectQueryMainTab(ProjectController mediator) {
        this.mediator = mediator;

        initView();
        initController();
    }

    private void initView() {
        // create widgets
        name = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setQueryName(text);
            }
        };

        queryRoot = CayenneWidgetFactory.createComboBox();
        queryRoot.setRenderer(CellRenderers.listRendererWithIcons());

        qualifier = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setQueryQualifier(text);
            }
        };

        distinct = new JCheckBox();

        properties = new ObjectQueryPropertiesPanel(mediator);

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, fill:200dlu",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("SelectQuery Settings", cc.xywh(1, 1, 3, 1));
        builder.addLabel("Query Name:", cc.xy(1, 3));
        builder.add(name.getComponent(), cc.xy(3, 3));
        builder.addLabel("Query Root:", cc.xy(1, 5));
        builder.add(queryRoot, cc.xy(3, 5));
        builder.addLabel("Qualifier:", cc.xy(1, 7));
        builder.add(qualifier.getComponent(), cc.xy(3, 7));
        builder.addLabel("Distinct:", cc.xy(1, 9));
        builder.add(distinct, cc.xy(3, 9));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(properties, BorderLayout.CENTER);
    }

    private void initController() {

        queryRoot.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                SelectQuery query = getQuery();
                if (query != null) {
                    query.setRoot(queryRoot.getModel().getSelectedItem());
                    mediator.fireQueryEvent(new QueryEvent(this, query));
                }
            }
        });

        distinct.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                SelectQuery query = getQuery();
                if (query != null) {
                    query.setDistinct(distinct.isSelected());
                    mediator.fireQueryEvent(new QueryEvent(this, query));
                }
            }
        });

    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    void initFromModel() {
        Query query = mediator.getCurrentQuery();

        if (!(query instanceof SelectQuery)) {
            setVisible(false);
            return;
        }

        SelectQuery selectQuery = (SelectQuery) query;

        name.setText(query.getName());
        distinct.setSelected(selectQuery.isDistinct());
        qualifier.setText(selectQuery.getQualifier() != null ? selectQuery
                .getQualifier()
                .toString() : null);

        // init root choices and title label..

        // - SelectQuery supports ObjEntity roots

        // TODO: now we only allow roots from the current map,
        // since query root is fully resolved during map loading,
        // making it impossible to reference other DataMaps.

        DataMap map = mediator.getCurrentDataMap();
        Object[] roots = map.getObjEntities().toArray();

        if (roots.length > 1) {
            Arrays.sort(roots, Comparators.getDataMapChildrenComparator());
        }

        DefaultComboBoxModel model = new DefaultComboBoxModel(roots);
        model.setSelectedItem(selectQuery.getRoot());
        queryRoot.setModel(model);

        properties.initFromModel(selectQuery);

        setVisible(true);
    }

    protected SelectQuery getQuery() {
        return (mediator.getCurrentQuery() instanceof SelectQuery)
                ? (SelectQuery) mediator.getCurrentQuery()
                : null;
    }

    /**
     * Initializes Query qualifier from string.
     */
    void setQueryQualifier(String text) {
        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        SelectQuery query = getQuery();
        if (query == null) {
            return;
        }

        ExpressionConvertor convertor = new ExpressionConvertor();
        try {
            String oldQualifier = convertor.valueAsString(query.getQualifier());
            if (!Util.nullSafeEquals(oldQualifier, text)) {
                Expression exp = (Expression) convertor.stringAsValue(text);
                query.setQualifier(exp);
                mediator.fireQueryEvent(new QueryEvent(this, query));
            }
        }
        catch (IllegalArgumentException ex) {
            // unparsable qualifier
            throw new ValidationException(ex.getMessage());
        }
    }

    /**
     * Initializes Query name from string.
     */
    void setQueryName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        AbstractQuery query = getQuery();

        if (query == null) {
            return;
        }

        if (Util.nullSafeEquals(newName, query.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("SelectQuery name is required.");
        }

        DataMap map = mediator.getCurrentDataMap();
        Query matchingQuery = map.getQuery(newName);

        if (matchingQuery == null) {
            // completely new name, set new name for entity
            QueryEvent e = new QueryEvent(this, query, query.getName());
            ProjectUtil.setQueryName(map, query, newName);
            mediator.fireQueryEvent(e);
        }
        else if (matchingQuery != query) {
            // there is a query with the same name
            throw new ValidationException("There is another query named '"
                    + newName
                    + "'. Use a different name.");
        }
    }
}
