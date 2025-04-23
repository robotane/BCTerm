package fr.univreunion.bcterm.jvm.state;

/**
 * Represents an integer value in the JVM state
 */
public class IntegerValue implements Value {

    private final Integer value;

    /**
     * Creates a new IntegerValue with the given integer value
     * 
     * @param value The integer value to store
     */
    public IntegerValue(Integer value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

}
