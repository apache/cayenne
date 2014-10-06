package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.undo.LinkDataMapsUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

public class LinkDataMapsAction extends CayenneAction {

    public static String getActionName() {
        return "Link unlinked DataMaps";
    }

    /**
     * Constructor for LinkDataMapsAction.
     *
     * @param application
     */
    public LinkDataMapsAction(Application application) {
        super(getActionName(), application);
    }

    @Override
    public String getIconName() {
        return "icon-sync.gif";
    }

    @Override
    public void performAction(ActionEvent e) {
        ProjectController mediator = getProjectController();
        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) mediator.getProject().getRootNode();

        Collection<String> linkedDataMaps = new ArrayList<String>();
        DataNodeDescriptor dataNodeDescriptor = mediator.getCurrentDataNode();
        for (DataNodeDescriptor dataNodeDesc : dataChannelDescriptor.getNodeDescriptors()) {
            linkedDataMaps.addAll(dataNodeDesc.getDataMapNames());
        }

        for (DataMap dataMap : dataChannelDescriptor.getDataMaps()) {
            if (!linkedDataMaps.contains(dataMap.getName())) {
                dataNodeDescriptor.getDataMapNames().add(dataMap.getName());
                mediator.fireDataNodeEvent(new DataNodeEvent(this, dataNodeDescriptor));
            }
        }

        application.getUndoManager().addEdit(
                new LinkDataMapsUndoableEdit(dataNodeDescriptor, linkedDataMaps, mediator));
    }

}
