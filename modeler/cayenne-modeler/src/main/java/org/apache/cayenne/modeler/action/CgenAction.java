package org.apache.cayenne.modeler.action;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.codegen.cgen.CgenGlobalController;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;

public class CgenAction extends CayenneAction{

    private static Logger logObj = LoggerFactory.getLogger(CgenAction.class);

    public CgenAction(Application application) {
        super(getActionName(), application);
    }

    public static String getActionName(){
        return "Generate All Classes";
    }

    @Override
    public void performAction(ActionEvent e) {
        new CgenGlobalController(getApplication().getFrameController()).startup();
    }
}
