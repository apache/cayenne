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

package org.apache.cayenne;

import org.apache.art.ArtGroup;

/**
 * Some more tests regarding reflexive relationships, especially related to delete rules
 * etc.  The implementation is hairy, and so needs a really good workout.
 */
public class CDOReflexiveRelTest extends CayenneDOTestBase {

	private void failWithException(Exception e) {
			e.printStackTrace();
			fail("Should not have thrown an exception :"+e.getMessage());
	}

	public void testAddDeleteNoCommit() {
		ArtGroup parentGroup=(ArtGroup)ctxt.newObject("ArtGroup");
		parentGroup.setName("parent");

		ArtGroup childGroup1=(ArtGroup)ctxt.newObject("ArtGroup");
		childGroup1.setName("child1");
		childGroup1.setToParentGroup(parentGroup);

		try {
			ctxt.deleteObject(parentGroup);
		} catch (Exception e) {
			this.failWithException(e);
		}
	}
	
	public void testAddDeleteWithCommit() {
		ArtGroup parentGroup=(ArtGroup)ctxt.newObject("ArtGroup");
		parentGroup.setName("parent");

		ArtGroup childGroup1=(ArtGroup)ctxt.newObject("ArtGroup");
		childGroup1.setName("child1");
		childGroup1.setToParentGroup(parentGroup);
		ctxt.commitChanges();

		try {
			ctxt.deleteObject(parentGroup);
			ctxt.commitChanges();
		} catch (Exception e) {
			this.failWithException(e);
		}
		
	}
	
	public void testReplaceDeleteNoCommit() {
		ArtGroup parentGroup1=(ArtGroup)ctxt.newObject("ArtGroup");
		parentGroup1.setName("parent1");
		ArtGroup parentGroup2=(ArtGroup)ctxt.newObject("ArtGroup");
		parentGroup2.setName("parent2");

		ArtGroup childGroup1=(ArtGroup)ctxt.newObject("ArtGroup");
		childGroup1.setName("child1");
		childGroup1.setToParentGroup(parentGroup1);


		childGroup1.setToParentGroup(parentGroup2);
		try {
			ctxt.deleteObject(parentGroup1);
			ctxt.deleteObject(parentGroup2);
		} catch (Exception e) {
			this.failWithException(e);
		}		
	}
	
	public void testReplaceDeleteWithCommit() {
		ArtGroup parentGroup1=(ArtGroup)ctxt.newObject("ArtGroup");
		parentGroup1.setName("parent1");
		ArtGroup parentGroup2=(ArtGroup)ctxt.newObject("ArtGroup");
		parentGroup2.setName("parent2");

		ArtGroup childGroup1=(ArtGroup)ctxt.newObject("ArtGroup");
		childGroup1.setName("child1");
		childGroup1.setToParentGroup(parentGroup1);
		childGroup1.setToParentGroup(parentGroup2);
		ctxt.commitChanges();

		try {
			ctxt.deleteObject(parentGroup1);
			ctxt.deleteObject(parentGroup2);
			ctxt.commitChanges();
		} catch (Exception e) {
			this.failWithException(e);
		}		
	}
	
	public void testCommitReplaceCommit() {
		ArtGroup parentGroup1=(ArtGroup)ctxt.newObject("ArtGroup");
		parentGroup1.setName("parent1");
		ArtGroup parentGroup2=(ArtGroup)ctxt.newObject("ArtGroup");
		parentGroup2.setName("parent2");

		ArtGroup childGroup1=(ArtGroup)ctxt.newObject("ArtGroup");
		childGroup1.setName("child1");
		childGroup1.setToParentGroup(parentGroup1);
		ctxt.commitChanges();
		childGroup1.setToParentGroup(parentGroup2);
		ctxt.commitChanges();
	}

	public void testComplexInsertUpdateOrdering() {
		ArtGroup parentGroup=(ArtGroup)ctxt.newObject("ArtGroup");
		parentGroup.setName("parent");
		ctxt.commitChanges();
		
		//Check that the update and insert both work write
		ArtGroup childGroup1=(ArtGroup)ctxt.newObject("ArtGroup");
		childGroup1.setName("child1");
		childGroup1.setToParentGroup(parentGroup);
		ctxt.commitChanges();
	}

}
