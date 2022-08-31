package samj.mario.editor.data;

public class Icon {
    private final IconSheet iconSheet;
    private final int xLocation;
    private final int yLocation;

    public Icon(IconSheet iconSheet, int xLocation, int yLocation) {
        this.iconSheet = iconSheet;
        this.xLocation = xLocation;
        this.yLocation = yLocation;
    }

    public IconSheet getSpriteSheet() {
        return iconSheet;
    }

    public int getxLocation() {
        return xLocation;
    }

    public int getyLocation() {
        return yLocation;
    }
}
