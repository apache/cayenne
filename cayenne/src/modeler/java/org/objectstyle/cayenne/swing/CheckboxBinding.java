package org.objectstyle.cayenne.swing;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;

/**
 * Binds a checkbox state to an int or boolean property.
 * 
 * @author Andrei Adamchik
 */
public class CheckboxBinding extends BindingBase {

    protected JCheckBox checkbox;

    /**
     * @param propertyExpression
     */
    public CheckboxBinding(JCheckBox checkbox, String expression) {
        super(expression);
        this.checkbox = checkbox;

        checkbox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                updateModel();
            }
        });
    }

    public Component getComponent() {
        return checkbox;
    }

    public void updateView() {
        Object value = getValue();
        boolean b = false;

        // convert to boolean
        if (value != null) {
            if (value instanceof Boolean) {
                b = ((Boolean) value).booleanValue();
            }
            else if (value instanceof Number) {
                b = ((Number) value).intValue() != 0;
            }
        }

        modelUpdateDisabled = true;
        try {
            checkbox.setSelected(b);
        }
        finally {
            modelUpdateDisabled = false;
        }
    }

    protected void updateModel() {
        setValue(checkbox.isSelected() ? Boolean.TRUE : Boolean.FALSE);
    }
}