package byow.Core;

public class Room {
    public int xStart;
    public int yStart;
    public int xCenter;
    public int yCenter;

    public int width;
    public int height;
    public static final int maxSize = 9;
    public static final int minSize = 3;

    public Room(int x, int y, int width, int height) {
        xStart = x;
        yStart = y;
        xCenter = xStart + width / 2;
        yCenter = yStart + height / 2;
        this.width = width;
        this.height = height;
    }

    public boolean isOverlap(Room r) {
        return (this.xStart + this.width >= r.xStart && r.xStart + r.width >= this.xStart &&
                this.yStart + this.height >= r.yStart && r.yStart + r.height >= this.yStart);
    }
}