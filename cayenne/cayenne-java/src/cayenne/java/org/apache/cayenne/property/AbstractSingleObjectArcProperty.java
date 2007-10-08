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

package org.apache.cayenne.property;


/**
 * A base superclass of SingleObjectArcProperty implementors.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public abstract class AbstractSingleObjectArcProperty extends SimpleProperty implements
        SingleObjectArcProperty {

    protected String complimentaryReverseArcName;
    protected ClassDescriptor targetDescriptor;

    public AbstractSingleObjectArcProperty(ClassDescriptor owner,
            ClassDescriptor targetDescriptor, PropertyAccessor accessor,
            String reverseName) {
        super(owner, accessor);
        this.targetDescriptor = targetDescriptor;
        this.complimentaryReverseArcName = reverseName;
    }

    public void setTarget(Object source, Object target, boolean setReverse) {
        Object oldTarget = readProperty(source);
        if (oldTarget == target) {
            return;
        }

        // TODO, Andrus, 2/9/2006 - CayenneDataObject also invokes "willConnect" and has a
        // callback to ObjectStore to handle flattened....

        if (setReverse) {
            setReverse(source, oldTarget, target);
        }

        writeProperty(source, oldTarget, target);
    }

    protected void setReverse(
            final Object source,
            final Object oldTarget,
            final Object newTarget) {

        ArcProperty reverseArc = getComplimentaryReverseArc();

        if (reverseArc != null) {

            // unset old
            if (oldTarget != null) {

                PropertyVisitor visitor = new PropertyVisitor() {

                    public boolean visitCollectionArc(CollectionProperty property) {
                        property.removeTarget(oldTarget, source, false);
                        return false;
                    }

                    public boolean visitSingleObjectArc(SingleObjectArcProperty property) {
                        property.setTarget(oldTarget, null, false);
                        return false;
                    }

                    public boolean visitProperty(Property property) {
                        return false;
                    }
                };

                reverseArc.visit(visitor);
            }

            // set new reverse
            if (newTarget != null) {
                PropertyVisitor visitor = new PropertyVisitor() {

                    public boolean visitCollectionArc(CollectionProperty property) {
                        property.addTarget(newTarget, source, false);
                        return false;
                    }

                    public boolean visitSingleObjectArc(SingleObjectArcProperty property) {
                        property.setTarget(newTarget, source, false);
                        return false;
                    }

                    public boolean visitProperty(Property property) {
                        return false;
                    }
                };

                reverseArc.visit(visitor);
            }
        }
    }

    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitSingleObjectArc(this);
    }

    public ArcProperty getComplimentaryReverseArc() {
        return (ArcProperty) targetDescriptor.getProperty(complimentaryReverseArcName);
    }

    public ClassDescriptor getTargetDescriptor() {
        return targetDescriptor;
    }

    public abstract boolean isFault(Object target);
}
