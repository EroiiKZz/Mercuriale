package fr.imero.mercuriale.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import fr.imero.mercuriale.theme.AeternumTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Map;

public final class Shape {
	private static final String MOD = "mercuriale";
	private static final int SAMPLES = 6;
	private static final int MAX_DEVICE_RADIUS = 192;
	private static final int SOFT_SPREAD = 6;
	private static final int STRONG_SPREAD = 10;
	private static final float SOFT_STRENGTH = 0.30f;
	private static final float STRONG_STRENGTH = 0.55f;
	private static final double FALLOFF = 4.0;
	private static final int SHADOW_COLOR = 0xFF000000;
	private static final int MARKER_WIDTH = 2;
	private static final int MARKER_INSET = 3;

	private static final int DISC = 0;
	private static final int RING = 1;
	private static final int SHADOW = 2;

	private static final Map<Long, Identifier> TEXTURES = new HashMap<>();

	private interface Coverage {
		float at(double x, double y);
	}

	private Shape() {
	}

	public static int radius(int width, int height) {
		return Math.max(0, Math.min(Theme.RADIUS, Math.min(width, height) / 2));
	}

	public static int inner(int inset) {
		return Math.max(0, Theme.RADIUS - inset);
	}

	public static boolean rounded() {
		return Theme.RADIUS > 0;
	}

	public static void fill(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, int color) {
		fill(context, x0, y0, x1, y1, Theme.RADIUS, color);
	}

	public static void fill(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, int radius, int color) {
		if (x1 <= x0 || y1 <= y0) {
			return;
		}
		int scale = scale();
		int r = deviceRadius(radius * scale, (x1 - x0) * scale, (y1 - y0) * scale);
		if (r <= 0) {
			context.fill(x0, y0, x1, y1, color);
			return;
		}
		push(context, scale);
		fillDevice(context, x0 * scale, y0 * scale, x1 * scale, y1 * scale, r, color);
		context.pose().popMatrix();
	}

	public static void pill(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, int color) {
		if (x1 <= x0 || y1 <= y0) {
			return;
		}
		if (!rounded()) {
			context.fill(x0, y0, x1, y1, color);
			return;
		}
		int scale = scale();
		int r = deviceRadius(Math.min(x1 - x0, y1 - y0) * scale / 2, (x1 - x0) * scale, (y1 - y0) * scale);
		if (r <= 0) {
			context.fill(x0, y0, x1, y1, color);
			return;
		}
		push(context, scale);
		fillDevice(context, x0 * scale, y0 * scale, x1 * scale, y1 * scale, r, color);
		context.pose().popMatrix();
	}

	public static void select(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, int barX) {
		int inset = rounded() ? MARKER_INSET : 0;
		switch (Theme.SELECTION) {
			case FILL -> fill(context, x0, y0, x1, y1, Theme.SELECTED);
			case UNDERLINE -> {
				int lift = inset > 0 ? 1 : 0;
				pill(context, x0 + inset + MARKER_WIDTH, y1 - MARKER_WIDTH - lift, x1 - inset - MARKER_WIDTH, y1 - lift,
					Theme.ACCENT);
			}
			case MARKER -> pill(context, barX, y0 + inset, barX + MARKER_WIDTH, y1 - inset, Theme.ACCENT);
		}
	}

	public static void box(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, int fill, int border) {
		fill(context, x0, y0, x1, y1, fill);
		outline(context, x0, y0, x1, y1, border);
	}

	public static void outline(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, int color) {
		stroke(context, x0, y0, x1, y1, Theme.RADIUS, Theme.STROKE, color);
	}

	public static void ring(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, int color) {
		stroke(context, x0, y0, x1, y1, Theme.RADIUS, Math.max(1, Theme.STROKE), color);
	}

