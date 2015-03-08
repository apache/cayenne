package de.jexp.jequel.generator;

import java.util.*;

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
