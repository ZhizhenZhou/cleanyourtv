package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;

/**
 * The typeface behind one of the font_* strings in res/values/fonts.xml, for views built in code. The string
 * points at a bundled font, or, in locales Outfit can't draw (values-vi), names a system family (#833).
 */
final class UiFont {
    private UiFont() {}

    static Typeface get(Context context, int fontString) {
        Resources res = context.getResources();
        TypedValue value = new TypedValue();
        res.getValue(fontString, value, true);
        String name = String.valueOf(value.string);
        if (name.startsWith("res/")) {
            // Resources.getFont is API 26: on Android 7 (the fork's minSdk 25) the bundled font cannot be
            // loaded, and the default typeface is used instead of crashing the home screen.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return res.getFont(value.resourceId);
            return null;
        }
        return Typeface.create(name, Typeface.NORMAL);
    }
}