	public static void stroke(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1, int radius, int width,
			int color) {
		if (width <= 0 || x1 <= x0 || y1 <= y0) {
			return;
		}
		int scale = scale();
		int deviceWidth = (x1 - x0) * scale;
		int deviceHeight = (y1 - y0) * scale;
		int r = deviceRadius(radius * scale, deviceWidth, deviceHeight);
		if (r <= 0) {
			int t = Math.min(width, Math.min(x1 - x0, y1 - y0) / 2 + 1);
			context.fill(x0, y0, x1, y0 + t, color);
			context.fill(x0, y1 - t, x1, y1, color);
			context.fill(x0, y0 + t, x0 + t, y1 - t, color);
			context.fill(x1 - t, y0 + t, x1, y1 - t, color);
			return;
		}
		int s = Math.min(width * scale, Math.min(deviceWidth, deviceHeight) / 2);
		r = deviceRadius(Math.max(r, s), deviceWidth, deviceHeight);
		int left = x0 * scale;
		int top = y0 * scale;
		int right = x1 * scale;
		int bottom = y1 * scale;
		push(context, scale);
		context.fill(left + r, top, right - r, top + s, color);
		context.fill(left + r, bottom - s, right - r, bottom, color);
		context.fill(left, top + r, left + s, bottom - r, color);
		context.fill(right - s, top + r, right, bottom - r, color);
		corners(context, ring(r, s), left, top, right, bottom, r, color);
		context.pose().popMatrix();
	}

	public static void shadow(GuiGraphicsExtractor context, int x0, int y0, int x1, int y1) {
		AeternumTheme.Shadow kind = Theme.SHADOW;
		if (kind == null || kind == AeternumTheme.Shadow.NONE || x1 <= x0 || y1 <= y0) {
			return;
		}
		boolean strong = kind == AeternumTheme.Shadow.STRONG;
		int scale = scale();
		int spread = (strong ? STRONG_SPREAD : SOFT_SPREAD) * scale;
		int left = x0 * scale;
		int top = y0 * scale;
		int right = x1 * scale;
		int bottom = y1 * scale;
		int r = deviceRadius(Theme.RADIUS * scale, right - left, bottom - top);
		int c = r + spread;
		int size = c * 2;
		Identifier texture = shadow(r, spread, strong);
		push(context, scale);
		context.blit(RenderPipelines.GUI_TEXTURED, texture, left - spread, top - spread, 0, 0, c, c, c, c, size, size,
			SHADOW_COLOR);
		context.blit(RenderPipelines.GUI_TEXTURED, texture, right - r, top - spread, c, 0, c, c, c, c, size, size,
			SHADOW_COLOR);
		context.blit(RenderPipelines.GUI_TEXTURED, texture, left - spread, bottom - r, 0, c, c, c, c, c, size, size,
			SHADOW_COLOR);
		context.blit(RenderPipelines.GUI_TEXTURED, texture, right - r, bottom - r, c, c, c, c, c, c, size, size,
			SHADOW_COLOR);
		int across = right - left - r * 2;
		int down = bottom - top - r * 2;
		if (across > 0) {
			context.blit(RenderPipelines.GUI_TEXTURED, texture, left + r, top - spread, c, 0, across, spread, 1, spread,
				size, size, SHADOW_COLOR);
			context.blit(RenderPipelines.GUI_TEXTURED, texture, left + r, bottom, c, size - spread, across, spread, 1,
				spread, size, size, SHADOW_COLOR);
		}
		if (down > 0) {
			context.blit(RenderPipelines.GUI_TEXTURED, texture, left - spread, top + r, 0, c, spread, down, spread, 1,
				size, size, SHADOW_COLOR);
			context.blit(RenderPipelines.GUI_TEXTURED, texture, right, top + r, size - spread, c, spread, down, spread, 1,
				size, size, SHADOW_COLOR);
		}
		context.pose().popMatrix();
	}

	private static void fillDevice(GuiGraphicsExtractor context, int left, int top, int right, int bottom, int r,
			int color) {
		context.fill(left + r, top, right - r, bottom, color);
		context.fill(left, top + r, left + r, bottom - r, color);
		context.fill(right - r, top + r, right, bottom - r, color);
		corners(context, disc(r), left, top, right, bottom, r, color);
	}

