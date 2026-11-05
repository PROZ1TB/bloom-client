package dev.bloom.lyrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser for .lrc timed-lyrics files. */
public final class Lrc {
    public static final class Line {
        public final long ms;
        public final String text;

        public Line(long ms, String text) {
            this.ms = ms;
            this.text = text;
        }
    }

    private static final Pattern STAMP = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]");
    private static final Pattern WORD_TAG = Pattern.compile("<\\d{1,3}:\\d{1,2}(?:[.:]\\d{1,3})?>");
    private static final Pattern OFFSET = Pattern.compile("^\\[offset:\\s*([+-]?\\d+)\\s*\\]", Pattern.CASE_INSENSITIVE);

    private Lrc() { }

    public static List<Line> parse(List<String> rawLines) {
        List<Line> out = new ArrayList<>();
        long offset = 0;
        for (String raw : rawLines) {
            String line = raw.replace("\uFEFF", "").trim();
            if (line.isEmpty()) continue;
            Matcher om = OFFSET.matcher(line);
            if (om.find()) {
                try { offset = Long.parseLong(om.group(1)); } catch (NumberFormatException ignored) { }
                continue;
            }
            Matcher m = STAMP.matcher(line);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (m.find() && m.start() == end) {
                long mins = Long.parseLong(m.group(1)), secs = Long.parseLong(m.group(2));
                long frac = 0;
                if (m.group(3) != null) {
                    String f = (m.group(3) + "00").substring(0, 3);
                    frac = Long.parseLong(f);
                }
                times.add(mins * 60_000L + secs * 1000L + frac);
                end = m.end();
            }
            if (times.isEmpty()) continue;
            String text = WORD_TAG.matcher(line.substring(end)).replaceAll("").trim();
            for (long t : times) out.add(new Line(t - offset, text));
        }
        out.sort(Comparator.comparingLong(l -> l.ms));
        return out;
    }
}
