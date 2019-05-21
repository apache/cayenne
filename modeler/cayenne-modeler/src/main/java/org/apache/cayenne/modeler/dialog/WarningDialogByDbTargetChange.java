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

package org.apache.cayenne.modeler.dialog;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.Collection;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.ProjectUtil;

/**
 * @since 4.0
 */
public class WarningDialogByDbTargetChange {

    /*
      Check the situation, when targetEntity in DbRelationship was changed.
      After we have two cases:
          - the user agrees to accept changes (all ObjRelationships and ObjAttributes),
            that are related to current targetEntity and set new targetEntity.
          - the user doesn't agree and wanna return back.
    */

    public static boolean showWarningDialog(ProjectController mediator, DbRelationship relationship) {

        int result;
        Collection<ObjRelationship> objRelationshipsForDbRelationship = ProjectUtil
                .findObjRelationshipsForDbRelationship(mediator, relationship);
        Collection<ObjAttribute> fObjAttributesForDbRelationship = ProjectUtil
                .findObjAttributesForDbRelationship(mediator, relationship);
        if (fObjAttributesForDbRelationship.isEmpty() && objRelationshipsForDbRelationship.isEmpty()) {
            result = JOptionPane.showConfirmDialog(Application.getFrame(), "Changing target entity will reset all joins.",
                    "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            return (result == JOptionPane.OK_OPTION);
        }

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BorderLayout());
        JLabel textLabel = new JLabel(String.format("<html><p>Following ObjAttributes and ObjRelationships "
                + "<br>will be affected by change of DbRelationship <br> '%s'"
                + " target and must be fixed manually. "
                + "<br>Are you sure you want to proceed?</p><br></html>", relationship.getName()));
        dialogPanel.add(textLabel, BorderLayout.NORTH);
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> objects = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(objects);

        if (!objRelationshipsForDbRelationship.isEmpty()) {
            model.addElement("Relationships: ");
            for (ObjRelationship objRelationship : objRelationshipsForDbRelationship) {
                model.addElement(objRelationship.getSourceEntity().getName() + "." + objRelationship.getName());
            }
        }

        if (!fObjAttributesForDbRelationship.isEmpty()) {
            model.addElement("Attributes: ");
            for (ObjAttribute objAttribute : fObjAttributesForDbRelationship) {
                model.addElement(objAttribute.getEntity().getName() + "." + objAttribute.getName());
            }
        }

        dialogPanel.add(scrollPane, BorderLayout.SOUTH);
        result = JOptionPane.showConfirmDialog(Application.getFrame(), dialogPanel,
                "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return (result == JOptionPane.OK_OPTION);
    }
}
