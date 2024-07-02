package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.DomainEvent;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.undo.CayenneUndoManager;
import org.apache.cayenne.modeler.undo.UpdateValidationConfigUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.validation.ValidationConfig;

import java.awt.event.ActionEvent;

/**
 * Requires config provided with {@link UpdateValidationConfigAction#putConfig(ValidationConfig)}.
 *
 * @since 5.0
 * */
public class UpdateValidationConfigAction extends CayenneAction {

    public static final String ACTION_NAME = "Update ValidationConfig";

    private static final String CONFIG_PARAM = "config";

    private boolean undoable;

    public UpdateValidationConfigAction(Application application) {
        super(ACTION_NAME, application);
        undoable = true;
    }

    protected UpdateValidationConfigAction(String name, Application application) {
        super(name, application);
    }

    public void performAction(Object source) {
        performAction(new ActionEvent(source, ActionEvent.ACTION_PERFORMED, null));
    }

    @Override
    public void performAction(ActionEvent e) {
        DataChannelMetaData metaData = application.getMetaData();
        DataChannelDescriptor dataChannel = ((DataChannelDescriptor) application.getProject().getRootNode());
        ValidationConfig config = (ValidationConfig) getValue(CONFIG_PARAM);
        ValidationConfig oldConfig = ValidationConfig.fromMetadata(metaData, dataChannel);
        metaData.add(dataChannel, config);

        if (undoable) {
            CayenneUndoManager undoManager = application.getUndoManager();
            undoManager.addEdit(new UpdateValidationConfigUndoableEdit(oldConfig, config));
        }
        getProjectController().fireDomainEvent(new DomainEvent(e.getSource(), dataChannel));
    }

    public UpdateValidationConfigAction putConfig(ValidationConfig config) {
        putValue(CONFIG_PARAM, config);
        return this;
    }

    public UpdateValidationConfigAction setUndoable(boolean undoable) {
        this.undoable = undoable;
        return this;
    }
}
