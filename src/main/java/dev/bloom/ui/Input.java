package dev.bloom.ui;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.ArrayDeque;

/**
 * Reads the mouse and keyboard straight from GLFW, so the menu and HUD do not depend on
 * Minecraft's input event classes (which changed in 1.21.9). {@link #poll()} only refreshes
 * the live state and can be called from anywhere; each screen keeps its own {@link Edges}
 * to detect "just pressed", so one caller can never swallow another caller's click.
 */
public final class Input {
    private static final int MAXK = 349;
    /** Only keys GLFW accepts (asking about other codes makes GLFW log an error every frame). */
    private static final int[] KEYS = buildKeys();
    private static final boolean[] KEY_NOW = new boolean[MAXK];
    private static final boolean[] MOUSE_NOW = new boolean[3];
    private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<>();
    private static boolean prevLeft, prevRight;
    private static long lastPoll;
    private static long windowHandle;

    private Input() { }

    private static int[] buildKeys() {
        int[] k = new int[10 + 26 + 7];
        int n = 0;
        for (int c = 48; c <= 57; c++) k[n++] = c;
        for (int c = 65; c <= 90; c++) k[n++] = c;
        k[n++] = GLFW.GLFW_KEY_SPACE;
        k[n++] = GLFW.GLFW_KEY_ENTER;
        k[n++] = GLFW.GLFW_KEY_BACKSPACE;
        k[n++] = GLFW.GLFW_KEY_ESCAPE;
        k[n++] = GLFW.GLFW_KEY_LEFT_SHIFT;
        k[n++] = GLFW.GLFW_KEY_RIGHT_SHIFT;
        k[n++] = GLFW.GLFW_KEY_DELETE;
        return k;
    }

    private static long window() {
        long w = GLFW.glfwGetCurrentContext();
        if (w != 0L) return w;
        if (windowHandle != 0L) return windowHandle;
        try {
            Object win = Minecraft.getInstance().getWindow();
            for (String n : new String[] { "handle", "getWindow", "method_4490" }) {
                try {
                    Method m = win.getClass().getMethod(n);
                    if (m.getReturnType() == long.class) {
                        windowHandle = (long) m.invoke(win);
                        if (windowHandle != 0L) return windowHandle;
                    }
                } catch (ReflectiveOperationException ignored) { }
            }
        } catch (RuntimeException ignored) { }
        return 0L;
    }

    /** Refreshes the live mouse/key state. Cheap and safe to call many times per frame. */
    public static void poll() {
        long now = System.nanoTime();
        if (now - lastPoll < 500_000L) return;
        lastPoll = now;
        long w = window();
        if (w == 0L) return;
        long ms = now / 1_000_000L;
        for (int i = 0; i < 3; i++) MOUSE_NOW[i] = GLFW.glfwGetMouseButton(w, i) == GLFW.GLFW_PRESS;
        if (MOUSE_NOW[0] && !prevLeft) LEFT_CLICKS.addLast(ms);
        if (MOUSE_NOW[1] && !prevRight) RIGHT_CLICKS.addLast(ms);
        prevLeft = MOUSE_NOW[0];
        prevRight = MOUSE_NOW[1];
        while (!LEFT_CLICKS.isEmpty() && ms - LEFT_CLICKS.peekFirst() > 1000L) LEFT_CLICKS.removeFirst();
        while (!RIGHT_CLICKS.isEmpty() && ms - RIGHT_CLICKS.peekFirst() > 1000L) RIGHT_CLICKS.removeFirst();
        for (int k : KEYS) KEY_NOW[k] = GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS;
    }

    public static boolean down(int button) { return MOUSE_NOW[button]; }
    public static boolean keyDown(int key) { return key >= 0 && key < MAXK && KEY_NOW[key]; }

    /** Clicks in the last second for button 0 (left) or 1 (right). */
    public static int cps(int button) {
        long ms = System.nanoTime() / 1_000_000L;
        int n = 0;
        for (long t : (button == 0 ? LEFT_CLICKS : RIGHT_CLICKS)) if (ms - t <= 1000L) n++;
        return n;
    }

    /** Per-screen "just pressed" detection. Call {@link #update()} once at the start of each frame. */
    public static final class Edges {
        private final boolean[] prevM = new boolean[3], curM = new boolean[3];
        private final boolean[] prevK = new boolean[MAXK], curK = new boolean[MAXK];
        private boolean first = true;

        public void update() {
            poll();
            for (int i = 0; i < 3; i++) {
                prevM[i] = first ? MOUSE_NOW[i] : curM[i];
                curM[i] = MOUSE_NOW[i];
            }
            for (int k : KEYS) {
                prevK[k] = first ? KEY_NOW[k] : curK[k];
                curK[k] = KEY_NOW[k];
            }
            first = false;
        }

        public boolean down(int b) { return curM[b]; }
        public boolean pressed(int b) { return curM[b] && !prevM[b]; }
        public boolean keyPressed(int k) { return k >= 0 && k < MAXK && curK[k] && !prevK[k]; }
    }
}
