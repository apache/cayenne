package de.jexp.jequel.table;

import java.util.Map;

public interface Table extends TablePart {
    Table resolve();

    Field getField(String name);

    Map<String, Field<?>> getFields();
}
