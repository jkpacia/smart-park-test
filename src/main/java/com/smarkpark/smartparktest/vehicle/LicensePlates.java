package com.smarkpark.smartparktest.vehicle;

import java.util.Locale;

public final class LicensePlates {
    private LicensePlates() {
    }

    public static String normalize(String plate) {
        return plate == null ? null : plate.trim().toUpperCase(Locale.ROOT);
    }
}

