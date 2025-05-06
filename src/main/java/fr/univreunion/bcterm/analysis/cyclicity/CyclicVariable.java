package fr.univreunion.bcterm.analysis.cyclicity;

/**
 * Represents a variable that points to a cyclic data structure.
 * This class tracks variables containing cycles in their referenced object
 * graphs.
 * 
 * @param variableName the name of the variable associated with the cyclic
 *                     reference
 */
public class CyclicVariable {
    private final String variableName;

    public CyclicVariable(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CyclicVariable other = (CyclicVariable) obj;
        return variableName.equals(other.variableName);
    }

    @Override
    public int hashCode() {
        return variableName.hashCode();
    }

    @Override
    public String toString() {
        return variableName;
    }
}
