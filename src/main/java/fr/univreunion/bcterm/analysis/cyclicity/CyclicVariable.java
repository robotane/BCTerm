package fr.univreunion.bcterm.analysis.cyclicity;

import java.util.Objects;

public class CyclicVariable {
    private final String variableName;

    public CyclicVariable(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CyclicVariable that = (CyclicVariable) o;
        return Objects.equals(variableName, that.variableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName);
    }

    @Override
    public String toString() {
        return variableName;
    }
}