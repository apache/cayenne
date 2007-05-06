/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.dialog.codegen;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.Predicate;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.validation.ValidationFailure;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * A base superclass of a top controller for the code generator. Defines all common model
 * parts used in class generation.
 * 
 * @author Andrus Adamchik
 */
public abstract class CodeGeneratorControllerBase extends CayenneController {

    public static final String SELECTED_PROPERTY = "selected";

    protected DataMap dataMap;

    protected ValidationResult validation;

    protected List entities;
    protected Set selectedEntities;

    protected transient ObjEntity currentEntity;

    public CodeGeneratorControllerBase(CayenneController parent, DataMap dataMap) {
        super(parent);

        this.dataMap = dataMap;
        this.entities = new ArrayList(dataMap.getObjEntities());
        this.selectedEntities = new HashSet();
    }

    public abstract Component getView();

    public void validate(GeneratorController validator) {

        ValidationResult validationBuffer = new ValidationResult();

        if (validator != null) {
            Iterator it = entities.iterator();
            while (it.hasNext()) {
                validator.validateEntity(validationBuffer, (ObjEntity) it.next(), false);
            }
        }

        this.validation = validationBuffer;
    }

    public boolean updateSelection(Predicate predicate) {

        boolean modified = false;

        Iterator it = entities.iterator();
        while (it.hasNext()) {
            ObjEntity entity = (ObjEntity) it.next();

            boolean select = predicate.evaluate(entity);

            if (select) {
                if (selectedEntities.add(entity.getName())) {
                    modified = true;
                }
            }
            else {
                if (selectedEntities.remove(entity.getName())) {
                    modified = true;
                }
            }
        }

        if (modified) {
            firePropertyChange(SELECTED_PROPERTY, null, null);
        }

        return modified;
    }

    public List getSelectedEntities() {
        List selected = new ArrayList(selectedEntities.size());

        Iterator it = entities.iterator();
        while (it.hasNext()) {
            ObjEntity e = (ObjEntity) it.next();
            if (selectedEntities.contains(e.getName())) {
                selected.add(e);
            }
        }

        return selected;
    }

    public int getSelectedEntitiesSize() {
        return selectedEntities.size();
    }

    public List getEntities() {
        return entities;
    }

    /**
     * Returns the first encountered validation problem for an antity matching the name or
     * null if the entity is valid or the entity is not present.
     */
    public String getProblem(String entityName) {

        if (validation == null) {
            return null;
        }

        List failures = validation.getFailures(entityName);
        if (failures.isEmpty()) {
            return null;
        }

        return ((ValidationFailure) failures.get(0)).getDescription();
    }

    public boolean isSelected() {
        return currentEntity != null
                ? selectedEntities.contains(currentEntity.getName())
                : false;
    }

    public void setSelected(boolean selectedFlag) {
        if (currentEntity == null) {
            return;
        }

        if (selectedFlag) {
            if (selectedEntities.add(currentEntity.getName())) {
                firePropertyChange(SELECTED_PROPERTY, null, null);
            }
        }
        else {
            if (selectedEntities.remove(currentEntity.getName())) {
                firePropertyChange(SELECTED_PROPERTY, null, null);
            }
        }
    }

    public ObjEntity getCurrentEntity() {
        return currentEntity;
    }

    public void setCurrentEntity(ObjEntity currentEntity) {
        this.currentEntity = currentEntity;
    }

    public DataMap getDataMap() {
        return dataMap;
    }
}
