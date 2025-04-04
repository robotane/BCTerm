package fr.univreunion.bcterm.jvm.state;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an object in the JVM heap.
 * An object maps field identifiers to values and has a class tag.
 */
public class JVMObject {
    /** The class tag (k) */
    private final String classTag;

    /** The map of field identifiers to their values */
    private final Map<String, Value> fields;
    /**
     * Creates a new JVM object with the specified class tag
     * @param classTag The class tag (k)
     */
    public JVMObject(String classTag) {
        this.classTag = classTag;
        this.fields = new HashMap<>();
    }

    /**
     * Gets the value of a field
     * @param fieldName The field identifier
     * @return The value of the field
     */
    public Value getField(String fieldName) {
        return fields.getOrDefault(fieldName, Value.NULL);
    }

    /**
     * Sets the value of a field
     * @param fieldName The field identifier
     * @param value The value to set
     */
    public void setField(String fieldName, Value value) {
        fields.put(fieldName, value);
    }

    /**
     * Gets the class tag of this object
     * @return The class tag (k)
     */
    public String getClassTag() {
        return classTag;
    }

    /**
     * Gets the class name of this object
     * @return The class name
     */
    public String getClassName() {
        return classTag;
    }

    /**
     * Checks if this object belongs to the specified class
     * @param className The class name to check
     * @return true if this object belongs to the specified class (has class tag k equal to className)
     */
    public boolean belongsToClass(String className) {
        return classTag.equals(className);
    }

    /**
     * Gets all field values in this object
     * @return Collection of all field values
     */
    public Collection<Value> getFieldValues() {
        return fields.values();
    }

    /**
     * Gets all fields in this object
     * @return Map of field names to values
     */
    public Map<String, Value> getFields() {
        return new HashMap<>(fields);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(classTag).append("[");

        boolean first = true;
        for (String key : fields.keySet()) {
            if (!first) sb.append(", ");
            sb.append(key).append("->").append(fields.get(key));
            first = false;
        }

        sb.append("]");
        return sb.toString();
    }
}