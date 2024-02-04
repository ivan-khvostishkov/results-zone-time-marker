package org3.sport.timemarker.v1;

/**
 * @author ikh
 * @since 3/13/14
 */
public enum Flag {
    NONE(R.id.flag_none, "None", R.drawable.flag_none),
    GREEN(R.id.flag_green, "Green", R.drawable.flag_green),
    YELLOW(R.id.flag_yellow, "Yellow", R.drawable.flag_yellow),
    RED(R.id.flag_red, "Red", R.drawable.flag_red),
    BLUE(R.id.flag_blue, "Blue", R.drawable.flag_blue);

    private final int contextMenuId;
    private final String name;
    private final int drawableId;

    Flag(int contextMenuId, String name, int drawableId) {
        this.contextMenuId = contextMenuId;
        this.name = name;
        this.drawableId = drawableId;
    }

    public static Flag byContextMenu(int contextMenuId) {
        switch (contextMenuId) {
            case R.id.flag_none:
                return Flag.NONE;
            case R.id.flag_green:
                return Flag.GREEN;
            case R.id.flag_yellow:
                return Flag.YELLOW;
            case R.id.flag_red:
                return Flag.RED;
            case R.id.flag_blue:
                return Flag.BLUE;
            default:
                throw new IllegalArgumentException("Unknown context menu");
        }
    }

    public int getDrawableId() {
        return drawableId;
    }

    public String getName() {
        return name;
    }

    public String toStorageId() {
        return name.toLowerCase();
    }
}
