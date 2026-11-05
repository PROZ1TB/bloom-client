package dev.bloom.risky.skin;

import dev.bloom.Group;
import dev.bloom.visual.Avatar;

public final class SkinGroup implements Group {
    @Override
    public void init() {
        Avatar.painter = new SkinPainter();
    }
}
