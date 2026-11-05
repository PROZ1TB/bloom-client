package dev.bloom.ui;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/**
 * Polls mouse buttons and keys straight from GLFW once per frame, so the menu and the
 * HUD do not depend on Minecraft's input event classes (which changed in 1.21.9).
 */
public final class Input {
    private static final int MAXK = 349;
    private static final boolean[] KEY_NOW = new boolean[MAXK], KEY_PREV = new boolean[MAXK];
    private static final boolean[] MOUSE_NOW = new boolean[3], MOUSE_PREV = new boolean[3];
    private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<>();
    private static long lastPoll;

    private Input() { }

    /** Safe to call from several places per frame: only the first call in a millisecond does the work. */
    public static void poll() {
        long now = System.nanoTime();
        if (now - lastPoll < 1_000_000L) return;
        lastPoll = now;
        long w = GLFW.glfwGetCurrentContext();
        if (w == 0L) return;
        long ms = now / 1_000_000L;
        for (int i = 0; i < 3; i++) {
            MOUSE_PREV[i] = MOUSE_NOW[i];
            MOUSE_NOW[i] = GLFW.glfwGetMouseButton(w, i) == GLFW.GLFW_PRESS;
        }
        if (MOUSE_NOW[0] && !MOUSE_PREV[0]) LEFT_CLICKS.addLast(ms);
        if (MOUSE_NOW[1] && !MOUSE_PREV[1]) RIGHT_CLICKS.addLast(ms);
        while (!LEFT_CLICKS.isEmpty() && ms - LEFT_CLICKS.peekFirst() > 1000L) LEFT_CLICKS.removeFirst();
        while (!RIGHT_CLICKS.isEmpty() && ms - RIGHT_CLICKS.peekFirst() > 1000L) RIGHT_CLICKS.removeFirst();
        for (int k = 32; k < MAXK; k++) {
            KEY_PREV[k] = KEY_NOW[k];
            KEY_NOW[k] = GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS;
        }
    }

    public static boolean down(int button) { return MOUSE_NOW[button]; }
    public static boolean pressed(int button) { return MOUSE_NOW[button] && !MOUSE_PREV[button]; }
    public static boolean keyDown(int key) { return key >= 0 && key < MAXK && KEY_NOW[key]; }
    public static boolean keyPressed(int key) { return key >= 0 && key < MAXK && KEY_NOW[key] && !KEY_PREV[key]; }

    /** Clicks in the last second for button 0 (left) or 1 (right). */
    public static int cps(int button) {
        long ms = System.nanoTime() / 1_000_000L;
        int n = 0;
        for (long t : (button == 0 ? LEFT_CLICKS : RIGHT_CLICKS)) if (ms - t <= 1000L) n++;
        return n;
    }
}
