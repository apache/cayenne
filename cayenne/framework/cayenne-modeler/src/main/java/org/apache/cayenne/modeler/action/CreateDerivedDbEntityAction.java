/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.action;

import javax.swing.Action;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.project.NamedObjectFactory;

/**
 * @author Andrus Adamchik
 */
public class CreateDerivedDbEntityAction extends CreateDbEntityAction {

	public static String getActionName() {
		return "Create Derived DbEntity";
	}
    
	/**
	 * Constructor for CreateDerivedDbEntityAction.
	 */
	public CreateDerivedDbEntityAction(Application application) {
        super(application);
        super.setName(getActionName());
        super.putValue(Action.DEFAULT, getActionName());
        super.putValue(Action.SHORT_DESCRIPTION, getActionName());
    }
	
	public String getIconName() {
		return "icon-derived-dbentity.gif";
	}

	/**
	 * Constructs and returns a new DerivedDbEntity. Entity returned
	 * is added to the DataMap.
	 */
	protected DbEntity createEntity(DataMap map) {
		DbEntity entity =
			(DbEntity) NamedObjectFactory.createObject(DerivedDbEntity.class, map);
		map.addDbEntity(entity);
		return entity;
	}

}

