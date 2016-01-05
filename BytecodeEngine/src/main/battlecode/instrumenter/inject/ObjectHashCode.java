package battlecode.instrumenter.inject;

import org.hibernate.search.util.WeakIdentityHashMap;

import java.lang.reflect.Method;
import java.util.HashMap;

@SuppressWarnings("unused")
public class ObjectHashCode {

    static final Method objectHashCode;
    static final Method enumHashCode;
    static final Method characterHashCode;

    static {
        Method tmpo = null, tmpe = null, tmpc = null;
        try {
            tmpo = Object.class.getMethod("hashCode");
            tmpe = Enum.class.getMethod("hashCode");
            tmpc = Character.class.getMethod("hashCode");
        } catch (Exception e) {
            throw new RuntimeException("Can't load needed functions", e);
        }

        objectHashCode = tmpo;
        enumHashCode = tmpe;
        characterHashCode = tmpc;
    }

    static int lastHashCode = -1;

    static WeakIdentityHashMap<Object, Integer> codes = new WeakIdentityHashMap<>();

    // reflection is slow so cache the results
    static HashMap<Class, Boolean> usesOHC = new HashMap<>();

    static public int hashCode(Object o) throws NoSuchMethodException {
        if (usesObjectHashCode(o.getClass()))
            return identityHashCode(o);
        else
            return o.hashCode();
    }

    static private boolean usesObjectHashCode(Class<?> cl) throws NoSuchMethodException {
        Boolean b = usesOHC.get(cl);
        if (b == null) {
            Method hashCodeMethod = cl.getMethod("hashCode");
            b = hashCodeMethod.equals(enumHashCode) ||
                    hashCodeMethod.equals(objectHashCode) ||
                    hashCodeMethod.equals(characterHashCode);
            usesOHC.put(cl, b);
        }
        return b;
    }

    static public int identityHashCode(Object o) {
        Integer code = codes.get(o);
        if (code == null) {
            codes.put(o, ++lastHashCode);
            return lastHashCode;
        } else
            return code;
    }

    private ObjectHashCode() {
    }

}
