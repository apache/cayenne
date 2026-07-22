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
package org.apache.cayenne.modeler.ui.project.editor.embeddable.main;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.event.model.ObjAttributeEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableEvent;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.ui.action.CreateAttributeAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayListener;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.project.ProjectComparators;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import java.util.Objects;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Iterator;

public class EmbeddableMainView extends ProjectPanel implements EmbeddableDisplayListener {

    protected CMUndoableTextField className;
    protected CMUndoableTextField comment;

    public EmbeddableMainView(ProjectSession session) {
        super(session);
        initView();
        initController();
    }

    private void initController() {
        session.addEmbeddableDisplayListener(this);
    }

    private void initView() {
        this.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        GlobalActions globalActions = app.getActionManager();
        toolBar.add(globalActions.getAction(CreateAttributeAction.class).buildButton());

        add(toolBar, BorderLayout.NORTH);

        className = new CMUndoableTextField(app.getUndoManager());
        className.addCommitListener(this::setClassName);

        comment = new CMUndoableTextField(app.getUndoManager());
        comment.addCommitListener(this::setComment);

        FormLayout layout = new FormLayout(
                "right:50dlu, $lcgap, fill:150dlu, $lcgap, fill:100",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.append("Class Name:", className, 3);
        builder.append("Comment:", comment, 3);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    void setClassName(String newClassName) {
        if (newClassName != null && newClassName.trim().length() == 0) {
            newClassName = null;
        }

        Embeddable embeddable = session.getSelectedEmbeddable();

        if (embeddable == null) {
            return;
        }

        if (Objects.equals(newClassName, embeddable.getClassName())) {
            return;
        }

        if (newClassName == null) {
            throw new ValidationException("Embeddable name is required.");
        }
        else if (embeddable.getDataMap().getEmbeddable(newClassName) == null) {

            // if newClassName dupliucates in other DataMaps
            DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
            if (domain != null) {
                for (DataMap nextMap : domain.getDataMaps()) {
                    if (nextMap == embeddable.getDataMap()) {
                        continue;
                    }

                    Embeddable conflictingEmbeddable = nextMap.getEmbeddable(newClassName);
                    if (conflictingEmbeddable != null) {
                        throw new ValidationException(
                                    "Duplicate Embeddable name in another DataMap: "
                                            + newClassName
                                            + ".");
                    }
                }
            }

            // completely new name, set new name for embeddable
            EmbeddableEvent e = EmbeddableEvent.ofChange(this, embeddable, embeddable
                    .getClassName());
            String oldName = embeddable.getClassName();
            embeddable.setClassName(newClassName);

            session.fireEmbeddableEvent(e, session.getSelectedDataMap());

            Iterator it = ((DataChannelDescriptor) session.project().getRootNode()).getDataMaps().iterator();
            while (it.hasNext()) {
                DataMap dataMap = (DataMap) it.next();
                Iterator<ObjEntity> ent = dataMap.getObjEntities().stream()
                        .sorted(ProjectComparators.forDataMapChildren())
                        .iterator();

                while (ent.hasNext()) {

                    Collection<ObjAttribute> attr = ent.next().getAttributes();
                    Iterator<ObjAttribute> attrIt = attr.iterator();

                    while (attrIt.hasNext()) {
                        ObjAttribute atribute = attrIt.next();
                        if (atribute.getType()==null || atribute.getType().equals(oldName)) {
                            atribute.setType(newClassName);
                            ObjAttributeEvent ev = ObjAttributeEvent.ofChange(this, atribute, atribute
                                    .getEntity());
                            session.fireObjAttributeEvent(ev);
                        }
                    }

                }
            }

        }
        else {
            // there is an embeddable with the same name
            throw new ValidationException("There is another embeddable with name '"
                    + newClassName
                    + "'.");
        }

    }

    public void embeddableSelected(EmbeddableDisplayEvent e) {
        Embeddable embeddable = e.getEmbeddable();
        if (embeddable == null) {
            return;
        }
        initFromModel(embeddable);
    }

    private void initFromModel(Embeddable embeddable) {
        className.setText(embeddable.getClassName());
        comment.setText(getComment(embeddable));
    }

    void setComment(String comment) {
        Embeddable embeddable = session.getSelectedEmbeddable();

        if (embeddable == null) {
            return;
        }

        ObjectInfo.putToMetaData(app.getMetaData(), embeddable, ObjectInfo.COMMENT, comment);
        session.fireEmbeddableEvent(EmbeddableEvent.ofChange(this, embeddable), session.getSelectedDataMap());
    }

    String getComment(Embeddable embeddable) {
        return ObjectInfo.getFromMetaData(app.getMetaData(), embeddable, ObjectInfo.COMMENT);
    }
}
