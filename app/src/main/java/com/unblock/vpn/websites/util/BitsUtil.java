package com.unblock.vpn.websites.util;

import java.util.Locale;

public class BitsUtil {
    private static final long B = 1;
    private static final long KB = B * 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;

    public static String parseSpeed(double bytes, boolean inBits) {
        double value = inBits ? bytes * 8 : bytes;
        if (value < KB) {
            return String.format(Locale.getDefault(), "%.0f", value);
        } else if (value < MB) {
            return String.format(Locale.getDefault(), "%.0f", value / KB);
        } else if (value < GB) {
            return String.format(Locale.getDefault(), "%.1f", value / MB);
        } else {
            return String.format(Locale.getDefault(), "%.2f", value / GB);
        }
    }

    public static String sizeByte(double bytes, boolean inBits){
        double value = inBits ? bytes * 8 : bytes;
        if (value < KB) {
            return String.format(Locale.getDefault(), "" + (inBits ? "b" : "b") + "ps", value);
        } else if (value < MB) {
            return String.format(Locale.getDefault(), "k" + (inBits ? "b" : "b") + "ps", value / KB);
        } else if (value < GB) {
            return String.format(Locale.getDefault(), "M" + (inBits ? "b" : "b") + "ps", value / MB);
        } else {
            return String.format(Locale.getDefault(), "G" + (inBits ? "b" : "b") + "ps", value / GB);
        }
    }
}
