package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.modeler.Application;

public class CutCallbackMethodAction extends CutAction implements MultipleObjectsAction {

    private final static String ACTION_NAME = "Cut Callback method";

    /**
     * Name of action if multiple attrs are selected
     */
    private final static String ACTION_NAME_MULTIPLE = "Cut Callback methods";

    public static String getActionName() {
        return ACTION_NAME;
    }

    public String getActionName(boolean multiple) {
        return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
    }

    public CutCallbackMethodAction(Application application) {
        super(ACTION_NAME, application);
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable
     * attribute.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
         return object != null;
    }

    /**
     * Performs cutting of items
     */
    @Override
    public void performAction(ActionEvent e) {
        application
                .getActionManager()
                .getAction(CopyCallbackMethodAction.class)
                .performAction(e);
        application
                .getActionManager()
                .getAction(RemoveCallbackMethodAction.class)
                .performAction(e, false);
    }
}
