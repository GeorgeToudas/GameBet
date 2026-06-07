package GameBet;

import java.io.Serializable;
//to xrisimopeio me ton reducer kai stelnei ta key pairs auta gia na mhn stelnw oloklhra game obj 
public class KeyValue<K, V> implements Serializable {
    private static final long serialVersionUID = 1L;

    private K key;
    private V value;

    public KeyValue(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "(" + key + ", " + value + ")";
    }
}
