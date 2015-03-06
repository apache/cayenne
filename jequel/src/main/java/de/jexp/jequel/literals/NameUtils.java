package de.jexp.jequel.literals;

/**
 * @author mh14 @ jexp.de
 * @since 12.11.2007 02:33:06 (c) 2007 jexp.de
 */
public class NameUtils {
    public static String constantNameToLowerCaseLiteral(String constantName) {
        return constantName.toLowerCase().replaceAll("_", " ");
    }
}
