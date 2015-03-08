package de.jexp.jequel.table;

public interface Table extends TablePart {
    Table resolve();

    Field getField(String name);
}
