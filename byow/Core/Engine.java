package byow.Core;

import byow.InputDemo.StringInputDevice;
import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.algs4.StdDraw;
import java.awt.*;
import java.io.*;
import java.util.*;

import static java.lang.Character.toLowerCase;

public class Engine {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 30;
    private static final int MIDWIDTH = WIDTH / 2;
    private static final int MIDHEIGHT = HEIGHT / 2;
    private static final int MAXROOMAMOUNT = 20;
    private static final int MINROOMAMOUNT = 10;
    private static final int LINESIZE = 2;
    private static final int SEEDHELPER = 10;
    private TERenderer ter = new TERenderer();
    private Random rand = new Random();
    private StringBuilder savedActions = new StringBuilder();
    private TETile[][] world = new TETile[WIDTH][HEIGHT];
    private boolean isPlaying = false;
    private ArrayList<Room> rooms = new ArrayList<>();
    private Position avatarPos;
    private Position flowerPos;
    private Position doorPos;
    private TETile prevTile;
    private boolean isWin = false;
    private long seed;
    private TETile bounds = Tileset.WALL;
    private TETile inner = Tileset.FLOOR;
    private TETile[][] mirrorWorld = new TETile[WIDTH][HEIGHT];
    private TETile[][] currentWorld = world;
    private Position[] finalFlowers = new Position[3];
    private int collectedFlowers = 0;

    /** constructor **/
    public Engine() {
        ter.initialize(WIDTH, HEIGHT + 3 * LINESIZE, 0, 0);
    }

    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() {
        drawMenu();
        while (!isWin) {
            if (isPlaying) {
                drawHud();
            }
            if (StdDraw.hasNextKeyTyped()) {
                char action = StdDraw.nextKeyTyped();
                handle(action);
                if (isPlaying) {
                    showWorld(currentWorld);
                }
            }
        }

        showWin();
    }

    /**
     * Method used for autograding and testing your code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww".) The engine should
     * behave exactly as if the user typed these characters into the engine using
     * interactWithKeyboard.
     */
    public TETile[][] interactWithInputString(String input) {
        StringInputDevice sd = new StringInputDevice(input);
        savedActions = new StringBuilder();

        char current = sd.getNextKey();
        while (sd.possibleNextInput() && current != 'n' && current != 'l') {
            current = sd.getNextKey();
        }

        if (current == 'n') {
            savedActions.append('n');
            current = sd.getNextKey();

            StringBuilder sb = new StringBuilder();
            while (sd.possibleNextInput() && current != 's') {
                sb.append(current);
                current = sd.getNextKey();
            }

            seed = Long.parseLong(sb.toString());
            savedActions.append(seed);
            savedActions.append('s');
            buildNewWorld();

        } else if (current == 'l') {
            handle(current);
        }

        isPlaying = true;
        while (sd.possibleNextInput()) {
            current = sd.getNextKey();
            handle(current);
        }

        return currentWorld;
    }

