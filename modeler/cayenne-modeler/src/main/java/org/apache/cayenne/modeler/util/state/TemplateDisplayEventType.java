package org.apache.cayenne.modeler.util.state;

import org.apache.cayenne.modeler.ProjectController;

/**
 * @since 4.0
 */
public class TemplateDisplayEventType extends DisplayEventType  {

    public TemplateDisplayEventType(ProjectController controller) {
        super(controller);
    }

    @Override
    public void fireLastDisplayEvent() {}

    @Override
    public void saveLastDisplayEvent() {}
}
