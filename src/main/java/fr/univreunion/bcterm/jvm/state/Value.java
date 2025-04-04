package fr.univreunion.bcterm.jvm.state;

/**
 * Interface representing a value in the JVM state
 */
public interface Value {
    /**
     * Null value constant
     */
    Value NULL = new NullValue();

    static Value createInteger(int value) {
        return new IntegerValue(value);
    }

    static Value createLocation(long address) {
        return new LocationValue(address);
    }

    static Value createLocation(long objectId, String typeName) {
        return new LocationValue(objectId, typeName);
    }

    // TODO: Maybe I should use getType instead of checking the type?

    /**
     * Check if this value is null
     */
    boolean isNull();

    /**
     * Check if this value is an integer
     */
    boolean isInteger();

    /**
     * Check if this value is a location
     */
    boolean isLocation();

    /**
     * Get the integer value if this is an integer
     * @throws IllegalStateException if this is not an integer
     */
    int getIntValue();

    /**
     * Get the location value if this is a location
     * @throws IllegalStateException if this is not a location
     */
    LocationValue getLocationValue();
}
