package org.apache.cayenne.lifecycle.db;

import org.apache.cayenne.lifecycle.audit.Auditable;
import org.apache.cayenne.lifecycle.db.auto._Auditable2;

@Auditable(ignoredProperties = _Auditable2.CHAR_PROPERTY1_PROPERTY)
public class Auditable2 extends _Auditable2 {

}
