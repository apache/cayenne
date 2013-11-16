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
package org.apache.cayenne.reflect;

import org.apache.cayenne.map.ObjRelationship;

/**
 * A base implementation of the {@link ArcProperty}.
 * 
 * @since 3.0
 */
public abstract class BaseArcProperty extends BaseProperty implements ArcProperty {

    protected String complimentaryReverseArcName;
    protected ClassDescriptor targetDescriptor;
    protected ObjRelationship relationship;
    protected String reverseDbPath;

    public BaseArcProperty(ClassDescriptor owner, ClassDescriptor targetDescriptor, Accessor accessor,
            String reverseName) {

        super(owner, accessor);

        this.targetDescriptor = targetDescriptor;
        this.complimentaryReverseArcName = reverseName;
        this.relationship = owner.getEntity().getRelationship(getName());
    }

    @Override
    public abstract boolean visit(PropertyVisitor visitor);

    public abstract boolean isFault(Object source);
    
    @Override
    public String getComplimentaryReverseDbRelationshipPath() {
        if (reverseDbPath == null) {
            reverseDbPath = relationship.getReverseDbRelationshipPath();
        }

        return reverseDbPath;
    }

    public ObjRelationship getRelationship() {
        return relationship;
    }

    public ArcProperty getComplimentaryReverseArc() {
        return (ArcProperty) targetDescriptor.getProperty(complimentaryReverseArcName);
    }

    public ClassDescriptor getTargetDescriptor() {
        return targetDescriptor;
    }

    /**
     * A convenience method to set the reverse arc used by subclasses.
     */
    protected void setReverse(final Object source, final Object oldTarget, final Object newTarget) {

        ArcProperty reverseArc = getComplimentaryReverseArc();

        if (reverseArc != null) {

            // unset old
            if (oldTarget != null) {

                PropertyVisitor visitor = new PropertyVisitor() {

                    public boolean visitToMany(ToManyProperty property) {
                        property.removeTarget(oldTarget, source, false);
                        return false;
                    }

                    public boolean visitToOne(ToOneProperty property) {
                        property.setTarget(oldTarget, null, false);
                        return false;
                    }

                    public boolean visitAttribute(AttributeProperty property) {
                        return false;
                    }
                };

                reverseArc.visit(visitor);
            }

            // set new reverse
            if (newTarget != null) {
                PropertyVisitor visitor = new PropertyVisitor() {

                    public boolean visitToMany(ToManyProperty property) {
                        property.addTarget(newTarget, source, false);
                        return false;
                    }

                    public boolean visitToOne(ToOneProperty property) {
                        property.setTarget(newTarget, source, false);
                        return false;
                    }

                    public boolean visitAttribute(AttributeProperty property) {
                        return false;
                    }
                };

                reverseArc.visit(visitor);
            }
        }
    }
}
