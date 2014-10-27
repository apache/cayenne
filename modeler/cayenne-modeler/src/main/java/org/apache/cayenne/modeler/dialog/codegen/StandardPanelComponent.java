package org.apache.cayenne.modeler.dialog.codegen;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.pref.DataMapDefaults;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

public class StandardPanelComponent extends JComponent {

    private DataMap dataMap;
    private DataMapDefaults preferences;
    private JLabel dataMapName;
    private JTextField superclassPackage;
    private DefaultFormBuilder builder;

    public StandardPanelComponent() {
        super();
        dataMapName = new JLabel();
        dataMapName.setFont(dataMapName.getFont().deriveFont(1));
        superclassPackage = new JTextField();

        FormLayout layout = new FormLayout(
                "right:77dlu, 3dlu, fill:200:grow, 3dlu", "");
        builder = new DefaultFormBuilder(layout);
        builder.append(dataMapName);
        builder.nextLine();
        builder.append("Superclass Package:", superclassPackage);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    public DataMapDefaults getPreferences() {
        return preferences;
    }

    public void setPreferences(DataMapDefaults preferences) {
        this.preferences = preferences;
    }

    public JLabel getDataMapName() {
        return dataMapName;
    }

    public JTextField getSuperclassPackage() {
        return superclassPackage;
    }

}
