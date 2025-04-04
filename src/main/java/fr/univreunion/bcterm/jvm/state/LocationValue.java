package fr.univreunion.bcterm.jvm.state;

/**
 * Represents a location value in the JVM state
 */
public class LocationValue implements Value {
    /** The memory address of this location */
    private final long address;
    /** Optional type information */
    private String typeName;
    /**
     * Creates a new LocationValue with the specified address.
     * @param address The memory address of this location
     */
    public LocationValue(long address) {
        this.address = address;
    }

    /**
     * Creates a new LocationValue with the specified address and type name.
     * @param address The memory address of this location
     * @param typeName The name of the type stored at this location
     */
    public LocationValue(long address, String typeName) {
        this.address = address;
        this.typeName = typeName;
    }

    public long getAddress() {
        return address;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public boolean isInteger() {
        return false;
    }

    @Override
    public boolean isLocation() {
        return true;
    }

    @Override
    public int getIntValue() {
        throw new IllegalStateException("Location value cannot be converted to integer");
    }

    @Override
    public LocationValue getLocationValue() {
        return this;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public String toString() {
        if (typeName != null) {
            return "loc:" + typeName + "@" + Long.toHexString(address);
        }
        return "loc@" + Long.toHexString(address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LocationValue)) return false;
        return address == ((LocationValue) obj).address;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(address);
    }
}
