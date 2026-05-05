package org.apache.cayenne.modeler.ui.errors;

import org.apache.cayenne.modeler.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorsController.class);

    /**
     * Shows an error dialog with stack trace
     */
    public static void guiException(Application application, Throwable th) {
        if (th != null) {
            LOGGER.error("CayenneModeler Error", th);
        }

        new ErrorDialog(application, "CayenneModeler Error", th, true, false).open();
    }

    /**
     * Shows a warning dialog with stack trace
     */
    public static void guiWarning(Application application, Throwable th, String message) {
        if (th != null) {
            LOGGER.warn("CayenneModeler Warning", th);
        }

        new WarningDialog(application, message, th, false, false).setDetailed(true);
    }
}
