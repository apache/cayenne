package org.apache.cayenne.lifecycle.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A built-in annotation used to tag an object that is not auditable on its own, but whose
 * changes should be tracked together with changes of another ("parent") object. This
 * annotation allows to group changes in a closely related subtree of objects.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface AuditableChild {

    /**
     * Returns the name of a to-one relationship from an annotated object to the "parent"
     * object that should be audited when annotated object is changed.
     */
    String value();
}
