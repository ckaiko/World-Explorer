package byow.Core;

public class Position {
    public int x;
    public int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position moveUp() {
        return new Position(this.x, this.y + 1);
    }

    public Position moveDown() {
        return new Position(this.x, this.y - 1);
    }

    public Position moveLeft() {
        return new Position(this.x - 1, this.y);
    }

    public Position moveRight() {
        return new Position(this.x + 1, this.y);
    }
}
