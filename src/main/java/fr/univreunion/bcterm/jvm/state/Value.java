package fr.univreunion.bcterm.jvm.state;

/**
 * Interface representing the values of the JVM state.
 */
public interface Value {

    Value NULL = new NullValue();

    /**
     * Returns the type of this value
     * 
     * @return the type of this value
     */
    ValueType getType();

    /**
     * Returns Java object representing this value
     * 
     * @return the Java object representing this value
     */
    Object getValue();
}
