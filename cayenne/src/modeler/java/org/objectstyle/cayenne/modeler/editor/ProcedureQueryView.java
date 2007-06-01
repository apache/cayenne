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

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.event.QueryDisplayEvent;
import org.objectstyle.cayenne.modeler.event.QueryDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.Comparators;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.modeler.util.TextAdapter;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Andrei Adamchik
 */
public class ProcedureQueryView extends JPanel {

    protected ProjectController mediator;
    protected TextAdapter name;
    protected JComboBox queryRoot;
    protected JCheckBox selecting;
    protected SelectPropertiesPanel properties;

    public ProcedureQueryView(ProjectController mediator) {
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
        selecting = new JCheckBox();
        queryRoot = CayenneWidgetFactory.createComboBox();
        queryRoot.setRenderer(CellRenderers.listRendererWithIcons());
        properties = new RawQueryPropertiesPanel(mediator) {

            protected void setEntity(ObjEntity entity) {
                ProcedureQueryView.this.setEntity(entity);
            }

            public ObjEntity getEntity(GenericSelectQuery query) {
                if (query instanceof ProcedureQuery) {
                    return ProcedureQueryView.this.getEntity((ProcedureQuery) query);
                }

                return null;
            }
        };

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, fill:max(200dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("ProcedureQuery Settings", cc.xywh(1, 1, 3, 1));
        builder.addLabel("Query Name:", cc.xy(1, 3));
        builder.add(name.getComponent(), cc.xy(3, 3));
        builder.addLabel("Procedure:", cc.xy(1, 5));
        builder.add(queryRoot, cc.xy(3, 5));
        builder.addLabel("Is Selecting:", cc.xy(1, 7));
        builder.add(selecting, cc.xy(3, 7));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(properties, BorderLayout.CENTER);
    }

    private void initController() {

        queryRoot.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Query query = mediator.getCurrentQuery();
                if (query != null) {
                    query.setRoot(queryRoot.getModel().getSelectedItem());
                    mediator.fireQueryEvent(new QueryEvent(this, query));
                }
            }
        });

        mediator.addQueryDisplayListener(new QueryDisplayListener() {

            public void currentQueryChanged(QueryDisplayEvent e) {
                initFromModel();
            }
        });

        selecting.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                properties.setEnabled(selecting.isSelected());

                Query query = mediator.getCurrentQuery();
                if (query instanceof ProcedureQuery) {
                    ((ProcedureQuery) query).setSelecting(selecting.isSelected());
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

        if (!(query instanceof ProcedureQuery)) {
            setVisible(false);
            return;
        }

        ProcedureQuery procedureQuery = (ProcedureQuery) query;

        selecting.setSelected(procedureQuery.isSelecting());
        properties.setEnabled(procedureQuery.isSelecting());
        name.setText(procedureQuery.getName());

        // init root choices and title label..

        // - ProcedureQuery supports Procedure roots

        // TODO: now we only allow roots from the current map,
        // since query root is fully resolved during map loading,
        // making it impossible to reference other DataMaps.

        DataMap map = mediator.getCurrentDataMap();
        Object[] roots = map.getProcedures().toArray();

        if (roots.length > 1) {
            Arrays.sort(roots, Comparators.getDataMapChildrenComparator());
        }

        DefaultComboBoxModel model = new DefaultComboBoxModel(roots);
        model.setSelectedItem(query.getRoot());
        queryRoot.setModel(model);

        properties.initFromModel(procedureQuery);
        setVisible(true);
    }

    /**
     * Initializes Query name from string.
     */
    void setQueryName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        Query query = mediator.getCurrentQuery();
        if (query == null) {
            return;
        }

        if (Util.nullSafeEquals(newName, query.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Query name is required.");
        }

        DataMap map = mediator.getCurrentDataMap();

        if (map.getQuery(newName) == null) {
            // completely new name, set new name for entity
            QueryEvent e = new QueryEvent(this, query, query.getName());
            ProjectUtil.setQueryName(map, query, newName);
            mediator.fireQueryEvent(e);
        }
        else {
            // there is a query with the same name
            throw new ValidationException("There is another query named '"
                    + newName
                    + "'. Use a different name.");
        }
    }

    /**
     * Returns an entity that maps to a procedure query result class.
     */
    ObjEntity getEntity(ProcedureQuery query) {
        String resultClass = query.getResultClassName();
        if (resultClass == null) {
            return null;
        }

        // currently entity resolver doesn't support lookup by class name
        // we will have to do manual ObjEntity scan...
        // TODO: replace this once such indexing is supported

        DataMap map = mediator.getCurrentDataMap();
        if (map == null) {
            return null;
        }

        return map.getObjEntityForJavaClass(resultClass);
    }

    void setEntity(ObjEntity entity) {
        Query query = mediator.getCurrentQuery();
        if (query instanceof ProcedureQuery) {
            ProcedureQuery procedureQuery = (ProcedureQuery) query;
            String resultClass = null;
            
            if (entity != null) {
                resultClass = entity.getClassName();
            }
            
            procedureQuery.setResultClassName(resultClass);
            mediator.fireQueryEvent(new QueryEvent(this, procedureQuery));
        }
    }
}