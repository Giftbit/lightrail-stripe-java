package com.lightrail.helpers;

import java.util.HashMap;
import java.util.Map;


public class Currency {
    static Map<String, Integer> majorToMinorRatioMap = new HashMap<>();

    {
        majorToMinorRatioMap.put("BIF", 1);
        majorToMinorRatioMap.put("CLP", 1);
        majorToMinorRatioMap.put("DJF", 1);
        majorToMinorRatioMap.put("GNF", 1);
        majorToMinorRatioMap.put("JPY", 1);
        majorToMinorRatioMap.put("KMF", 1);
        majorToMinorRatioMap.put("KRW", 1);
        majorToMinorRatioMap.put("MGA", 1);
        majorToMinorRatioMap.put("PYG", 1);
        majorToMinorRatioMap.put("RWF", 1);
        majorToMinorRatioMap.put("VND", 1);
        majorToMinorRatioMap.put("VUV", 1);
        majorToMinorRatioMap.put("XAF", 1);
        majorToMinorRatioMap.put("XOF", 1);
        majorToMinorRatioMap.put("XPF", 1);
    }

    private static int getConversionRatio(String currency) {
        int conversionRatio = 100;
        if (majorToMinorRatioMap.containsKey(currency)) {
            conversionRatio = majorToMinorRatioMap.get(currency);
        }
        return conversionRatio;
    }

    public static int majorToMinor(float value, String currency) {
        Double minorValue;
        int conversionRatio = getConversionRatio(currency);
        minorValue = (Math.round(value * conversionRatio * 100.0) / 100.0);
        return minorValue.intValue();
    }

    public static float minorToMajor(int minorValue, String currency) {
        Double majorValue;
        int conversionRatio = getConversionRatio(currency);
        majorValue = Math.round(((float) minorValue / conversionRatio) * 100.0) / 100.0;
        return majorValue.floatValue();
    }

}
