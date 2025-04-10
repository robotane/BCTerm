package fr.univreunion.bcterm.jvm.state;

public class LocationValue implements Value {

    /** Memory address of the object referenced by this location. */
    private long address;

    /** Type name of the object referenced by this location. */
    private String typeName;

    /**
     * Creates a new location value.
     * 
     * @param address Memory address of the object referenced by this location
     */
    public LocationValue(long address) {
        this.address = address;
        this.typeName = null;
    }

    /**
     * Creates a new location value.
     * 
     * @param address  Memory address of the object referenced by this location
     * @param typeName Type name of the object referenced by this location
     */
    public LocationValue(long address, String typeName) {
        this.address = address;
        this.typeName = typeName;
    }

    /**
     * Returns the memory address of the object referenced by this location.
     * 
     * @return Memory address of the referenced object
     */
    public long getAddress() {
        return address;
    }

    /**
     * Returns the type name of the object referenced by this location.
     * 
     * @return Type name of the referenced object
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Sets the memory address of the object referenced by this location.
     * 
     * @param address Memory address of the referenced object
     */
    public void setAddress(long address) {
        this.address = address;
    }

    /**
     * Sets the type name of the object referenced by this location.
     * 
     * @param typeName Type name of the referenced object
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public ValueType getType() {
        return ValueType.LOCATION;
    }

    @Override
    public LocationValue getValue() {
        return this;
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
        if (this == obj)
            return true;
        if (!(obj instanceof LocationValue))
            return false;
        return address == ((LocationValue) obj).address;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(address);
    }

}
