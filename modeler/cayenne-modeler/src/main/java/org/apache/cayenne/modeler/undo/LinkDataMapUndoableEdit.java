package org.apache.cayenne.modeler.undo;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.LinkDataMapAction;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.Collection;

public class LinkDataMapUndoableEdit extends CayenneUndoableEdit {

    DataMap map;
    DataNodeDescriptor node;
    Collection<DataNodeDescriptor> unlinkedNodes;
    ProjectController mediator;

    @Override
    public String getPresentationName() {
        return "Link unlinked DataMaps";
    }

    public LinkDataMapUndoableEdit(DataMap map, DataNodeDescriptor node, Collection<DataNodeDescriptor> unlinkedNodes, ProjectController mediator) {
        this.map = map;
        this.node = node;
        this.unlinkedNodes = unlinkedNodes;
        this.mediator = mediator;
    }

    @Override
    public void redo() throws CannotRedoException {
        LinkDataMapAction action = actionManager.getAction(LinkDataMapAction.class);
        action.linkDataMap(map, node);
    }

    @Override
    public void undo() throws CannotUndoException {
        if (node != null) {
            node.getDataMapNames().remove(map.getName());
            mediator.fireDataNodeEvent(new DataNodeEvent(this, node));
        }

        if (!unlinkedNodes.isEmpty()) {
            for (DataNodeDescriptor unlinkedNode : unlinkedNodes) {
                unlinkedNode.getDataMapNames().add(map.getName());
                mediator.fireDataNodeEvent(new DataNodeEvent(this, unlinkedNode));
            }
        }
    }

}
