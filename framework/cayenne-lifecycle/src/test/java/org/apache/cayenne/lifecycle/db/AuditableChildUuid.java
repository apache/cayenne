package org.apache.cayenne.lifecycle.db;

import org.apache.cayenne.lifecycle.audit.AuditableChild;
import org.apache.cayenne.lifecycle.db.auto._AuditableChildUuid;
import org.apache.cayenne.lifecycle.relationship.ObjectIdRelationship;

@ObjectIdRelationship(_AuditableChildUuid.UUID_PROPERTY)
@AuditableChild(objectIdRelationship = _AuditableChildUuid.UUID_PROPERTY)
public class AuditableChildUuid extends _AuditableChildUuid {
}
