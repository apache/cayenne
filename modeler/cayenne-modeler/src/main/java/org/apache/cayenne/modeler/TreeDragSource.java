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

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;

public class TreeDragSource implements DragSourceListener, DragGestureListener {

    private DragSource source;
    private JTree sourceTree;
    private ProjectController eventController;
    private DragGestureRecognizer recognizer;
    private TreeDropTarget dt;

    public TreeDragSource(JTree tree, int actions, ProjectController eventController) {
        sourceTree = tree;
        this.eventController = eventController;
        source = new DragSource();
        recognizer = source.createDefaultDragGestureRecognizer(sourceTree, actions, this);
    }

    /*
     * Drag Gesture Handler
     */
    public void dragGestureRecognized(DragGestureEvent dge) {
        TreePath path = sourceTree.getSelectionPath();
        if ((path == null) || (path.getPathCount() <= 1)) {
            return;
        }
        dt = new TreeDropTarget(sourceTree, eventController, path);
        source.startDrag(dge, DragSource.DefaultLinkDrop, dt, this);
    }

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

    public void dropActionChanged(DragSourceDragEvent dsde) {
    }

    public void dragDropEnd(DragSourceDropEvent dsde) {
    }

    public void dragEnter(DragSourceDragEvent dsde) {
    }

    public void dragExit(DragSourceEvent dse) {
    }

}