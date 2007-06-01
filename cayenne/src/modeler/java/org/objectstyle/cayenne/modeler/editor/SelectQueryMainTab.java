/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.Comparators;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.modeler.util.TextAdapter;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.ValidationException;
import org.scopemvc.util.convertor.StringConvertor;
import org.scopemvc.util.convertor.StringConvertors;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A tabbed pane that contains editors for various SelectQuery parts.
 * 
 * @author Andrei Adamchik
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
                "right:max(80dlu;pref), 3dlu, fill:max(200dlu;pref)",
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
                Query query = getQuery();
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
        model.setSelectedItem(query.getRoot());
        queryRoot.setModel(model);

        properties.initFromModel(selectQuery);

        setVisible(true);
    }

    protected SelectQuery getQuery() {
        return (SelectQuery) mediator.getCurrentQuery();
    }

    /**
     * Initializes Query qualifier from string.
     */
    void setQueryQualifier(String text) {
        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        SelectQuery query = getQuery();
        if(query == null) {
            return;
        }
        
        StringConvertor convertor = StringConvertors.forClass(Expression.class);
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

        Query query = getQuery();
 
        if(query == null) {
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