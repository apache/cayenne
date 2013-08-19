package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import org.apache.cayenne.modeler.Application;

import org.apache.cayenne.modeler.dialog.datadomain.FilterDialog;
import org.apache.cayenne.modeler.util.CayenneAction;

public class FilterAction extends CayenneAction{
    
	private FilterDialog filterDialog = null;
	
    public static String getActionName() {
        return "Filter tree";
    }

	public FilterAction(Application application) {
        super(getActionName(), application);
    }
	
    @Override
    public String getIconName() {
        return "icon-filter.png";
    }

	@Override
	public void performAction(ActionEvent e) {
		JButton source = (JButton)e.getSource();
        if(filterDialog == null)
        	filterDialog  = new FilterDialog(getApplication().getFrameController().getEditorView().getFilterController());
		filterDialog.pack();
		filterDialog.show(source, 0, source.getHeight());
	}	
}