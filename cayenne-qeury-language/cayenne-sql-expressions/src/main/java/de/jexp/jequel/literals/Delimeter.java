package de.jexp.jequel.literals;

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
