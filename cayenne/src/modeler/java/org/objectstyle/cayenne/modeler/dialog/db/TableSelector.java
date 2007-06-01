/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.modeler.dialog.db;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.validator.ValidationDisplayHandler;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.project.validator.ValidationInfo;
import org.objectstyle.cayenne.project.validator.Validator;
import org.objectstyle.cayenne.swing.ObjectBinding;
import org.objectstyle.cayenne.swing.TableBindingBuilder;

/**
 * @author Andrei Adamchik
 */
public class TableSelector extends CayenneController {

    protected TableSelectorView view;
    protected ObjectBinding tableBinding;

    protected DbEntity table;
    protected List tables;
    protected Map excludedTables;
    protected Map validationMessages;

    public TableSelector(ProjectController parent) {
        super(parent);
        this.view = new TableSelectorView();
        this.excludedTables = new HashMap();
        this.validationMessages = new HashMap();
        initController();
    }

    // ----- properties -----

    public Component getView() {
        return view;
    }

    /**
     * Called by table binding script to set current table.
     */
    public void setTable(DbEntity table) {
        this.table = table;
    }

    /**
     * Returns DbEntities that are excluded from DB generation.
     */
    public Collection getExcludedTables() {
        return excludedTables.values();
    }

    public List getTables() {
        return tables;
    }

    public boolean isIncluded() {
        if (table == null) {
            return false;
        }

        return !excludedTables.containsKey(table.getName());
    }

    public void setIncluded(boolean b) {
        if (table == null) {
            return;
        }

        if (b) {
            excludedTables.remove(table.getName());
        }
        else {
            excludedTables.put(table.getName(), table);
        }
    }

    public Object getProblem() {
        return (table != null) ? validationMessages.get(table.getName()) : null;
    }

    // ------ other stuff ------

    protected void initController() {
        TableBindingBuilder tableBuilder = new TableBindingBuilder(getApplication()
                .getBindingFactory(), this);

        tableBuilder.addColumn("Table", "#item.name", String.class, false);
        tableBuilder.addColumn(
                "Generate",
                "setTable(#item), included",
                Boolean.class,
                true);
        tableBuilder.addColumn(
                "Problems",
                "setTable(#item), problem",
                String.class,
                false);

        this.tableBinding = tableBuilder.bindToTable(view.getTables(), "tables");
    }

    /**
     * Performs validation of DbEntities in the current DataMap. Returns a collection of
     * ValidationInfo objects describing the problems.
     */
    public void updateTables(DataMap dataMap) {
        this.tables = new ArrayList(dataMap.getDbEntities());

        excludedTables.clear();
        validationMessages.clear();

        // if there were errors, filter out those related to
        // non-derived DbEntities...

        // TODO: this is inefficient.. we need targeted validation
        // instead of doing it on the whole project

        Validator validator = ((ProjectController) getParent())
                .getProject()
                .getValidator();
        int validationCode = validator.validate();
        if (validationCode >= ValidationDisplayHandler.WARNING) {

            Iterator it = validator.validationResults().iterator();
            while (it.hasNext()) {
                ValidationInfo nextProblem = (ValidationInfo) it.next();
                Entity failedEntity = null;

                if (nextProblem.getValidatedObject() instanceof DbAttribute) {
                    DbAttribute failedAttribute = (DbAttribute) nextProblem
                            .getValidatedObject();
                    failedEntity = failedAttribute.getEntity();
                }
                else if (nextProblem.getValidatedObject() instanceof DbRelationship) {
                    DbRelationship failedRelationship = (DbRelationship) nextProblem
                            .getValidatedObject();
                    failedEntity = failedRelationship.getSourceEntity();
                }
                else if (nextProblem.getValidatedObject() instanceof DbEntity) {
                    failedEntity = (Entity) nextProblem.getValidatedObject();
                }

                if (failedEntity == null) {
                    continue;
                }

                excludedTables.put(failedEntity.getName(), failedEntity);
                validationMessages.put(failedEntity.getName(), nextProblem.getMessage());
            }
        }

        // now do a pass through the tables and exclude derived
        Iterator tablesIt = tables.iterator();
        while (tablesIt.hasNext()) {
            DbEntity table = (DbEntity) tablesIt.next();
            if (table instanceof DerivedDbEntity) {
                excludedTables.put(table.getName(), table);
                validationMessages.put(table.getName(), "derived entity");
            }
        }

        tableBinding.updateView();
    }
}