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
package org.apache.cayenne.modeler.ui.project.tree;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ui.project.ProjectController;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;

class TreeDragSource implements DragSourceListener, DragGestureListener {

    private final DragSource source;
    private final JTree sourceTree;
    private final ProjectController controller;

    private TreeDropTarget dt;

    public TreeDragSource(DragSource source, JTree sourceTree, ProjectController controller) {
        this.sourceTree = sourceTree;
        this.controller = controller;
        this.source = source;
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        TreePath path = sourceTree.getSelectionPath();
        if ((path == null) || (path.getPathCount() <= 1)) {
            return;
        }
        dt = new TreeDropTarget(sourceTree, controller, path);
        source.startDrag(dge, DragSource.DefaultLinkDrop, dt, this);
    }

    @Override
    public void dragOver(DragSourceDragEvent dsde) {

        TreePath sourcePath = sourceTree.getSelectionPath();
        DefaultMutableTreeNode sourceParent = (DefaultMutableTreeNode) sourcePath.getLastPathComponent();

        TreePath path = dt.getPath();

        if (path == null) {
            dsde.getDragSourceContext().setCursor(DragSource.DefaultLinkNoDrop);
            return;
        }

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (sourceParent.getUserObject() instanceof DataMap && parent.getUserObject() instanceof DataNodeDescriptor) {
            dsde.getDragSourceContext().setCursor(DragSource.DefaultLinkDrop);
        } else {
            dsde.getDragSourceContext().setCursor(DragSource.DefaultLinkNoDrop);
        }

    }

    @Override
    public void dropActionChanged(DragSourceDragEvent dsde) {
    }

    @Override
    public void dragDropEnd(DragSourceDropEvent dsde) {
    }

    @Override
    public void dragEnter(DragSourceDragEvent dsde) {
    }

    @Override
    public void dragExit(DragSourceEvent dse) {
    }
}