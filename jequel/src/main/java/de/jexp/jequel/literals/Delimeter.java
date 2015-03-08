package de.jexp.jequel.literals;

import de.jexp.jequel.literals.SqlKeyword;

public enum Delimeter implements SqlKeyword {
    COMMA(", "),
    EMPTY(""),
    POINT("."),
    SPACE(" "),
    SHARP("#");

    private final String delimString;

    Delimeter(String delimString) {
        this.delimString = delimString;
    }

    public String toString() {
        return delimString;
    }

    public String getSqlKeyword() {
        return delimString;
    }
}
