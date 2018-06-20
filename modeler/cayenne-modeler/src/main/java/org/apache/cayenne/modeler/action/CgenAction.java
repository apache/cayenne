package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

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
        Collection<DataMap> dataMaps;
        DataChannelMetaData metaData = getApplication().getMetaData();

        try {
            Project project = getProjectController().getProject();
            dataMaps = ((DataChannelDescriptor) project.getRootNode()).getDataMaps();
            for (DataMap dataMap : dataMaps) {
                ClassGenerationAction classGenerationAction = metaData.get(dataMap, ClassGenerationAction.class);
                if (classGenerationAction != null) {
                    classGenerationAction.execute();
                }
            }
            JOptionPane.showMessageDialog(
                    this.getApplication().getFrameController().getView(),
                    "Class generation finished");
        } catch (Exception ex) {
            logObj.error("Error generating classes", e);
            JOptionPane.showMessageDialog(
                    this.getApplication().getFrameController().getView(),
                    "Error generating classes - " + ex.getMessage());
        }
    }
}
