package org.objectstyle.cayenne.modeler.dialog.classgen;

import org.objectstyle.cayenne.map.ObjEntity;

/**
 * @author Andrei Adamchik
 */
public class ClassGeneratorEntityWrapper {
    protected ObjEntity entity;
    protected boolean selected;
    protected String validationMessage;
    
    public ClassGeneratorEntityWrapper(ObjEntity entity, boolean selected) {
    	this(entity, selected, null);
    }
    
    public ClassGeneratorEntityWrapper(ObjEntity entity, boolean selected, String validationMessage) {
        this.entity = entity;
        this.selected = selected;
        this.validationMessage = validationMessage;
    }
    
    /**
     * Returns the entity.
     * @return ObjEntity
     */
    public ObjEntity getEntity() {
        return entity;
    }

    /**
     * Returns the selected.
     * @return boolean
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the entity.
     * @param entity The entity to set
     */
    public void setEntity(ObjEntity entity) {
        this.entity = entity;
    }

    /**
     * Sets the selected.
     * @param selected The selected to set
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     * Returns the enabled.
     * @return boolean
     */
    public boolean isEnabled() {
        return validationMessage == null;
    }
    
    /**
     * Returns the validationMessage.
     * @return String
     */
    public String getValidationMessage() {
        return validationMessage;
    }

    /**
     * Sets the validationMessage.
     * @param validationMessage The validationMessage to set
     */
    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
}
