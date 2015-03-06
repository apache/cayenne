package de.jexp.jequel.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 20.10.2007 08:03:14
 */
public class CollectionUtils {
    public static Collection<String> asCollection(final Enumeration<String> enumeration) {
        final Collection<String> enumCollection = new HashSet<String>();
        while (enumeration.hasMoreElements()) {
            enumCollection.add(enumeration.nextElement());
        }
        return enumCollection;
    }
}
