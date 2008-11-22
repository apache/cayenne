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

package org.apache.cayenne.project.validator;

import java.io.File;

import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.TstProject;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Test cases for the Validator class.
 * 
 */
public class ValidatorTest extends CayenneCase {
    
	public void testProject() throws Exception {
		Project project = new TstProject(new File(System.getProperty("user.dir")));
		Validator validator = new Validator(project);
		assertSame(project, validator.getProject());
	}

/*	

	
	public void testValidateDbAttributes() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");

		DbAttribute a1 = new DbAttribute();
		a1.setName("a1");
		a1.setType(Types.CHAR);
		a1.setMaxLength(2);
		DbEntity e1 = new DbEntity("e1");
		map.addDbEntity(e1);
		e1.addAttribute(a1);
		validator.reset();
		validator.validateDbAttributes(d1, map, e1);
		assertValidator(ValidationResult.VALID);

		// should complain about no max length
		DbAttribute a3 = new DbAttribute();
		a3.setName("a3");
		a3.setType(Types.CHAR);
		DbEntity e3 = new DbEntity("e3");
		map.addDbEntity(e3);
		e3.addAttribute(a3);
		validator.reset();
		validator.validateDbAttributes(d1, map, e3);
		assertValidator(ValidationResult.WARNING);

		// should complain about no type
		DbAttribute a4 = new DbAttribute();
		a4.setName("a4");
		DbEntity e4 = new DbEntity("e4");
		map.addDbEntity(e4);
		e4.addAttribute(a4);
		validator.reset();
		validator.validateDbAttributes(d1, map, e4);
		assertValidator(ValidationResult.WARNING);
	}

	public void testValidateObjRels() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		ObjRelationship or1 = buildValidObjRelationship("r1");
		validator.reset();
		validator.validateObjRels(d1, map, (ObjEntity) or1.getSourceEntity());
		assertValidator(ValidationResult.VALID);

		// no target entity, must give a warning
		ObjRelationship or2 = buildValidObjRelationship("r2");
		or2.setTargetEntity(null);
		validator.reset();
		validator.validateObjRels(d1, map, (ObjEntity) or2.getSourceEntity());
		assertValidator(ValidationResult.WARNING);

		// no DbRelationship mapping, must give a warning
		ObjRelationship or3 = buildValidObjRelationship("r2");
		or3.clearDbRelationships();
		validator.reset();
		validator.validateObjRels(d1, map, (ObjEntity) or3.getSourceEntity());
		assertValidator(ValidationResult.WARNING);

		// no name, must give an error
		ObjRelationship or4 = buildValidObjRelationship("r2");
		or4.setName(null);
		validator.reset();
		validator.validateObjRels(d1, map, (ObjEntity) or4.getSourceEntity());
		assertValidator(ValidationResult.ERROR);
	}
	
    public void testValidateDbRels() throws Exception {
		// should succeed
		DataDomain d1 = new DataDomain("abc");
		DbRelationship dr1 = buildValidDbRelationship("r1");
		validator.reset();
		validator.validateDbRels(d1, map, (DbEntity) dr1.getSourceEntity());
		assertValidator(ValidationResult.VALID);
		
		// no target entity
		DbRelationship dr2 = buildValidDbRelationship("r2");
		dr2.setTargetEntity(null);
		validator.reset();
		validator.validateDbRels(d1, map, (DbEntity) dr2.getSourceEntity());
		assertValidator(ValidationResult.WARNING);
		
    	// no name
		DbRelationship dr3 = buildValidDbRelationship("r3");
		dr3.setName(null);
		validator.reset();
		validator.validateDbRels(d1, map, (DbEntity) dr3.getSourceEntity());
		assertValidator(ValidationResult.ERROR);		
		
		// no joins
		DbRelationship dr4 = buildValidDbRelationship("r4");
		dr4.removeAllJoins();
		validator.reset();
		validator.validateDbRels(d1, map, (DbEntity) dr4.getSourceEntity());
		assertValidator(ValidationResult.WARNING);		
	}



*/
}
