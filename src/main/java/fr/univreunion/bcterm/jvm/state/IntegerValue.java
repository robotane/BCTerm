package fr.univreunion.bcterm.jvm.state;

/**
 * Represents an integer value in the JVM state
 */
public class IntegerValue implements Value {
    private final int value;

    /**
     * Creates a new integer value.
     * @param value The integer value to store
     */
    public IntegerValue(int value) {
        this.value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isInteger() {
        return true;
    }

    @Override
    public boolean isLocation() {
        return false;
    }

    @Override
    public int getIntValue() {
        return value;
    }

    @Override
    public LocationValue getLocationValue() {
        throw new IllegalStateException("Integer value cannot be converted to location");
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IntegerValue)) return false;
        IntegerValue other = (IntegerValue) obj;
        return value == other.value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
