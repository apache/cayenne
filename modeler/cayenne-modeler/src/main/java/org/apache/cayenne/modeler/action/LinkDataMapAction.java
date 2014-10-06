package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.undo.LinkDataMapUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

public class LinkDataMapAction extends CayenneAction {

    public static String getActionName() {
        return "Link DataMap";
    }

    /**
     * Constructor for LinkDataMapAction.
     *
     * @param application
     */
    public LinkDataMapAction(Application application) {
        super(getActionName(), application);
    }

    public void linkDataMap(DataMap map, DataNodeDescriptor node) {
        if (map == null) {
            return;
        }

        // no change?
        if (node != null && node.getDataMapNames().contains(map.getName())) {
            return;
        }

        ProjectController mediator = getProjectController();
        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) mediator.getProject().getRootNode();
        Collection<DataNodeDescriptor> unlinkedNodes = new ArrayList<DataNodeDescriptor>();

        // unlink map from any nodes
        // Theoretically only one node may contain a datamap at each given time.
        // Being paranoid, we will still scan through all.
        for (DataNodeDescriptor nextNode : dataChannelDescriptor.getNodeDescriptors()) {
            if (nextNode.getDataMapNames().contains(map.getName())) {
                nextNode.getDataMapNames().remove(map.getName());
                mediator.fireDataNodeEvent(new DataNodeEvent(this, nextNode));
                unlinkedNodes.add(nextNode);
            }
        }

        // link to a selected node
        if (node != null) {
            node.getDataMapNames().add(map.getName());

            // announce DataNode change
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
        }

        application.getUndoManager().addEdit(
                new LinkDataMapUndoableEdit(map, node, unlinkedNodes, mediator));
    }

    @Override
    public void performAction(ActionEvent e) {

    }
}