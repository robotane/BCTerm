package fr.univreunion.bcterm.jvm.state;

public class NullValue implements Value {

    @Override
    public Object getValue() {
        return this;
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public Value deepCopy() {
        return Value.NULL;
    }
}