	private static void corners(GuiGraphicsExtractor context, Identifier texture, int left, int top, int right,
			int bottom, int r, int color) {
		int size = r * 2;
		context.blit(RenderPipelines.GUI_TEXTURED, texture, left, top, 0, 0, r, r, r, r, size, size, color);
		context.blit(RenderPipelines.GUI_TEXTURED, texture, right - r, top, r, 0, r, r, r, r, size, size, color);
		context.blit(RenderPipelines.GUI_TEXTURED, texture, left, bottom - r, 0, r, r, r, r, r, size, size, color);
		context.blit(RenderPipelines.GUI_TEXTURED, texture, right - r, bottom - r, r, r, r, r, r, r, size, size, color);
	}

	private static int deviceRadius(int wanted, int width, int height) {
		return Math.max(0, Math.min(Math.min(wanted, MAX_DEVICE_RADIUS), Math.min(width, height) / 2));
	}

	private static int scale() {
		Minecraft client = Minecraft.getInstance();
		return client == null ? 1 : Math.max(1, client.getWindow().getGuiScale());
	}

	private static void push(GuiGraphicsExtractor context, int scale) {
		Matrix3x2fStack pose = context.pose();
		pose.pushMatrix();
		pose.scale(1.0f / scale, 1.0f / scale);
	}

	private static Identifier disc(int r) {
		return texture(key(DISC, r, 0), "disc_" + r, r * 2, (x, y) -> inside(x, y, r, r) ? 1f : 0f);
	}

	private static Identifier ring(int r, int s) {
		int inner = r - s;
		return texture(key(RING, r, s), "ring_" + r + "_" + s, r * 2,
			(x, y) -> inside(x, y, r, r) && (inner <= 0 || !inside(x, y, r, inner)) ? 1f : 0f);
	}

	private static Identifier shadow(int r, int spread, boolean strong) {
		int c = r + spread;
		float strength = strong ? STRONG_STRENGTH : SOFT_STRENGTH;
		double tail = Math.exp(-FALLOFF);
		return texture(key(SHADOW, r, spread * 2 + (strong ? 1 : 0)), "shadow_" + r + "_" + spread + (strong ? "s" : ""),
			c * 2, (x, y) -> {
				double distance = Math.hypot(x - c, y - c) - r;
				if (distance <= 0) {
					return 0f;
				}
				double t = distance / spread;
				if (t >= 1) {
					return 0f;
				}
				return (float) (strength * (Math.exp(-FALLOFF * t * t) - tail) / (1 - tail));
			});
	}

	private static boolean inside(double x, double y, int center, int radius) {
		double dx = x - center;
		double dy = y - center;
		return dx * dx + dy * dy <= (double) radius * radius;
	}

	private static long key(int kind, int a, int b) {
		return ((long) kind << 48) | ((long) a << 24) | b;
	}

	private static Identifier texture(long key, String name, int size, Coverage coverage) {
		Identifier cached = TEXTURES.get(key);
		if (cached != null) {
			return cached;
		}
		NativeImage image = new NativeImage(size, size, true);
		float step = 1.0f / SAMPLES;
		for (int j = 0; j < size; j++) {
			for (int i = 0; i < size; i++) {
				float total = 0f;
				for (int b = 0; b < SAMPLES; b++) {
					for (int a = 0; a < SAMPLES; a++) {
						total += coverage.at(i + (a + 0.5f) * step, j + (b + 0.5f) * step);
					}
				}
				int alpha = Math.clamp(Math.round(total / (SAMPLES * SAMPLES) * 255f), 0, 255);
				image.setPixelABGR(i, j, (alpha << 24) | 0xFFFFFF);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath("aeternum", "shape/" + MOD + "/" + name);
		Minecraft.getInstance().getTextureManager().register(id, new SmoothTexture(MOD + " " + name, image));
		TEXTURES.put(key, id);
		return id;
	}

	private static final class SmoothTexture extends DynamicTexture {
		SmoothTexture(String label, NativeImage image) {
			super(() -> label, image);
			sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		}
	}
}
