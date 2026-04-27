package org.apache.cayenne.modeler.ui.errors;

import org.apache.cayenne.modeler.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorsController.class);

    /**
     * Shows an error dialog with stack trace
     */
    public static void guiException(Throwable th) {
        if (th != null) {
            LOGGER.error("CayenneModeler Error", th);
        }

        new ErrorDialog(Application.getInstance(), "CayenneModeler Error", th, true, false).setVisible(true);
    }

    /**
     * Shows a warning dialog with stack trace
     */
    public static void guiWarning(Throwable th, String message) {
        if (th != null) {
            LOGGER.warn("CayenneModeler Warning", th);
        }

        new WarningDialog(Application.getInstance(), message, th, false, false).setDetailed(true);
    }
}
