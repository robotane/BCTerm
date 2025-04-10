package fr.univreunion.bcterm.jvm.state;

/**
 * Represents the different types of values that can be stored in the JVM state.
 * NULL: Represents a null value
 * INTEGER: Represents an integer value
 * LOCATION: Represents a memory location/reference
 */
public enum ValueType {
    /** Represents a null value */
    NULL,
    /** Represents an integer value */
    INTEGER,
    /** Represents a memory location/reference */
    LOCATION
}