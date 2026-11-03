package com.gathin.tvhome;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * The tile art for an app that ships no TV banner (a phone app added from the library): a card in the icon's own
 * colour, with the icon on it.
 *
 * <p>Two ways in, decided from the icon itself:
 * <ul>
 *   <li><b>A plated icon</b> (it fills a rounded square): its own top and bottom edge colours become the card, and
 *       the icon is drawn on top, so its plate fades into the card instead of sitting on it. A white plate gives a
 *       white tile.</li>
 *   <li><b>A bare logo</b>: the card takes a gradient of the logo's own colour, taken from its saturated pixels so a
 *       white or grey background does not turn every card the same grey.</li>
 * </ul>
 *
 * <p>Nothing here is a downloaded or bundled asset; both the colour and the drawing come from the app itself.
 */
final class TileCard {
    private TileCard() {
    }

    private static final int W = 320, H = 180;
    /** Share of the card's height the mark takes, in per cent. */
    private static final int ICON_RATIO = 58;

    static Drawable build(Resources res, Drawable icon) {
        Bitmap card = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(card);
        // The mark inside the card, as a share of its height: a bigger mark reads better on a TV across the room,
        // a smaller one leaves more of the card's colour visible.
        int mark = Math.round(H * ICON_RATIO / 100f);
        Bitmap art = raster(icon, mark * 2);
        Stats stats = stats(art);
        // An icon that fills a rounded square carries its own tile: the colours along its top and bottom edges are
        // spread across the card and the icon is drawn on top, so the plate fades into the card instead of sitting
        // on it. That is why a white-plated app (影视) gets a white tile rather than one tinted by its logo.
        int[] plate = plateEdges(art);
        // The icon's own plate is usually white or near it, which would make every card grey. The mark's colour is
        // what names the app, so the card is built from the saturated pixels when the icon has any.
        int tint = stats.hasTint ? stats.tint : stats.average;
        int top, bottom;
        if (plate != null) {
            top = plate[0];
            bottom = plate[1];
        } else if (stats.hasTint) {
            top = lighten(tint, 0.20f);
            bottom = darken(tint, 0.48f);
        } else if (stats.coverage < 0.72f) {
            // A bare logo with no colour of its own: a grey card that follows its brightness still separates it.
            top = lighten(stats.average, 0.18f);
            bottom = darken(stats.average, 0.45f);
        } else {
            // A grey plated icon: no colour to tint with, so keep the plate readable on a neutral card.
            top = 0xFF3C424B;
            bottom = 0xFF191C21;
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0, 0, 0, H, top, bottom, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, W, H, paint);
        paint.setShader(null);
        // A plated icon is drawn larger, so its logo reads as big as a bare one, and its plate merges into the card.
        int side = plate != null ? Math.round(mark * 1.25f) : mark;
        int left = (W - side) / 2, topY = (H - side) / 2;
        icon.setBounds(left, topY, left + side, topY + side);
        icon.draw(canvas);
        return new BitmapDrawable(res, card);
    }

