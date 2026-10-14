package dev.sugamdeol.pollinations;

import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.annotation.Nonnull;

/**
 * Renders a pre-computed 128x128 color matrix onto a map.
 */
public class ImageMapRenderer extends MapRenderer {

    private final byte[][] colors;
    private final String prompt;
    private boolean rendered;

    public ImageMapRenderer(byte[][] colors, String prompt) {
        super(true); // contextual
        this.colors = colors;
        this.prompt = prompt;
    }

    @Override
    public void render(@Nonnull MapView view, @Nonnull MapCanvas canvas, @Nonnull org.bukkit.entity.Player player) {
        if (rendered) return;
        rendered = true;
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                canvas.setPixel(x, y, colors[x][y]);
            }
        }
    }
}