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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.Comparators;
import org.objectstyle.cayenne.query.GenericSelectQuery;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel that supports editing the properties a query not based on ObjEntity, but still
 * supporting DataObjects retrieval.
 * 
 * @author Andrei Adamchik
 */
public abstract class RawQueryPropertiesPanel extends SelectPropertiesPanel {

    protected JCheckBox dataObjects;
    protected JComboBox entities;

    public RawQueryPropertiesPanel(ProjectController mediator) {
        super(mediator);
    }

    protected void initController() {
        super.initController();
        dataObjects.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                setFetchingDataObjects(dataObjects.isSelected());
            }
        });

        entities.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                ObjEntity entity = (ObjEntity) entities.getModel().getSelectedItem();
                setEntity(entity);
            }
        });
    }

    protected void initView() {
        super.initView();

        // create widgets

        dataObjects = new JCheckBox();

        entities = CayenneWidgetFactory.createComboBox();
        entities.setRenderer(CellRenderers.listRendererWithIcons());

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:max(80dlu;pref), 3dlu, left:max(10dlu;pref), "
                        + "3dlu, left:max(37dlu;pref), 3dlu, fill:max(147dlu;pref)",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("", cc.xywh(1, 1, 7, 1));

        builder.addLabel("Result Caching:", cc.xy(1, 3));
        builder.add(cachePolicy, cc.xywh(3, 3, 5, 1));
        builder.addLabel("Fetch Data Objects:", cc.xy(1, 7));
        builder.add(dataObjects, cc.xy(3, 7));
        builder.add(entities, cc.xywh(5, 7, 3, 1));
        builder.addLabel("Refresh Results:", cc.xy(1, 9));
        builder.add(refreshesResults, cc.xy(3, 9));
        builder.addLabel("Fetch Limit, Rows:", cc.xy(1, 11));
        builder.add(fetchLimit.getComponent(), cc.xywh(3, 11, 3, 1));
        builder.addLabel("Page Size:", cc.xy(1, 13));
        builder.add(pageSize.getComponent(), cc.xywh(3, 13, 3, 1));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * query is changed.
     */
    public void initFromModel(GenericSelectQuery query) {
        super.initFromModel(query);

        boolean fetchingDO = !query.isFetchingDataRows();
        dataObjects.setSelected(fetchingDO);

        // TODO: now we only allow ObjEntities from the current map,
        // since query root is fully resolved during map loading,
        // making it impossible to reference other DataMaps.

        DataMap map = mediator.getCurrentDataMap();
        List objEntities = new ArrayList();
        objEntities.addAll(map.getObjEntities());

        if (objEntities.size() > 1) {
            Collections.sort(objEntities, Comparators.getDataMapChildrenComparator());
        }

        entities.setEnabled(fetchingDO && isEnabled());
        DefaultComboBoxModel model = new DefaultComboBoxModel(objEntities.toArray());
        model.setSelectedItem(getEntity(query));
        entities.setModel(model);
    }

    protected abstract void setEntity(ObjEntity selectedEntity);

    protected abstract ObjEntity getEntity(GenericSelectQuery query);

    protected void setFetchingDataObjects(boolean dataObjects) {
        entities.setEnabled(dataObjects && isEnabled());

        if (!dataObjects) {
            entities.getModel().setSelectedItem(null);
        }

        setQueryProperty("fetchingDataRows", dataObjects ? Boolean.FALSE : Boolean.TRUE);
    }
}