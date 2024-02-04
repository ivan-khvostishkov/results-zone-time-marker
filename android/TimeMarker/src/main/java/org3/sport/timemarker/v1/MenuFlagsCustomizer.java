package org3.sport.timemarker.v1;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.ContextMenu;
import android.view.MenuItem;

/**
 * @author ikh
 * @since 3/12/14
 */
public class MenuFlagsCustomizer {
    private final ContextMenu menu;
    private final Resources resources;

    public MenuFlagsCustomizer(ContextMenu menu, Resources resources) {
        this.menu = menu;
        this.resources = resources;
    }

    public void customize() {
        for (Flag flag : Flag.values()) {
            customizeFlag(flag);
        }
    }

    private void customizeFlag(Flag flag) {
        Drawable drawableFlag = getResources().getDrawable(flag.getDrawableId());
        setIntrinsicBounds(drawableFlag);
        SpannableString flagTitle = new SpannableString("  " + flag.getName());
        flagTitle.setSpan(new ImageSpan(drawableFlag, ImageSpan.ALIGN_BASELINE),
                0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        MenuItem item = menu.getItem(flag.ordinal());
        item.setTitle(flagTitle);
        item.setTitleCondensed(flag.getName());
    }

    private void setIntrinsicBounds(Drawable flag) {
        assert flag != null;
        flag.setBounds(0, 0, flag.getIntrinsicWidth(), flag.getIntrinsicHeight());
    }

    private Resources getResources() {
        return resources;
    }


}
