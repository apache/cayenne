package org.objectstyle.cayenne.modeler.dialog.classgen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.modeler.pref.DataMapDefaults;
import org.objectstyle.cayenne.project.validator.ValidationInfo;
import org.objectstyle.cayenne.util.Util;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 * @author Andrei Adamchik
 */
public class ClassGeneratorModel extends BasicModel {

    protected DataMap map;
    protected DataMapDefaults defaults;

    protected String outputDir;
    protected boolean pairs;
    protected List entities;
    protected String superClassPackage;
    protected String customSuperclassTemplate;
    protected String customClassTemplate;

    public ClassGeneratorModel(DataMap map, DataMapDefaults defaults,
            ObjEntity selectedEntity, List validationInfo) {
        this.map = map;
        this.defaults = defaults;

        prepareEntities(selectedEntity, validationInfo);
        initFromDefaults();
    }

    protected void initFromDefaults() {
        this.customClassTemplate = defaults.getSubclassTemplate();
        this.customSuperclassTemplate = defaults.getSuperclassTemplate();
        this.pairs = (defaults.getGeneratePairs() != null) ? defaults
                .getGeneratePairs()
                .booleanValue() : true;
        this.outputDir = defaults.getOutputPath();
        this.superClassPackage = defaults.getSuperclassPackage();
    }

    protected void prepareEntities(ObjEntity selectedEntity, List validationInfo) {
        Map failedEntities = new HashMap();

        if (validationInfo != null) {
            Iterator vit = validationInfo.iterator();
            while (vit.hasNext()) {
                ValidationInfo info = (ValidationInfo) vit.next();
                ObjEntity ent = (ObjEntity) info.getPath().firstInstanceOf(
                        ObjEntity.class);
                if (ent != null) {
                    failedEntities.put(ent.getName(), info.getMessage());
                }
            }
        }

        List tmp = new ArrayList();
        Iterator it = map.getObjEntities().iterator();
        while (it.hasNext()) {
            ObjEntity entity = (ObjEntity) it.next();

            // check if entity didn't pass the validation
            ClassGeneratorEntityWrapper wrapper = null;
            String errorMessage = (String) failedEntities.get(entity.getName());
            if (errorMessage != null) {
                wrapper = new ClassGeneratorEntityWrapper(entity, false, errorMessage);
            }
            else {
                boolean enabled = (selectedEntity != null)
                        ? selectedEntity == entity
                        : true;
                wrapper = new ClassGeneratorEntityWrapper(entity, enabled);
            }

            tmp.add(wrapper);
        }

        this.entities = tmp;
    }

    public List getSelectedEntities() {
        Iterator it = entities.iterator();
        List selected = new ArrayList();
        while (it.hasNext()) {
            ClassGeneratorEntityWrapper wrapper = (ClassGeneratorEntityWrapper) it.next();
            if (wrapper.isSelected()) {
                selected.add(wrapper.getEntity());
            }
        }

        return selected;
    }

    public boolean selectAllEnabled() {
        boolean changed = false;

        Iterator it = entities.iterator();
        while (it.hasNext()) {
            ClassGeneratorEntityWrapper wrapper = (ClassGeneratorEntityWrapper) it.next();
            if (wrapper.isEnabled() && !wrapper.isSelected()) {
                wrapper.setSelected(true);
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Returns the map.
     * 
     * @return DataMap
     */
    public DataMap getMap() {
        return map;
    }

    /**
     * Returns the outputDir.
     * 
     * @return File
     */
    public File getOutputDirectory() {
        return (outputDir != null) ? new File(outputDir) : null;
    }

    /**
     * Returns the pairs.
     * 
     * @return boolean
     */
    public boolean isPairs() {
        return pairs;
    }

    /**
     * Sets the pairs.
     * 
     * @param pairs The pairs to set
     */
    public void setPairs(boolean pairs) {
        if (this.pairs != pairs) {
            this.pairs = pairs;
            this.defaults.setGeneratePairs(Boolean.valueOf(pairs));
            fireModelChange(VALUE_CHANGED, Selector.fromString("pairs"));
        }
    }

    public String getCustomClassTemplate() {
        return customClassTemplate;
    }

    public void setCustomClassTemplate(String customClassTemplate) {
        if (!Util.nullSafeEquals(this.customClassTemplate, customClassTemplate)) {
            this.customClassTemplate = customClassTemplate;
            this.defaults.setSubclassTemplate(customClassTemplate);
            fireModelChange(VALUE_CHANGED, Selector.fromString("customClassTemplate"));
        }
    }

    public String getCustomSuperclassTemplate() {
        return customSuperclassTemplate;
    }

    public void setCustomSuperclassTemplate(String customSuperclassTemplate) {
        if (!Util.nullSafeEquals(this.customSuperclassTemplate, customSuperclassTemplate)) {
            this.customSuperclassTemplate = customSuperclassTemplate;
            this.defaults.setSuperclassTemplate(customSuperclassTemplate);
            fireModelChange(VALUE_CHANGED, Selector
                    .fromString("customSuperclassTemplate"));
        }
    }

    /**
     * Returns the entities.
     * 
     * @return List
     */
    public List getEntities() {
        return entities;
    }

    /**
     * Returns the outputDir.
     * 
     * @return String
     */
    public String getOutputDir() {
        return outputDir;
    }

    /**
     * Sets the outputDir.
     * 
     * @param outputDir The outputDir to set
     */
    public void setOutputDir(String outputDir) {
        if (!Util.nullSafeEquals(this.outputDir, outputDir)) {
            this.outputDir = outputDir;
            this.defaults.setOutputPath(outputDir);
            fireModelChange(VALUE_CHANGED, Selector.fromString("outputDir"));
        }
    }

    public String getSuperClassPackage() {
        return superClassPackage;
    }

    public void setSuperClassPackage(String superClassPackage) {
        if (!Util.nullSafeEquals(this.superClassPackage, superClassPackage)) {
            this.superClassPackage = superClassPackage;

            defaults.setSuperclassPackage(superClassPackage);
            fireModelChange(VALUE_CHANGED, Selector.fromString("superClassPackage"));
        }
    }
}