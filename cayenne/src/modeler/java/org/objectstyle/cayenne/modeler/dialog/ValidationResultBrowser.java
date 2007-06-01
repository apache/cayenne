package org.objectstyle.cayenne.modeler.dialog;

import java.awt.Component;
import java.util.Iterator;

import javax.swing.JDialog;

import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.validation.ValidationFailure;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * @author Andrei Adamchik
 */
public class ValidationResultBrowser extends CayenneController {

    protected ValidationResultBrowserView view;

    public ValidationResultBrowser(CayenneController parent) {
        super(parent);

        this.view = new ValidationResultBrowserView();

        initController();
    }

    protected void initController() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);
        builder.bindToAction(view.getCloseButton(), "closeDialogAction()");
    }

    public Component getView() {
        return view;
    }

    public void closeDialogAction() {
        view.dispose();
    }

    public void startupAction(
            String title,
            String message,
            ValidationResult validationResult) {

        this.view.setTitle(title);
        this.view.getMessageLabel().setText(message);
        this.view.getErrorsDisplay().setText(buildValidationText(validationResult));

        view.pack();
        view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.show();
    }

    /**
     * Creates validation text for the validation result.
     */
    protected String buildValidationText(ValidationResult validationResult) {
        StringBuffer buffer = new StringBuffer();
        String separator = System.getProperty("line.separator");

        Iterator it = validationResult.getFailures().iterator();
        while (it.hasNext()) {

            if (buffer.length() > 0) {
                buffer.append(separator);
            }

            ValidationFailure failure = (ValidationFailure) it.next();
            if (failure.getSource() != null) {
                buffer.append("[SQL: ").append(failure.getSource()).append("] - ");
            }

            if (failure.getDescription() != null) {
                buffer.append(failure.getDescription());
            }
        }

        return buffer.toString();
    }
}