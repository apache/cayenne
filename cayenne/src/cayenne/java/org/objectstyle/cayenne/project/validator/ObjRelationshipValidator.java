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
package org.objectstyle.cayenne.project.validator;

import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public class ObjRelationshipValidator extends TreeNodeValidator {

    /**
     * Constructor for ObjRelationshipValidator.
     */
    public ObjRelationshipValidator() {
        super();
    }

    public void validateObject(ProjectPath path, Validator validator) {
        ObjRelationship rel = (ObjRelationship) path.getObject();

        // skip validation of inherited relationships
        if (path.getObjectParent() != null
                && path.getObjectParent() != rel.getSourceEntity()) {
            return;
        }

        if (Util.isEmptyString(rel.getName())) {
            validator.registerError("Unnamed ObjRelationship.", path);
        }
        // check if there are attributes having the same name
        else if (rel.getSourceEntity().getAttribute(rel.getName()) != null) {
            validator.registerWarning(
                    "ObjRelationship has the same name as one of ObjAttributes",
                    path);
        }
        else {
            MappingNamesHelper helper = MappingNamesHelper.getInstance();
            String invalidChars = helper.invalidCharsInObjPathComponent(rel.getName());

            if (invalidChars != null) {
                validator.registerWarning(
                        "ObjRelationship name contains invalid characters: "
                                + invalidChars,
                        path);
            }
            else if (helper.invalidDataObjectProperty(rel.getName())) {
                validator.registerWarning("ObjRelationship name is invalid: "
                        + rel.getName(), path);
            }
        }

        if (rel.getTargetEntity() == null) {
            validator.registerWarning("ObjRelationship has no target entity.", path);
        }
        else {
            // check for missing DbRelationship mappings
            List dbRels = rel.getDbRelationships();
            if (dbRels.size() == 0) {
                validator.registerWarning(
                        "ObjRelationship has no DbRelationship mapping.",
                        path);
            }
            else {
                DbEntity expectedSrc = ((ObjEntity) rel.getSourceEntity()).getDbEntity();
                DbEntity expectedTarget = ((ObjEntity) rel.getTargetEntity())
                        .getDbEntity();

                if (((DbRelationship) dbRels.get(0)).getSourceEntity() != expectedSrc
                        || ((DbRelationship) dbRels.get(dbRels.size() - 1))
                                .getTargetEntity() != expectedTarget) {
                    validator.registerWarning(
                            "ObjRelationship has incomplete DbRelationship mapping.",
                            path);
                }
            }
        }

        //Disallow a Nullify delete rule where the relationship is toMany and the
        //foreign key attributes are mandatory.
        if (rel.isToMany()
                && !rel.isFlattened()
                && (rel.getDeleteRule() == DeleteRule.NULLIFY)) {
            ObjRelationship inverse = rel.getReverseRelationship();
            if (inverse != null) {
                DbRelationship firstRel = (DbRelationship) inverse
                        .getDbRelationships()
                        .get(0);
                Iterator attributePairIterator = firstRel.getJoins().iterator();
                while (attributePairIterator.hasNext()) {
                    DbJoin pair = (DbJoin) attributePairIterator.next();
                    if (pair.getSource().isMandatory()) {
                        validator
                                .registerWarning(
                                        "ObjRelationship "
                                                + rel.getName()
                                                + " has a Nullify delete rule and a mandatory reverse relationship ",
                                        path);
                    }
                }
            }
        }
    }
}