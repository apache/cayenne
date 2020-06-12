package org.apache.cayenne.reflect.generic;

import java.util.function.BiFunction;

import org.apache.cayenne.map.ObjAttribute;

/**
 * @since 4.2
 */
public interface ComparisionStrategyFactory {

    BiFunction<Object, Object, Boolean>  getStrategy(ObjAttribute attribute);

}
