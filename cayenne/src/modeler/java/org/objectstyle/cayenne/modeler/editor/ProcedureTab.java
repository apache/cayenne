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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.event.ProcedureEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.modeler.util.TextAdapter;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel for editing stored procedure general settings, such as name, schema, etc.
 * 
 * @author Andrei Adamchik
 */
public class ProcedureTab extends JPanel implements ProcedureDisplayListener,
        ExistingSelectionProcessor {

    protected ProjectController eventController;
    protected TextAdapter name;
    protected TextAdapter schema;
    protected JCheckBox returnsValue;
    protected boolean ignoreChange;

    public ProcedureTab(ProjectController eventController) {
        this.eventController = eventController;

        initView();
        initController();
    }

    private void initView() {
        // create widgets

        this.name = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setProcedureName(text);
            }
        };

        this.schema = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setSchema(text);
            }
        };

        this.returnsValue = new JCheckBox();

        JLabel returnValueHelp = CayenneWidgetFactory
                .createLabel("(first parameter will be used as return value)");
        returnValueHelp.setFont(returnValueHelp.getFont().deriveFont(10));

        // assemble
        FormLayout layout = new FormLayout(
                "right:max(50dlu;pref), 3dlu, left:max(20dlu;pref), 3dlu, fill:150dlu",
                "p, 3dlu, p, 3dlu, p, 3dlu, p");

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("Stored Procedure Configuration", cc.xywh(1, 1, 5, 1));
        builder.addLabel("Procedure Name:", cc.xy(1, 3));
        builder.add(name.getComponent(), cc.xywh(3, 3, 3, 1));
        builder.addLabel("Schema:", cc.xy(1, 5));
        builder.add(schema.getComponent(), cc.xywh(3, 5, 3, 1));
        builder.addLabel("Returns Value:", cc.xy(1, 7));
        builder.add(returnsValue, cc.xy(3, 7));
        builder.add(returnValueHelp, cc.xy(5, 7));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initController() {
        returnsValue.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                Procedure procedure = eventController.getCurrentProcedure();
                if (procedure != null && !ignoreChange) {
                    procedure.setReturningValue(returnsValue.isSelected());
                    eventController.fireProcedureEvent(new ProcedureEvent(
                            ProcedureTab.this,
                            procedure));
                }
            }
        });

        eventController.addProcedureDisplayListener(this);
    }

    public void processExistingSelection() {
        ProcedureDisplayEvent e = new ProcedureDisplayEvent(
                this,
                eventController.getCurrentProcedure(),
                eventController.getCurrentDataMap(),
                eventController.getCurrentDataDomain());
        eventController.fireProcedureDisplayEvent(e);
    }

    /**
     * Invoked when currently selected Procedure object is changed.
     */
    public synchronized void currentProcedureChanged(ProcedureDisplayEvent e) {
        Procedure procedure = e.getProcedure();
        if (procedure == null || !e.isProcedureChanged()) {
            return;
        }

        name.setText(procedure.getName());
        schema.setText(procedure.getSchema());

        ignoreChange = true;
        returnsValue.setSelected(procedure.isReturningValue());
        ignoreChange = false;
    }

    void setProcedureName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        Procedure procedure = eventController.getCurrentProcedure();

        if (procedure == null || Util.nullSafeEquals(newName, procedure.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Procedure name is required.");
        }
        else if (procedure.getDataMap().getProcedure(newName) == null) {
            // completely new name, set new name for entity
            ProcedureEvent e = new ProcedureEvent(this, procedure, procedure.getName());
            ProjectUtil.setProcedureName(procedure.getDataMap(), procedure, newName);
            eventController.fireProcedureEvent(e);
        }
        else {
            // there is an entity with the same name
            throw new ValidationException("There is another procedure with name '"
                    + newName
                    + "'.");
        }
    }

    void setSchema(String text) {
        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        Procedure procedure = eventController.getCurrentProcedure();

        if (procedure != null && !Util.nullSafeEquals(procedure.getSchema(), text)) {
            procedure.setSchema(text);
            eventController.fireProcedureEvent(new ProcedureEvent(this, procedure));
        }
    }
}