package util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Debugger {

    public static void debug(Object... os) {
        int n = os.length;
        for (int i = 0; i < n; i+=2) {
            System.out.printf("[%s: %s] ", os[i+1], process(os[i]));
        }
        System.out.println();
    }

    private static Object process(Object value) {
        if (value instanceof int[]) {
            value = Arrays.stream((int[])value).boxed().collect(Collectors.toList());
        } else if (value instanceof double[]) {
            value = Arrays.stream((double[])value).boxed().collect(Collectors.toList());
        }
        return value;
    }
}
