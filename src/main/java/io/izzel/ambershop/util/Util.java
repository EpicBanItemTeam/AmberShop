package io.izzel.ambershop.util;

import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class Util {

    public boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<Double> asDouble(String str) {
        try {
            return Optional.of(Double.parseDouble(str));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<Integer> asInteger(String str) {
        try {
            return Optional.of(Integer.parseInt(str));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
