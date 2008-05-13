package org.apache.cayenne.swing;

import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * A generic adapter that binds a check box to a bean property.
 *
 * @author Kevin Menard
 */
public class CheckBoxBinding extends BindingBase {

    protected JCheckBox checkBox;

    public CheckBoxBinding(JCheckBox checkBox, String propertyExpression) {
        super(propertyExpression);
        this.checkBox = checkBox;

        this.checkBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent event) {
                if (!modelUpdateDisabled) {
                    updateModel();
                }
            }
        });
    }

    public Component getView() {
        return checkBox;
    }

    public void updateView() {
        Boolean value = (Boolean) getValue();

        modelUpdateDisabled = true;
        try {
            checkBox.setSelected(value.booleanValue());
        }
        finally {
            modelUpdateDisabled = false;
        }
    }

    protected void updateModel() {
        setValue(Boolean.valueOf(checkBox.isSelected()));
    }
}
