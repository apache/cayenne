package org.apache.cayenne.modeler.editor.validation;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.modeler.editor.ObjAttributeTableModel;
import org.apache.cayenne.modeler.editor.wrapper.ObjAttributeWrapper;
import org.apache.cayenne.project.validation.ConfigurationNodeValidator;
import org.apache.cayenne.validation.ValidationResult;

public class ObjAttributeWrapperValidator extends ConfigurationNodeValidator {

    public boolean validate(ObjAttributeWrapper wrapper, ValidationResult validationResult) {
        if (isAttributeNameOverlapped(wrapper)) {
            addFailure(validationResult, new AttributeValidationFailure(
                    ObjAttributeTableModel.OBJ_ATTRIBUTE,
                    "Duplicate attribute name."));
        }
        
        return wrapper.isValid();
    }

    /**
     * @return false if entity has attribute with the same name.
     */
    private boolean isAttributeNameOverlapped(ObjAttributeWrapper attr) {
    	ObjAttribute temp = attr.getEntity().getAttributeMap().get(attr.getName());
    	if (temp != null && attr.getValue() != temp ){
    		return true;
    	}
    	return false;
    }
}
