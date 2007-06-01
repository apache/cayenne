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

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.modeler.util.TextAdapter;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A main panel for editing a SQLTemplate.
 * 
 * @author Andrei Adamchik
 */
public class SQLTemplateMainTab extends JPanel {

    protected ProjectController mediator;
    protected TextAdapter name;
    protected JCheckBox selecting;
    protected SelectPropertiesPanel properties;

    public SQLTemplateMainTab(ProjectController mediator) {
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

        properties = new RawQueryPropertiesPanel(mediator) {

            protected void setEntity(ObjEntity entity) {
                SQLTemplateMainTab.this.setEntity(entity);
            }

            public ObjEntity getEntity(GenericSelectQuery query) {
                if (query instanceof SQLTemplate) {
                    return SQLTemplateMainTab.this.getEntity((SQLTemplate) query);
                }

                return null;
            }
        };

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, fill:max(200dlu;pref)",
                "p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("SQLTemplate Settings", cc.xywh(1, 1, 3, 1));
        builder.addLabel("Query Name:", cc.xy(1, 3));
        builder.add(name.getComponent(), cc.xy(3, 3));
        builder.addLabel("Is Selecting:", cc.xy(1, 5));
        builder.add(selecting, cc.xy(3, 5));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.NORTH);
        this.add(properties, BorderLayout.CENTER);
    }

    private void initController() {

        selecting.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                properties.setEnabled(selecting.isSelected());

                SQLTemplate query = getQuery();
                if (query != null) {
                    query.setSelecting(selecting.isSelected());
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

        if (!(query instanceof SQLTemplate)) {
            setVisible(false);
            return;
        }

        SQLTemplate sqlQuery = (SQLTemplate) query;

        name.setText(sqlQuery.getName());
        selecting.setSelected(sqlQuery.isSelecting());
        properties.setEnabled(sqlQuery.isSelecting());
        properties.initFromModel(sqlQuery);

        setVisible(true);
    }

    protected SQLTemplate getQuery() {
        Query query = mediator.getCurrentQuery();
        return (query instanceof SQLTemplate) ? (SQLTemplate) query : null;
    }

    /**
     * Initializes Query name from string.
     */
    void setQueryName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        Query query = getQuery();

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
    ObjEntity getEntity(SQLTemplate query) {
        return query != null && query.getRoot() instanceof ObjEntity ? (ObjEntity) query
                .getRoot() : null;
    }

    void setEntity(ObjEntity entity) {
        SQLTemplate template = getQuery();
        if (template != null) {
            // in case of null entity, set root to DataMap
            Object root = entity != null ? (Object) entity : mediator.getCurrentDataMap();
            template.setRoot(root);

            mediator.fireQueryEvent(new QueryEvent(this, template));
        }
    }
}