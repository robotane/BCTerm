package fr.univreunion.bcterm.jvm.state;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an object in the JVM heap.
 * An object maps field identifiers to values and has a class tag.
 */
public class JVMObject {

    private final String classTag;

    /** The map of field identifiers to their values */
    private final Map<String, Value> fields;

    /**
     * Creates a new JVM object with the specified class tag.
     * 
     * @param classTag the class tag of the object
     */
    public JVMObject(String classTag) {
        this.classTag = classTag;
        this.fields = new HashMap<>();
    }

    /**
     * Returns the class tag of this object.
     * 
     * @return the class tag
     */
    public String getClassTag() {
        return classTag;
    }

    /**
     * Returns the value of the specified field.
     * 
     * @param fieldName the name of the field
     * @return the value of the field, or NULL if the field does not exist
     */
    public Value getField(String fieldName) {
        return fields.getOrDefault(fieldName, Value.NULL);
    }

    /**
     * Sets the value of the specified field.
     * 
     * @param fieldName the name of the field
     * @param value     the value to set
     */
    public void setField(String fieldName, Value value) {
        fields.put(fieldName, value);
    }

    /**
     * Gets all field values in this object
     * 
     * @return Collection of all field values
     */
    public Map<String, Value> getFields() {
        return fields;
    }

    /**
     * Gets all field names in this object
     * 
     * @return Collection of all field names
     */
    public Collection<String> getFieldNames() {
        return fields.keySet();
    }

    /**
     * Gets all field values in this object
     * 
     * @return Collection of all field values
     */
    public Collection<Value> getFieldValues() {
        return fields.values();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(classTag).append("[");

        boolean first = true;
        for (String key : fields.keySet()) {
            if (!first)
                sb.append(", ");
            sb.append(key).append("->").append(fields.get(key));
            first = false;
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * Creates and returns a deep copy of this JVMObject.
     * 
     * @return A new JVMObject that is a deep copy of this object
     */
    public JVMObject deepCopy() {
        JVMObject copy = new JVMObject(this.classTag);

        for (Map.Entry<String, Value> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Value fieldValue = entry.getValue();

            Value copiedValue = fieldValue != null ? fieldValue.deepCopy() : Value.NULL;
            copy.setField(fieldName, copiedValue);
        }

        return copy;
    }
}