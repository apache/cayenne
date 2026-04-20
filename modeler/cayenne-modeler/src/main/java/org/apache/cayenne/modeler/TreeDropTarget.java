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
package org.apache.cayenne.modeler;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.action.LinkDataMapAction;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

public class TreeDropTarget implements DropTargetListener, Transferable {

    private DropTarget target;
    private final JTree targetTree;
    private final ProjectController controller;
    private final TreePath parentPath;
    private TreePath targetPath;

    public TreeDropTarget(JTree targetTree, ProjectController controller, TreePath parentPath) {
        this.targetTree = targetTree;
        this.controller = controller;
        this.parentPath = parentPath;

        // ugly - many DND objects are using constructors with side effects
        new DropTarget(targetTree, this);
    }

    public void dragEnter(DropTargetDragEvent dtde) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
        Point p = dtde.getLocation();
        targetPath = targetTree.getPathForLocation(p.x, p.y);
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public void drop(DropTargetDropEvent dtde) {
        if (targetPath != null) {
            try {
                dtde.acceptDrop(dtde.getDropAction());
                DefaultMutableTreeNode target = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) parentPath.getLastPathComponent();

                if (target.getUserObject() instanceof DataNodeDescriptor && parent.getUserObject() instanceof DataMap) {
                    DataNodeDescriptor currentDataNode = (DataNodeDescriptor) target.getUserObject();
                    DataMap currentDataMap = (DataMap) parent.getUserObject();

                    LinkDataMapAction action = controller.getApplication().getActionManager().getAction(LinkDataMapAction.class);
                    action.linkDataMap(currentDataMap, currentDataNode);

                    targetTree.makeVisible(targetPath.pathByAddingChild(target));
                    dtde.dropComplete(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                dtde.rejectDrop();
            }
        }

    }

    public TreePath getPath() {
        return this.targetPath;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) {
        return null;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return null;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor arg0) {
        return false;
    }
}