    /**
     * The two edge colours of an icon that is a filled rounded square, or null for any other shape. Mirrors what
     * Aerial does: the icon's own plate becomes the tile, so a white plate gives a white card. Null when the
     * opaque area is not square enough, not filled enough, when an edge is not mostly one colour, or when the two
     * edges are so far apart that the card would read as two stripes.
     */
    private static int[] plateEdges(Bitmap icon) {
        int n = icon.getWidth();
        int[] px = new int[n * n];
        icon.getPixels(px, 0, n, 0, 0, n, n);
        int minX = n, minY = n, maxX = -1, maxY = -1, opaque = 0;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (((px[y * n + x] >>> 24) & 0xFF) < 64) continue;
                opaque++;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }
        int w = maxX - minX + 1, h = maxY - minY + 1;
        if (opaque == 0 || w < n / 2 || Math.abs(w - h) > w * 0.06f) return null;
        if (opaque < w * h * 0.88f) return null;
        int top = band(px, n, minX, maxX, minY + h * 2 / 100, minY + h * 5 / 100);
        int bottom = band(px, n, minX, maxX, maxY - h * 5 / 100, maxY - h * 2 / 100);
        if (top == 0 || bottom == 0) return null;
        // Two unrelated colours (black over orange) would read as a stripe, not as one plate.
        int dr = ((top >> 16) & 0xFF) - ((bottom >> 16) & 0xFF);
        int dg = ((top >> 8) & 0xFF) - ((bottom >> 8) & 0xFF);
        int db = (top & 0xFF) - (bottom & 0xFF);
        if (Math.sqrt(dr * dr + dg * dg + db * db) > 260) return null;
        return new int[]{top, bottom};
    }

    /** The colour most of a horizontal band shares, or 0 when the band is not mostly one colour. */
    private static int band(int[] px, int n, int minX, int maxX, int from, int to) {
        int width = maxX - minX + 1;
        int left = minX + width / 5, right = maxX - width / 5;
        if (right <= left) return 0;
        int[] reds = new int[(to - from + 1) * (right - left + 1)];
        int[] greens = new int[reds.length];
        int[] blues = new int[reds.length];
        int count = 0;
        for (int y = from; y <= to; y++) {
            for (int x = left; x <= right; x++) {
                int c = px[y * n + x];
                reds[count] = (c >> 16) & 0xFF;
                greens[count] = (c >> 8) & 0xFF;
                blues[count] = c & 0xFF;
                count++;
            }
        }
        if (count == 0) return 0;
        java.util.Arrays.sort(reds, 0, count);
        java.util.Arrays.sort(greens, 0, count);
        java.util.Arrays.sort(blues, 0, count);
        int r = reds[count / 2], g = greens[count / 2], b = blues[count / 2];
        int same = 0;
        for (int i = 0; i < count; i++) {
            int dr = reds[i] - r, dg = greens[i] - g, db = blues[i] - b;
            if (dr * dr + dg * dg + db * db <= 48 * 48) same++;
        }
        return same >= count * 0.6f ? (0xFF000000 | (r << 16) | (g << 8) | b) : 0;
    }

    /** The icon onto its own transparent bitmap, so its pixels can be measured. */
    private static Bitmap raster(Drawable icon, int side) {
        Bitmap bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        icon.setBounds(0, 0, side, side);
        icon.draw(canvas);
        return bitmap;
    }

    private static final class Stats {
        /** Mean colour of the opaque pixels. */
        int average = 0xFF808080;
        /** Share of the square the icon actually paints: low means a bare logo with margins. */
        float coverage;
        /** Mean of the pixels that carry a real colour, i.e. the mark rather than a white or grey plate. */
        int tint;
        /** False when no pixel is saturated enough to tint a card with. */
        boolean hasTint;
    }

    private static Stats stats(Bitmap bitmap) {
        Stats stats = new Stats();
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        long r = 0, g = 0, b = 0;
        long tr = 0, tg = 0, tb = 0;
        int painted = 0, tinted = 0;
        for (int pixel : pixels) {
            int alpha = (pixel >>> 24) & 0xFF;
            if (alpha < 24) continue;
            painted++;
            int pr = (pixel >> 16) & 0xFF, pg = (pixel >> 8) & 0xFF, pb = pixel & 0xFF;
            r += pr;
            g += pg;
            b += pb;
            int pmax = Math.max(pr, Math.max(pg, pb)), pmin = Math.min(pr, Math.min(pg, pb));
            // Skip near-white and near-black pixels: they are the plate, not the mark.
            if (pmax > 40 && pmax < 236 && pmax - pmin > 26) {
                tr += pr;
                tg += pg;
                tb += pb;
                tinted++;
            }
        }
        if (painted == 0) return stats;
        if (tinted > painted / 20) {
            stats.hasTint = true;
            stats.tint = 0xFF000000 | ((int) (tr / tinted) << 16) | ((int) (tg / tinted) << 8) | (int) (tb / tinted);
        }
        stats.coverage = (float) painted / pixels.length;
        stats.average = 0xFF000000 | ((int) (r / painted) << 16) | ((int) (g / painted) << 8) | (int) (b / painted);
        return stats;
    }

    private static int lighten(int colour, float amount) {
        return blend(colour, 0xFFFFFFFF, amount);
    }

    private static int darken(int colour, float amount) {
        return blend(colour, 0xFF000000, amount);
    }

    private static int blend(int colour, int towards, float amount) {
        int r = (int) (((colour >> 16) & 0xFF) + ((((towards >> 16) & 0xFF) - ((colour >> 16) & 0xFF)) * amount));
        int g = (int) (((colour >> 8) & 0xFF) + ((((towards >> 8) & 0xFF) - ((colour >> 8) & 0xFF)) * amount));
        int b = (int) ((colour & 0xFF) + (((towards & 0xFF) - (colour & 0xFF)) * amount));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
