package org.apache.cayenne.modeler.undo;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.Collection;

public class LinkDataMapsUndoableEdit extends CayenneUndoableEdit {

    DataNodeDescriptor dataNodeDescriptor;
    Collection<String> linkedDataMaps;
    ProjectController mediator;

    @Override
    public String getPresentationName() {
        return "Link unlinked DataMaps";
    }

    public LinkDataMapsUndoableEdit(DataNodeDescriptor dataNodeDescriptor, Collection<String> linkedDataMaps, ProjectController mediator) {
        this.dataNodeDescriptor = dataNodeDescriptor;
        this.linkedDataMaps = linkedDataMaps;
        this.mediator = mediator;
    }

    @Override
    public void redo() throws CannotRedoException {
        for (DataMap dataMap : ((DataChannelDescriptor) mediator.getProject().getRootNode()).getDataMaps()) {
            if (!linkedDataMaps.contains(dataMap.getName())) {
                dataNodeDescriptor.getDataMapNames().add(dataMap.getName());
                mediator.fireDataNodeEvent(new DataNodeEvent(this, dataNodeDescriptor));
            }
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        dataNodeDescriptor.getDataMapNames().retainAll(linkedDataMaps);
        mediator.fireDataNodeEvent(new DataNodeEvent(this, dataNodeDescriptor));
    }

}
