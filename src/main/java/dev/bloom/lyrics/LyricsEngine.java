package dev.bloom.lyrics;

import dev.bloom.Config;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Keeps the current song, its timed lyrics and the playback clock. Songs are .lrc files in
 * config/bloom/songs/. If a .wav with the same name is next to it, it is played too.
 */
public final class LyricsEngine {
    public static final class Frame {
        public String prev = "", cur = "", next = "";
        public long sinceMs;
        public boolean playing;
    }

    public static String song;
    public static String status = "";
    public static long offsetMs;
    private static List<Lrc.Line> lines = new ArrayList<>();
    private static boolean playing;
    private static long baseMs, startNano;
    private static Clip clip;

    private LyricsEngine() { }

    public static Path dir() { return Config.dir().resolve("songs"); }

    public static List<String> list() {
        List<String> names = new ArrayList<>();
        try {
            Files.createDirectories(dir());
            try (Stream<Path> s = Files.list(dir())) {
                s.forEach(p -> {
                    String n = p.getFileName().toString();
                    if (n.toLowerCase().endsWith(".lrc")) names.add(n.substring(0, n.length() - 4));
                });
            }
        } catch (IOException ignored) { }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static void select(String name) {
        stopAudio();
        playing = false;
        baseMs = 0;
        lines = new ArrayList<>();
        song = null;
        status = "";
        try {
            List<String> raw = Files.readAllLines(dir().resolve(name + ".lrc"), StandardCharsets.UTF_8);
            lines = Lrc.parse(raw);
            song = name;
            Config.put("lyrics.song", name);
            status = lines.isEmpty() ? "No timed lines found in this .lrc file." : lines.size() + " lines loaded.";
        } catch (IOException e) {
            status = "Could not read " + name + ".lrc";
            return;
        }
        openAudio(name);
    }

    private static void openAudio(String name) {
        Path wav = dir().resolve(name + ".wav");
        if (Files.exists(wav)) {
            try (AudioInputStream in = AudioSystem.getAudioInputStream(wav.toFile())) {
                clip = AudioSystem.getClip();
                clip.open(in);
                status += " Playing " + name + ".wav with the lyrics.";
            } catch (Exception e) {
                clip = null;
                status += " Could not open the .wav (" + e.getClass().getSimpleName() + ").";
            }
        } else if (Files.exists(dir().resolve(name + ".mp3")) || Files.exists(dir().resolve(name + ".ogg"))) {
            status += " Only .wav audio plays for now; lyrics follow the timer.";
        } else {
            status += " No .wav found: lyrics follow the timer.";
        }
    }

    private static void stopAudio() {
        if (clip != null) {
            try { clip.stop(); clip.close(); } catch (RuntimeException ignored) { }
            clip = null;
        }
    }

    public static boolean isPlaying() { return playing; }

    public static void play() {
        if (song == null || playing) return;
        playing = true;
        startNano = System.nanoTime();
        if (clip != null) {
            clip.setMicrosecondPosition(Math.max(0, baseMs) * 1000L);
            clip.start();
        }
    }

    public static void pause() {
        if (!playing) return;
        baseMs = position();
        playing = false;
        if (clip != null) clip.stop();
    }

    public static void restart() {
        boolean was = playing;
        if (clip != null) clip.stop();
        playing = false;
        baseMs = 0;
        if (was) play();
    }

    public static void nudge(long deltaMs) { offsetMs += deltaMs; }

    public static long position() {
        if (!playing) return baseMs;
        if (clip != null && clip.isRunning()) return clip.getMicrosecondPosition() / 1000L;
        return baseMs + (System.nanoTime() - startNano) / 1_000_000L;
    }

    /** What to show right now, or null when no song is loaded. */
    public static Frame frame() {
        if (song == null) return null;
        long pos = position() + offsetMs;
        Frame f = new Frame();
        f.playing = playing;
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).ms <= pos) idx = i; else break;
        }
        if (idx >= 0) {
            f.cur = lines.get(idx).text;
            f.sinceMs = pos - lines.get(idx).ms;
            if (idx > 0) f.prev = lines.get(idx - 1).text;
        }
        if (idx + 1 < lines.size()) f.next = lines.get(idx + 1).text;
        return f;
    }
}
