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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.ExpressionConvertor;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.modeler.util.ValidatorTextAdapter;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.query.AbstractQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A tabbed pane that contains editors for various SelectQuery parts.
 * 
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

            @Override
            protected void updateModel(String text) {
                setQueryName(text);
            }
        };

        queryRoot = CayenneWidgetFactory.createComboBox();
        AutoCompletion.enable(queryRoot);
        queryRoot.setRenderer(CellRenderers.listRendererWithIcons());

        qualifier = new ValidatorTextAdapter(new JTextField()) {

            @Override
            protected void updateModel(String text) {
                setQueryQualifier(text);
            }

            @Override
            protected void validate(String text) throws ValidationException {
                createQualifier(text);
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
        RootSelectionHandler rootHandler = new RootSelectionHandler();
        
        queryRoot.addActionListener(rootHandler);
        queryRoot.addFocusListener(rootHandler);
        queryRoot.getEditor().getEditorComponent().addFocusListener(rootHandler);

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

        Expression qualifier = createQualifier(text);
        if (qualifier != null)
        {
            //getQuery() is not null if we reached here
            getQuery().setQualifier(qualifier);
            mediator.fireQueryEvent(new QueryEvent(this, getQuery()));
        }
        
    }
    
    /**
     * Method to create and check an expression
     * @param text String to be converted as Expression
     * @return Expression if a new expression was created, null otherwise.
     * @throws ValidationException if <code>text</code> can't be converted  
     */
    Expression createQualifier(String text) throws ValidationException
    {
        SelectQuery query = getQuery();
        if (query == null) {
            return null;
        }
        
        ExpressionConvertor convertor = new ExpressionConvertor();
        try {
            String oldQualifier = convertor.valueAsString(query.getQualifier());
            if (!Util.nullSafeEquals(oldQualifier, text)) {
                Expression exp = (Expression) convertor.stringAsValue(text);
                
                /**
                 * Advanced checking. See CAY-888 #1
                 */
                if (query.getRoot() instanceof Entity) {
                    checkExpression((Entity) query.getRoot(), exp);
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
    static void checkExpression(Entity root, Expression ex) throws ValidationException {
        try {
            if (ex instanceof ASTPath) {
                /**
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
    
    /**
     * Handler to user's actions with root selection combobox
     */
    class RootSelectionHandler implements FocusListener, ActionListener {
        String newName = null;
        boolean needChangeName;

        public void actionPerformed(ActionEvent ae) {
            SelectQuery query = getQuery();
            if (query != null) {
                Entity root = (Entity) queryRoot.getModel().getSelectedItem();

                if (root != null) {
                    query.setRoot(root);
                    
                    if (needChangeName) { //not changed by user
                        /**
                         * Doing auto name change, following CAY-888 #2
                         */
                        String newPrefix = root.getName() + "Query";
                        newName = newPrefix;
                        
                        DataMap map = mediator.getCurrentDataMap();
                        long postfix = 1;
                        
                        while (map.getQuery(newName) != null) {
                            newName = newPrefix + (postfix++);
                        }
                        
                        name.setText(newName);
                    }
                }
            }
        }

        public void focusGained(FocusEvent e) {
            //reset new name tracking
            newName = null;
            
            SelectQuery query = getQuery();
            if (query != null) {
                needChangeName = hasDefaultName(query);
            }
            else {
                needChangeName = false;
            }
        }

        public void focusLost(FocusEvent e) {
            if (newName != null) {
                setQueryName(newName);
            }
            
            newName = null;
            needChangeName = false;
        }

        /**
         * @return whether specified's query name is 'default' i.e. Cayenne generated
         * A query's name is 'default' if it starts with 'UntitledQuery' or with root name.
         * 
         * We cannot follow user input because tab might be opened many times
         */
        boolean hasDefaultName(SelectQuery query) {
            String prefix = query.getRoot() == null ? "UntitledQuery" :
                CellRenderers.asString(query.getRoot()) + "Query";
            
            return name.getComponent().getText().startsWith(prefix);
        }
    }
}
