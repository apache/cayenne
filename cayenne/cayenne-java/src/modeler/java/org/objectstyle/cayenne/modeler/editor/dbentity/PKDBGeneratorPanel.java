/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.modeler.editor.dbentity;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.MutableComboBoxModel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.ProjectController;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class PKDBGeneratorPanel extends PKGeneratorPanel {

    private JComboBox attributes;

    public PKDBGeneratorPanel(ProjectController mediator) {
        super(mediator);
        initView();
    }

    private void initView() {

        attributes = new JComboBox();
        attributes.setEditable(false);
        attributes.setRenderer(new AttributeRenderer());

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
                "right:70dlu, 3dlu, fill:200dlu",
                ""));
        builder.setDefaultDialogBorder();
        builder.append("Auto Incremented:", attributes);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void setDbEntity(DbEntity entity) {
        // refresh only if this entity
        if (isVisible()) {
            updateView(entity);
        }
    }

    public void onInit(DbEntity entity) {
        resetStrategy(entity, true, false);

        Collection pkAttributes = entity.getPrimaryKey();

        // by default check the only numeric PK
        if (pkAttributes.size() == 1) {
            DbAttribute pk = (DbAttribute) pkAttributes.iterator().next();
            if (TypesMapping.isNumeric(pk.getType()) && !pk.isGenerated()) {
                pk.setGenerated(true);
                mediator.fireDbEntityEvent(new EntityEvent(this, entity));
            }
        }

        updateView(entity);
    }

    void updateView(final DbEntity entity) {
        ItemListener[] listeners = attributes.getItemListeners();
        for (int i = 0; i < listeners.length; i++) {
            attributes.removeItemListener(listeners[i]);
        }

        Collection pkAttributes = entity.getPrimaryKey();
        if (pkAttributes.isEmpty()) {
            attributes.removeAllItems();
            attributes.addItem("<Entity has no PK columns>");
            attributes.setSelectedIndex(0);
            attributes.setEnabled(false);
        }
        else {

            attributes.setEnabled(true);
            MutableComboBoxModel model = new DefaultComboBoxModel(pkAttributes.toArray());
            String noSelection = "<Select Generated Column>";
            model.insertElementAt(noSelection, 0);
            model.setSelectedItem(noSelection);
            attributes.setModel(model);

            Iterator it = pkAttributes.iterator();
            while (it.hasNext()) {
                DbAttribute a = (DbAttribute) it.next();
                if (a.isGenerated()) {
                    model.setSelectedItem(a);
                    break;
                }
            }

            // listen for selection changes of the new entity
            attributes.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    Object item = e.getItem();
                    if (item instanceof DbAttribute) {

                        boolean generated = e.getStateChange() == ItemEvent.SELECTED;
                        DbAttribute a = (DbAttribute) item;

                        if (a.isGenerated() != generated) {
                            a.setGenerated(generated);
                            mediator.fireDbEntityEvent(new EntityEvent(this, entity));
                        }
                    }
                }
            });
        }

        // revalidate as children layout has changed...
        revalidate();
    }

    class AttributeRenderer extends BasicComboBoxRenderer {

        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            if (value instanceof DbAttribute) {
                DbAttribute a = (DbAttribute) value;
                String type = TypesMapping.getSqlNameByType(a.getType());
                value = a.getName() + " (" + (type != null ? type : "?") + ")";
            }

            return super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus);
        }
    }
}
