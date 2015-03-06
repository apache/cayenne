package de.jexp.jequel.generator;

import java.util.*;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 20.10.2007 01:44:39
 */
public class TestResourceBundle extends ResourceBundle {
    private final Map<String, String> data;
    private final Locale locale;

    public TestResourceBundle(final Map<String, String> data, final Locale locale) {
        this.data = data;
        this.locale = locale;
    }

    protected Object handleGetObject(final String key) {
        return data.get(key);
    }

    public Enumeration<String> getKeys() {
        return Collections.enumeration(data.keySet());
    }

    public Locale getLocale() {
        return locale;
    }
}
