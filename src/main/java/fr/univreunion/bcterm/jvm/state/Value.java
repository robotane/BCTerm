package fr.univreunion.bcterm.jvm.state;

/**
 * Interface representing the values of the JVM state.
 */
public interface Value {

    /**
     * A constant representing a null value in the JVM state
     */

    Value NULL = new NullValue();

    /**
     * Returns Java object representing this value
     * 
     * @return the Java object representing this value
     */
    Object getValue();
}
