package com.mrzsh;

/**
 * @program: javapromise
 * @description: mutable long
 * @author: Mr.zsh
 * @create: 2019-01-14 17:37
 **/
public class MutableLong extends Number
        implements Comparable<MutableLong>{
    private volatile long val;

    public static MutableLong hold(long value){
        return new MutableLong(value);
    }

    private MutableLong(long val){
        set(val);
    }

    public void set(long val){
        this.val = val;
    }

    public long get() {
        return val;
    }

    public MutableLong sub(long suber){
        val -= suber;
        return this;
    }

    public MutableLong add(long adder){
        val += adder;
        return this;
    }

    public MutableLong subBy(long toSuber){
        val = toSuber - val;
        return this;
    }

    @Override
    public int intValue() {
        return (int) val;
    }

    @Override
    public long longValue() {
        return val;
    }

    @Override
    public float floatValue() {
        return (float)val;
    }

    @Override
    public double doubleValue() {
        return (double)val;
    }

    @Override
    public int compareTo(MutableLong o) {
        return Long.compare(val, o.val);
    }

    @Override
    public boolean equals(Object another) {
        if( another instanceof MutableLong ) {
            return ((MutableLong)another).val == this.val;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(val);
    }
}
