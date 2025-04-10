package fr.univreunion.bcterm.jvm.state;

public class NullValue implements Value {

    @Override
    public ValueType getType() {
        return ValueType.NULL;
    }

    @Override
    public Object getValue() {
        return this;
    }

    @Override
    public String toString() {
        return "null";
    }

}
