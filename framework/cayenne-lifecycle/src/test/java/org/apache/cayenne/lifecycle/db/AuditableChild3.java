package org.apache.cayenne.lifecycle.db;

import org.apache.cayenne.lifecycle.audit.AuditableChild;
import org.apache.cayenne.lifecycle.db.auto._AuditableChild3;

@AuditableChild(value = _AuditableChild3.PARENT_PROPERTY, ignoredProperties = _AuditableChild3.CHAR_PROPERTY1_PROPERTY)
public class AuditableChild3 extends _AuditableChild3 {

}
