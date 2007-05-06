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
package org.objectstyle.cayenne.project.validator;

import java.io.File;

import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.TstProject;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * Test cases for the Validator class.
 * 
 * @author Andrei Adamchik
 */
public class ValidatorTst extends CayenneTestCase {
    
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
