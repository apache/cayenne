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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.swing.checkbox.CayenneCheckBox;
import org.apache.cayenne.modeler.swing.text.CayenneUndoableTextField;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.project.editor.ExpressionConvertor;
import org.apache.cayenne.modeler.ui.project.editor.query.BaseQueryMainTab;
import org.apache.cayenne.modeler.ui.project.editor.query.ObjectQueryPropertiesPanel;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A tabbed pane that contains editors for various SelectQuery parts.
 * 
 */
public class SelectQueryMainTab extends BaseQueryMainTab {

    protected CayenneUndoableTextField comment;
    protected CayenneUndoableTextField qualifier;
    protected JCheckBox distinct;
    protected ObjectQueryPropertiesPanel properties;

    public SelectQueryMainTab(ProjectController mediator) {
        super(mediator);

        initQueryRoot();
        initView();
        initController();
    }

    private void initView() {
        // create widgets
        name = new CayenneUndoableTextField();
        name.addCommitListener(this::setQueryName);

        qualifier = new CayenneUndoableTextField();
        qualifier.addCommitListener(this::setQueryQualifier);

        comment = new CayenneUndoableTextField();
        comment.addCommitListener(this::setQueryComment);

        distinct = new CayenneCheckBox();

        properties = new ObjectQueryPropertiesPanel(controller);

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, fill:200dlu",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("SelectQuery Settings", cc.xywh(1, 1, 3, 1));
        builder.addLabel("Query Name:", cc.xy(1, 3));
        builder.add(name, cc.xy(3, 3));
        builder.addLabel("Query Root:", cc.xy(1, 5));
        builder.add(queryRoot, cc.xy(3, 5));
        builder.addLabel("Qualifier:", cc.xy(1, 7));
        builder.add(qualifier, cc.xy(3, 7));
        builder.addLabel("Distinct:", cc.xy(1, 9));
        builder.add(distinct, cc.xy(3, 9));
        builder.addLabel("Comment:", cc.xy(1, 11));
        builder.add(comment, cc.xy(3, 11));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(properties, BorderLayout.CENTER);
    }

    private void initController() {
        distinct.addItemListener(e -> {
            QueryDescriptor query = getQuery();
            if (query != null) {
                query.setProperty(SelectQueryDescriptor.DISTINCT_PROPERTY, Boolean.toString(distinct.isSelected()));
                controller.fireQueryEvent(QueryEvent.ofChange(this, query));
            }
        });
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    void initFromModel() {
        QueryDescriptor descriptor = controller.getSelectedQuery();

        if (descriptor == null || !QueryDescriptor.SELECT_QUERY.equals(descriptor.getType())) {
            setVisible(false);
            return;
        }

        SelectQueryDescriptor query = (SelectQueryDescriptor) descriptor;

        name.setText(query.getName());
        distinct.setSelected(Boolean.parseBoolean(query.getProperties().get(SelectQueryDescriptor.DISTINCT_PROPERTY)));
        qualifier.setText(query.getQualifier() != null ? query
                .getQualifier()
                .toString() : null);
        comment.setText(getQueryComment(query));

        // init root choices and title label..

        // - SelectQuery supports ObjEntity roots

        // TODO: now we only allow roots from the current map,
        // since query root is fully resolved during map loading,
        // making it impossible to reference other DataMaps.

        DataMap map = controller.getSelectedDataMap();
        ObjEntity[] roots = map.getObjEntities().toArray(new ObjEntity[0]);

        if (roots.length > 1) {
            Arrays.sort(roots, Comparators.forDataMapChildren());
        }

        DefaultComboBoxModel<ObjEntity> model = new DefaultComboBoxModel<>(roots);
        model.setSelectedItem(query.getRoot());
        queryRoot.setModel(model);

        properties.initFromModel(query);

        setVisible(true);
    }

    @Override
    protected SelectQueryDescriptor getQuery() {
        if(controller.getSelectedQuery() == null) {
            return null;
        }
        return QueryDescriptor.SELECT_QUERY.equals(controller.getSelectedQuery().getType())
                ? (SelectQueryDescriptor) controller.getSelectedQuery()
                : null;
    }

    void setQueryQualifier(String text) {
        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        getQuery().setQualifier(createQualifier(text));
        controller.fireQueryEvent(QueryEvent.ofChange(this, getQuery()));
    }

    /**
     * Method to create and check an expression
     * @param text String to be converted as Expression
     * @return Expression if a new expression was created, null otherwise.
     * @throws ValidationException if <code>text</code> can't be converted
     */
    Expression createQualifier(String text) throws ValidationException
    {
        SelectQueryDescriptor query = getQuery();
        if (query == null) {
            return null;
        }

        try {
            String oldQualifier = ExpressionConvertor.asString(query.getQualifier());
            if (!Util.nullSafeEquals(oldQualifier, text)) {
                Expression exp = ExpressionConvertor.fromString(text);

                /*
                 * Advanced checking. See CAY-888 #1
                 */
                if (query.getRoot() instanceof Entity) {
                    checkExpression((Entity<?,?,?>) query.getRoot(), exp);
                }

                return exp;
            }

            return null;
        }
        catch (IllegalArgumentException ex) {
            // unparsable qualifier
            throw new ValidationException(ex.getMessage());
        }
    }

    /**
     * Advanced checking of an expression, needed because Expression.fromString()
     * might terminate normally, but returned Expression will not be appliable
     * for real Entities.
     * Current implementation assures all attributes in expression are present in
     * Entity
     * @param root Root of a query
     * @param ex Expression to check
     * @throws ValidationException when something's wrong
     */
    static void checkExpression(Entity<?,?,?> root, Expression ex) throws ValidationException {
        try {
            if (ex instanceof ASTPath) {
                /*
                 * Try to iterate through path, if some attributes are not present,
                 * exception will be raised
                 */

                Iterator<CayenneMapEntry> path = root.resolvePathComponents(ex);
                while (path.hasNext()) {
                    path.next();
                }
            }

            if (ex != null) {
                for (int i = 0; i < ex.getOperandCount(); i++) {
                    if (ex.getOperand(i) instanceof Expression) {
                        checkExpression(root, (Expression)ex.getOperand(i));
                    }
                }
            }
        }
        catch (ExpressionException eex) {
            throw new ValidationException(eex.getUnlabeledMessage());
        }
    }

    private void setQueryComment(String text) {
        QueryDescriptor query = getQuery();
        if (query == null) {
            return;
        }
        ObjectInfo.putToMetaData(controller.getApplication().getMetaData(), query, ObjectInfo.COMMENT, text);
        controller.fireQueryEvent(QueryEvent.ofChange(this, query));
    }

    private String getQueryComment(QueryDescriptor queryDescriptor) {
        return ObjectInfo.getFromMetaData(controller.getApplication().getMetaData(), queryDescriptor, ObjectInfo.COMMENT);
    }
}
