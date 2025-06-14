package fr.univreunion.bcterm.jvm.state;

/**
 * Interface representing the values of the JVM state.
 */
public interface Value {

    /**
     * A constant representing a null value in the JVM state
     */
    final Value NULL = new NullValue();

    /**
     * Returns Java object representing this value
     * 
     * @return the Java object representing this value
     */
    Object getValue();

    /**
     * Creates and returns a deep copy of this value.
     * 
     * @return A new Value instance that is a deep copy of this value
     */
    Value deepCopy();
}
