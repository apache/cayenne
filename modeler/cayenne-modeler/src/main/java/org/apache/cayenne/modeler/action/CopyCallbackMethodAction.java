package org.apache.cayenne.modeler.action;

import java.util.Arrays;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;

public class CopyCallbackMethodAction extends CopyAction implements MultipleObjectsAction {

    private final static String ACTION_NAME = "Copy Callback Method";

    /**
     * Name of action if multiple attrs are selected
     */
    private final static String ACTION_NAME_MULTIPLE = "Copy Callback Methods";

    public static String getActionName() {
        return ACTION_NAME;
    }

    public String getActionName(boolean multiple) {
        return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
    }

    public CopyCallbackMethodAction(Application application) {
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

    @Override
    public Object copy(ProjectController mediator) {
        Object[] methods = getProjectController().getCurrentCallbackMethods();

        if (methods != null && methods.length > 0) {
            return Arrays.asList(methods);
        }

        return null;
    }
}
