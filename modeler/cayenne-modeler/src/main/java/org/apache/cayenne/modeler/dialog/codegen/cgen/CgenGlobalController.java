package org.apache.cayenne.modeler.dialog.codegen.cgen;

import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;

import java.awt.*;

public class CgenGlobalController extends CayenneController{

    protected CgenDialog view;

    protected CgenGlobalPanelController globalPanelController;

    public CgenGlobalController(CayenneController parent){
        super(parent);

        globalPanelController = new CgenGlobalPanelController(this);
    }

    public void startup(){
        this.view = new CgenDialog(globalPanelController.getView());
        initBindings();

        view.pack();
        view.setModal(true);
        centerView();
        makeCloseableOnEscape();
        view.setVisible(true);
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCancelButton(), "cancelAction()");
    }

    public void cancelAction() {
        view.dispose();
    }

    @Override
    public Component getView() {
        return view;
    }
}
