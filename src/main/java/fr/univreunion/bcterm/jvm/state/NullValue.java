package fr.univreunion.bcterm.jvm.state;

/**
 * Represents a null value in the JVM state
 */
public class NullValue implements Value {

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public boolean isInteger() {
        return false;
    }

    @Override
    public boolean isLocation() {
        return false;
    }

    @Override
    public int getIntValue() {
        throw new IllegalStateException("Null value cannot be converted to integer");
    }

    @Override
    public LocationValue getLocationValue() {
        throw new IllegalStateException("Null value cannot be converted to location");
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullValue;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
