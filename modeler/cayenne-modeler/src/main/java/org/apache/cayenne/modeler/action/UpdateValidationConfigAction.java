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
import java.util.Optional;

/**
 * Requires parameters provided with {@link UpdateValidationConfigAction#putDataChannel(DataChannelDescriptor)}
 * and {@link UpdateValidationConfigAction#putConfig(ValidationConfig)}.
 *
 * @since 5.0
 * */
public class UpdateValidationConfigAction extends CayenneAction {

    public static final String DATA_CHANNEL_PARAM = "dataChannel";
    public static final String CONFIG_PARAM = "config";

    private static final String ACTION_NAME = "Update ValidationConfig";

    private boolean undoable;
    private boolean reverberated;

    public UpdateValidationConfigAction(Application application) {
        super(ACTION_NAME, application);
        undoable = true;
    }

    @Override
    public void performAction(ActionEvent e) {
        DataChannelDescriptor dataChannel = (DataChannelDescriptor) getValue(DATA_CHANNEL_PARAM);
        ValidationConfig config = (ValidationConfig) getValue(CONFIG_PARAM);
        CayenneUndoManager undoManager = application.getUndoManager();
        DataChannelMetaData metaData = application.getMetaData();
        ValidationConfig oldConfig = Optional.ofNullable(metaData.get(dataChannel, ValidationConfig.class))
                .orElseGet(ValidationConfig::new);
        metaData.add(dataChannel, config);

        if (undoable && !reverberated) {
            undoManager.addEdit(new UpdateValidationConfigUndoableEdit(dataChannel, oldConfig, config));
        }
        reverberated = true;
        getProjectController().fireDomainEvent(new DomainEvent(this, dataChannel));
        reverberated = false;
    }

    public UpdateValidationConfigAction putDataChannel(DataChannelDescriptor dataChannel) {
        putValue(DATA_CHANNEL_PARAM, dataChannel);
        return this;
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
