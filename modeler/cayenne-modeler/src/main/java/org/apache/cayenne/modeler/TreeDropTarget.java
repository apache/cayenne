package org.apache.cayenne.modeler;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.action.LinkDataMapAction;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;

public class TreeDropTarget implements DropTargetListener, Transferable {

    private DropTarget target;
    private JTree targetTree;
    private ProjectController eventController;
    private TreePath parentPath;
    private TreePath targetPath;

    public TreeDropTarget(JTree tree, ProjectController eventController, TreePath parentPath) {
        targetTree = tree;
        this.eventController = eventController;
        this.parentPath = parentPath;
        target = new DropTarget(targetTree, this);
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

                    LinkDataMapAction action = eventController.getApplication().getActionManager().getAction(LinkDataMapAction.class);
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

    public Object getTransferData(DataFlavor arg0) throws UnsupportedFlavorException, IOException {
        return null;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return null;
    }

    public boolean isDataFlavorSupported(DataFlavor arg0) {
        return false;
    }
}