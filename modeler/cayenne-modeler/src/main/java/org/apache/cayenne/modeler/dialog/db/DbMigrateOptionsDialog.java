package org.apache.cayenne.modeler.dialog.db;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.CayenneDialog;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class DbMigrateOptionsDialog extends CayenneDialog {
	private static final long serialVersionUID = 1L;
	public static final int CANCEL = 0;
    public static final int SELECT = 1;

	protected JLabel schemaLabel;
    protected JComboBox<String> schemaSelector;
    protected JButton selectButton;
    protected JButton cancelButton;
    protected int choice;
    
    public DbMigrateOptionsDialog(Collection<String> schemas, String dbUserName) {
        super(Application.getFrame(), "Migrater DB Schema: Select Schema");
        init();
        initController();
        initFromModel(schemas, dbUserName);

        pack();
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setModal(true);
        centerWindow();    	
    }
    
    protected void init() {
        selectButton = new JButton("Continue");
        cancelButton = new JButton("Cancel");
        schemaSelector = new JComboBox<String>();
        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, fill:max(170dlu;pref):grow",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        schemaLabel = builder.append("Select Schema:", schemaSelector);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(selectButton);
        buttons.add(cancelButton);
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);        
    }
    
    protected void initController() {
        selectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                processSelect();
            }
        });

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                processCancel();
            }
        });
    }
    
    private void processSelect() {
        choice = SELECT;
        setVisible(false);
    }

    private void processCancel() {
    	schemaSelector.setSelectedItem(null);
        choice = CANCEL;
        setVisible(false);
    }
    
    protected void initFromModel(Collection<String> schemas, String dbUserName) {

        this.choice = CANCEL;

        schemaSelector.setVisible(true);
        schemaLabel.setVisible(true);
        schemaSelector.setModel(new DefaultComboBoxModel<String>(schemas.toArray(new String[] {})));

        // select schema belonging to the user
        if (dbUserName != null) {
            for (String schema : schemas) {
                if (dbUserName.equalsIgnoreCase(schema)) {
                    schemaSelector.setSelectedItem(schema);
                    break;
                }
            }
        }
    }
    
    /**
     * Returns selected schema.
     */
    public String getSelectedSchema() {
    	return (String) schemaSelector.getSelectedItem();
    }
    
    public int getChoice() {
    	return choice;
    }
    
    public void showDialog() {
    	setVisible(true);
    }
}
