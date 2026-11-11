package com.paymentalerts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Amounts {
    private Amounts() {}

    public record Payment(String player, double amount) {}

    private static String cachedPattern = null;
    private static Pattern compiled = null;

    public static Payment parse(String message, String regex) {
        if (message == null) return null;
        try {
            if (compiled == null || !regex.equals(cachedPattern)) {
                compiled = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                cachedPattern = regex;
            }
            Matcher m = compiled.matcher(message);
            if (!m.find()) return null;
            String player = m.group(1);
            double value = Double.parseDouble(m.group(2).replace(",", ""));
            String suffix = m.groupCount() >= 3 ? m.group(3) : null;
            return new Payment(player, value * multiplier(suffix));
        } catch (Exception e) {
            return null;
        }
    }

    /** Parses things like "10M", "1.5B", "250m", "$100B", "1,000,000". */
    public static double parseAmount(String s) {
        s = s.trim().replace("$", "").replace(",", "");
        if (s.isEmpty()) throw new NumberFormatException("empty");
        char last = s.charAt(s.length() - 1);
        if (Character.isLetter(last)) {
            return Double.parseDouble(s.substring(0, s.length() - 1)) * multiplier(String.valueOf(last));
        }
        return Double.parseDouble(s);
    }

    private static double multiplier(String suffix) {
        if (suffix == null || suffix.isEmpty()) return 1d;
        switch (Character.toLowerCase(suffix.charAt(0))) {
            case 'k': return 1_000d;
            case 'm': return 1_000_000d;
            case 'b': return 1_000_000_000d;
            case 't': return 1_000_000_000_000d;
            default: return 1d;
        }
    }

    public static String format(double amount) {
        if (amount >= 1e12) return "$" + trim(amount / 1e12) + "T";
        if (amount >= 1e9) return "$" + trim(amount / 1e9) + "B";
        if (amount >= 1e6) return "$" + trim(amount / 1e6) + "M";
        if (amount >= 1e3) return "$" + trim(amount / 1e3) + "K";
        return "$" + trim(amount);
    }

    private static String trim(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.005) return Long.toString(Math.round(v));
        String s = String.format(java.util.Locale.ROOT, "%.2f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}