    /** World Generator **/
    // generate new world
    public void buildNewWorld() {
        rand.setSeed(seed);
        // randomize the theme of the world
        int randWorld = rand.nextInt(5);
        if (randWorld == 4) {
            bounds = Tileset.TREE;
            inner = Tileset.GRASS;
        } else if (randWorld == 1) {
            inner = Tileset.SAND;
            bounds = Tileset.MOUNTAIN;
        }
        else if (randWorld == 2) {
            inner = Tileset.SKY;
            bounds = Tileset.CLOUD;
        }
        else if (randWorld == 3) {
            inner = Tileset.SPACE;
            bounds = Tileset.STAR;
        }

        // create blank canvas world
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                world[x][y] = Tileset.NOTHING;
            }
        }

        // create random amount of rooms
        int roomAmount = rand.nextInt(MAXROOMAMOUNT - MINROOMAMOUNT) + MINROOMAMOUNT;
        for (int i = 0; i < roomAmount; i++) {
            Room newRoom = drawRoom();

            if (i != 0) {
                int j = i - 1;
                while (j >= 0 && newRoom.xStart < rooms.get(j).xStart) {
                    j--;
                }
                rooms.add(j + 1, newRoom);
            } else {
                rooms.add(newRoom);
            }
        }

        for (int i = 1; i < roomAmount; i++) {
            drawHallway(rooms.get(i), rooms.get(i - 1));
        }

        showWorld(world);

        // put avatar in the leftmost room
        Room leftRoom = rooms.get(0);
        int randX = rand.nextInt(leftRoom.width - 2) + leftRoom.xStart + 1;
        int randY = rand.nextInt(leftRoom.height - 2) + leftRoom.yStart + 1;
        avatarPos = new Position(randX, randY);
        prevTile = showChar(null, avatarPos, Tileset.AVATAR, null);

        // put flower in the rightmost room
        Room rightRoom = rooms.get(roomAmount - 1);
        int randX2 = rand.nextInt(rightRoom.width - 2) + rightRoom.xStart + 1;
        int randY2 = rand.nextInt(rightRoom.height - 2) + rightRoom.yStart + 1;
        flowerPos = new Position(randX2, randY2);
        showChar(null, flowerPos, Tileset.FLOWER, null);

        // put locked door in the wall of  middle room
        Room midRoom = rooms.get(roomAmount / 2);
        int randX3 = rand.nextInt(midRoom.width - 2) + midRoom.xStart + 1;
        int randY3 = rand.nextInt(midRoom.height - 2) + midRoom.yStart + 1;
        doorPos = new Position(randX3, randY3);
        showChar(null, doorPos, Tileset.LOCKED_DOOR, null);

        currentWorld = world;
    }

    // create secret world
    public void buildMirrorWorld() {
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world[i].length; j++) {
                TETile current = world[i][world[i].length - 1 - j];

                if (current == inner || current == Tileset.UNLOCKED_DOOR) {
                    mirrorWorld[i][j] = Tileset.WATER;
                } else {
                    if (current == Tileset.AVATAR) {
                        avatarPos = new Position(i, j);
                        prevTile = Tileset.WATER;
                    }
                    mirrorWorld[i][j] = current;
                }
            }
        }

        // put 3 flowers in random places
        for (int i = 0; i < finalFlowers.length; i++) {
            int randX = rand.nextInt(WIDTH - 1);
            int randY = rand.nextInt(HEIGHT - 1);

            while (mirrorWorld[randX][randY] != Tileset.WATER) {
                randX = rand.nextInt(WIDTH - 1);
                randY = rand.nextInt(HEIGHT - 1);
            }

            finalFlowers[i] = new Position(randX, randY);
            showChar(null, finalFlowers[i], Tileset.FLOWER, null);
        }

        currentWorld = mirrorWorld;
        showWorld(mirrorWorld);
    }

    // draw each room
    private Room drawRoom() {
        int randWidth, randHeight, randX, randY;
        Room newRoom = null;
        boolean hasOverlap = true;

        while (hasOverlap) {
            randWidth = rand.nextInt(Room.maxSize - Room.minSize) + Room.minSize + 3;
            randHeight = rand.nextInt(Room.maxSize - Room.minSize) + Room.minSize + 3;
            randX = rand.nextInt(WIDTH - randWidth);
            randY = rand.nextInt(HEIGHT - randHeight);
            newRoom = new Room(randX, randY, randWidth, randHeight);

            Room finalNewRoom = newRoom;
            hasOverlap = rooms.stream().anyMatch(room -> finalNewRoom.isOverlap(room));

            if (!hasOverlap) {
                rooms.add(newRoom);
                int maxX = randX + randWidth - 1;
                int maxY = randY + randHeight - 1;
                for (int x = randX; x <= maxX; x++) {
                    for (int y = randY; y <= maxY; y++) {
                        world[x][y] = (x == randX || x == maxX || y == randY || y == maxY) ? bounds : inner;
                    }
                }
            }
        }

        return newRoom;
    }

    // draw hallways between 2 rooms
    private void drawHallway(Room room1, Room room2) {

        // get random points in the rooms
        int x1 = rand.nextInt(room1.width - 2) + room1.xStart + 1;
        int x2 = rand.nextInt(room2.width - 2) + room2.xStart + 1;
        int y1 = rand.nextInt(room1.height - 2) + room1.yStart;
        int y2 = rand.nextInt(room2.height - 2) + room2.yStart;

        // draw a straight hallway between the two points
        int hallwayWidth = 2;

        int hallwayLengthY = Math.abs(y2 - y1);
        int hallwayLengthX = Math.abs(x2 - x1);
        int yStart;
        int xStart;

        if (y2 > y1) {
            yStart = y1;
            xStart = x1;
        } else {
            yStart = y2;
            xStart = x2;
        }

        //int randDir = RandomUtils.uniform(new Random(), 2); //changes direction of turning hallway

        //if (randDir == 1) {
        changeToTileY(xStart, yStart, hallwayWidth, hallwayLengthY);
        //} //changes from L to T shape
        int xStart2;

        if (x2 > x1) {
            xStart2 = x1;
        } else {
            xStart2 = x2;
        }

        if (xStart == xStart2 /*&& randDir == 1*/) {
            changeToTileX(xStart2, yStart + hallwayLengthY, hallwayLengthX, hallwayWidth);
        }
        /*else if (xStart == xStart2){ //y hallway is not set up yet (randDir)
            changeToTileX(xStart2, yStart, hallwayLengthX, hallwayWidth);
            changeToTileY(xStart2 + hallwayLengthX, yStart, hallwayLengthY, hallwayWidth);
        }
        else if (randDir != 1) { //y hallway is not set up yet (randDir)
            changeToTileX(xStart2, yStart, hallwayLengthX + hallwayWidth - 1, hallwayWidth);
            changeToTileY(xStart2 + hallwayLengthX, yStart, hallwayLengthX + hallwayWidth - 1, hallwayWidth);
        }*/
        else {
            changeToTileX(xStart2, yStart + hallwayLengthY, hallwayLengthX + hallwayWidth - 1, hallwayWidth);
        }
    }

    private void changeToTileY(int xStart, int yStart, int width, int height) {
        for (int x = xStart; x <= xStart + width; x++) {
            for (int y = yStart; y <= yStart + height + width; y++) {
                if (x == xStart || x == xStart + width) {
                    if (world[x][y] == Tileset.NOTHING) {
                        world[x][y] = bounds;
                    }
                } else if (y == yStart) {
                    if ((y - 1) >= 0 && world[x][y - 1] == Tileset.NOTHING) {
                        world[x][y] = bounds;
                    }
                } else if (y == yStart + width + height) {
                    if ((y + 1) < HEIGHT && world[x][y + 1] == Tileset.NOTHING) {
                        world[x][y] = bounds;
                    }
                } else {
                    world[x][y] = inner;
                }
            }
        }
    }

    private void changeToTileX(int xStart, int yStart, int width, int height) {
        for (int x = xStart; x <= xStart + width; x++) {
            for (int y = yStart; y <= yStart + height; y++) {
                if (y == yStart || y == yStart + height) {
                    if (world[x][y] == Tileset.NOTHING) {
                        world[x][y] = bounds;
                    }
                } else if (x == xStart) {
                    if ((x - 1) >= 0 && world[x - 1][y] == Tileset.NOTHING) {
                        world[x][y] = bounds;
                    }
                } else if (x == xStart + width) {
                    if ((x + 1) < WIDTH && world[x + 1][y] == Tileset.NOTHING) {
                        world[x][y] = bounds;
                    }
                } else {
                    world[x][y] = inner;
                }
            }
        }
    }

    // show the world map
    public void showWorld(TETile[][] worldMap) {
        isPlaying = true;
        ter.renderFrame(worldMap);
    }

    // update avatar location
    public TETile showChar(Position oldPos, Position newPos, TETile type, TETile prevType) {
        if (oldPos != null) {
            currentWorld[oldPos.x][oldPos.y] = prevType;
        }

        TETile keepTile = prevType;
        if (currentWorld[newPos.x][newPos.y] != Tileset.FLOWER) {
            keepTile = currentWorld[newPos.x][newPos.y];
        }

        currentWorld[newPos.x][newPos.y] = type;
        return keepTile;
    }

    /** UI methods **/
    // UI to show seed as user is typing
    private void drawSeedInterface() {
        StdDraw.clear(Color.BLACK);
        drawTitle();
        StdDraw.text(MIDWIDTH, MIDHEIGHT, "Please input a number as seed:");
        StdDraw.text(MIDWIDTH, MIDHEIGHT - 2 * LINESIZE, "Start (S)");
        StdDraw.text(MIDWIDTH, MIDHEIGHT - 3 * LINESIZE, "Quit (Q)");
    }

    // opening menu UI
    private void drawMenu() {
        drawTitle();
        StdDraw.text(MIDWIDTH, MIDHEIGHT, "New Game (N)");
        StdDraw.text(MIDWIDTH, MIDHEIGHT - LINESIZE, "Load Game (L)");
        StdDraw.text(MIDWIDTH, MIDHEIGHT - LINESIZE - LINESIZE, "Quit (Q)");
        StdDraw.show();
    }

    // title UI
    private void drawTitle() {
        StdDraw.clear(Color.black);
        StdDraw.setPenColor(Color.white);
        StdDraw.text(MIDWIDTH, MIDHEIGHT / 2 * 3, "CS61B \n Build Your Own World");
    }

    // when win
    private void showWin() {
        StdDraw.clear(Color.black);
        StdDraw.setPenColor(Color.white);
        StdDraw.text(MIDWIDTH, MIDHEIGHT + LINESIZE, "YOU WIN!");

        StdDraw.show();
    }

    // show HUD
    private void drawHud() {
        StdDraw.setPenColor(Color.white);
        StdDraw.textLeft(LINESIZE, HEIGHT + 2 * LINESIZE,  "Up (W) \n Down (S) \n Left (A) \n Right (D) \n Save (:Q)");
        StdDraw.textRight(WIDTH - LINESIZE, HEIGHT + 2 * LINESIZE, "Click the tile to get the description");
        StdDraw.show();

        if (StdDraw.isMousePressed()) {
            int x = (int) StdDraw.mouseX();
            int y = (int) StdDraw.mouseY();
            Position pos = new Position(x, y);
            String description = "";
            if (pos.x >= 0 && pos.x < WIDTH && pos.y >= 0 && pos.y < HEIGHT) {
                TETile target = currentWorld[x][y];
                clearHud();
                if (currentWorld == world) {
                    if (target == bounds) {
                        description = "It's a " + target.description() + ", you can't go through it.";
                    } else if (target == inner) {
                        description = "It's a " + target.description() + ", you can go through it.";
                    } else if (target == Tileset.NOTHING) {
                        description = "Don't worry about this. You will not reach this tile";
                    } else if (target == Tileset.FLOWER) {
                        description = "It's a flower, collect it to open the locked door.";
                    } else if (target == Tileset.LOCKED_DOOR) {
                        description = "It's a locked door, get the flower to unlock a secret world.";
                    } else if (target == Tileset.UNLOCKED_DOOR) {
                        description = "It's a door to a secret world. Go to it to win the game!";
                    } else if (target == Tileset.AVATAR) {
                        description = "That's you! Try moving around using your keyboard's AWSD.";
                    }
                } else if (currentWorld == mirrorWorld) {
                    if (target == bounds) {
                        description = "It's a " + target.description() + ", you can't go through it.";
                    } else if (target == Tileset.WATER) {
                        description = "It's water, you can go through it.";
                    } else if (target == Tileset.NOTHING) {
                        description = "Don't worry about this. You will not reach this tile";
                    } else if (target == Tileset.FLOWER) {
                        description = "It's a flower, collect all of them to win!";
                    } else if (target == Tileset.AVATAR) {
                        description = "That's you! Try moving around using your keyboard's AWSD.";
                    }
                }
                StdDraw.textRight(WIDTH - LINESIZE, HEIGHT + LINESIZE, description);
            }
            StdDraw.show();
        }
    }

    // reset HUD
    private void clearHud() {
        StdDraw.setPenColor(Color.BLACK);
        StdDraw.filledRectangle(WIDTH / 2, HEIGHT + 3 / 2 * LINESIZE, WIDTH / 2, LINESIZE);
        StdDraw.setPenColor(Color.WHITE);
    }

    /** handle user input **/
    // get seed number
    private void getSeed() {
        drawSeedInterface();
        StdDraw.show();

        seed = 0;
        boolean done = false;
        while (!done) {
            if (StdDraw.hasNextKeyTyped()) {
                char input = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (input == 'q') {
                    System.exit(0);
                } else if (input == 's') {
                    done = true;
                } else if (input >= '0' && input <= '9') {
                    seed = seed * SEEDHELPER + (input - '0');
                    drawSeedInterface();
                    StdDraw.text(MIDWIDTH, MIDHEIGHT - LINESIZE, String.valueOf(seed));
                    StdDraw.show();
                }
            }
        }
    }

    // save world (if user types ":q")
    private void saveWorld() {
        File f = new File("./saved_world.txt");
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            FileOutputStream fs = new FileOutputStream(f);
            ObjectOutputStream os = new ObjectOutputStream(fs);
            String save = savedActions.substring(0, savedActions.length() - 2);
            os.writeObject(save);
        }  catch (FileNotFoundException e) {
            System.out.println("file not found");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    // load saved world (if user types "l")
    private String loadWorld() {
        File f = new File("./saved_world.txt");
        if (f.exists()) {
            try {
                FileInputStream fs = new FileInputStream(f);
                ObjectInputStream os = new ObjectInputStream(fs);
                return (String) os.readObject();
            } catch (FileNotFoundException e) {
                System.out.println(e);
            } catch (IOException e) {
                System.out.println(e);
                System.exit(0);
            } catch (ClassNotFoundException e) {
                System.out.println("class not found");
                System.exit(0);
            }
        }
        return "";
    }

    private void handle(char action) {

        action = toLowerCase(action);
        savedActions.append(action);

        if (isPlaying && !isWin) {
            Position newPos = avatarPos;
            if (action == 'w') {
                newPos = avatarPos.moveUp();
            } else if (action == 's') {
                newPos = avatarPos.moveDown();
            } else if (action == 'a') {
                newPos = avatarPos.moveLeft();
            } else if (action == 'd') {
                newPos = avatarPos.moveRight();
            } else if (action == 'q') {
                char lastAction = savedActions.charAt(savedActions.length() - 2);
                if (lastAction == ':') {
                    saveWorld();
                    System.exit(0);
                }
            }

            TETile[][] oldWorld = currentWorld;

            if (isGoodPos(newPos)) {
                if (oldWorld == currentWorld) {
                    prevTile = showChar(avatarPos, newPos, Tileset.AVATAR, prevTile);
                    avatarPos = newPos;
                }
            }

        } else {
            if (action == 'n') {
                getSeed();
                buildNewWorld();
                savedActions.append(seed);
                savedActions.append('s');
                showWorld(currentWorld);
            } else if (action == 'l') {
                String actions = loadWorld();
                if (actions.length() > 0) {
                    interactWithInputString(actions);
                    showWorld(currentWorld);
                } else {
                    StdDraw.text(MIDWIDTH, MIDHEIGHT, "Sorry, you don't have any saved progress.");
                }
            } else if (action == 'q') {
                System.exit(0);
            }
        }

        if (collectedFlowers == finalFlowers.length) {
            isWin = true;
            showWin();
        }
    }

    private boolean isGoodPos(Position pos) {
        if (currentWorld == world) {
            if (currentWorld[pos.x][pos.y] == inner || currentWorld[pos.x][pos.y] == Tileset.LOCKED_DOOR) {
                return true;
            } else if (currentWorld[pos.x][pos.y] == Tileset.FLOWER) {
                currentWorld[doorPos.x][doorPos.y] = Tileset.UNLOCKED_DOOR;
                return true;
            } else if (currentWorld[pos.x][pos.y] == Tileset.UNLOCKED_DOOR) {
                currentWorld = mirrorWorld;
                buildMirrorWorld();
                showWorld(mirrorWorld);
                return true;
            }
        } else if (currentWorld == mirrorWorld) {
            if (currentWorld[pos.x][pos.y] == Tileset.WATER) {
                return true;
            } else if (currentWorld[pos.x][pos.y] == Tileset.FLOWER) {
                collectedFlowers++;
                return true;
            }
        }

        return false;
    }
}

