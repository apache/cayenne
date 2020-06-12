
package org.apache.cayenne.reflect.generic;

import java.math.BigDecimal;
import java.util.function.BiFunction;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.util.Util;

/**
 * @since 4.2
 */
public class DefaultComparisionStrategyFactory implements ComparisionStrategyFactory {

    private static final BiFunction<Object, Object, Boolean> DEFAULT_STRATEGY = Util::nullSafeEquals;

    private static final BiFunction<Object, Object, Boolean> BIG_DECIMAL_STRATEGY = (o1, o2) -> {
        if(o1 == o2) {
            return true;
        }
        if(o1 == null || o2 == null) {
            return false;
        }
        BigDecimal bigDecimal1 = (BigDecimal)o1;
        BigDecimal bigDecimal2 = (BigDecimal)o2;
        return bigDecimal1.compareTo(bigDecimal2) == 0;
    };

    @Override
    public BiFunction<Object, Object, Boolean> getStrategy(ObjAttribute attribute) {
        if(BigDecimal.class.equals(attribute.getJavaClass())) {
            return BIG_DECIMAL_STRATEGY;
        }
        return DEFAULT_STRATEGY;
    }
}
