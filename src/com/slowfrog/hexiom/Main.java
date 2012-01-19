package com.slowfrog.hexiom;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
  private static final int NONE = -2;

  // //////////////////////////////////////////////
  static class Point {
    public int x;

    public int y;

    public Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public boolean equals(Object other) {
      if (!(other instanceof Point)) {
        return false;
      }
      Point p2 = (Point) other;
      return ((p2.x == this.x) && (p2.y == this.y));
    }

    public int hashCode() {
      return this.x + this.y * 20;
    }
    
    public String toString() {
      return "(" + x + "," + y + ")";
    }
  }

  // //////////////////////////////////////////////
  static class Dir {
    public int x;

    public int y;

    public Dir(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public static final Dir[] ALL = { new Dir(1, 0), new Dir(-1, 0), new Dir(0, 1),
        new Dir(0, -1), new Dir(1, 1), new Dir(-1, -1) };
    
    public String toString() {
      return "[" + x + "," + y + "]";
    }
  }

  // //////////////////////////////////////////////
  static class Done {
    private int count;

    private int done;

    private int[] cells;

    private int used;

    public Done(int count) {
      this.count = count;
      this.done = 0;
      this.cells = new int[this.count];
      Arrays.fill(this.cells, NONE);
      this.used = 0;
    }

    public boolean alreadyDone(int i) {
      return this.cells[i] != NONE;
    }

    public int nextCell() {
      return this.done;
    }

    public int get(int i) {
      return this.cells[i];
    }

    private void adjustDone() {
      for (int i = this.done; i < this.count; ++i) {
        if (this.cells[i] == NONE) {
          this.done = i;
          return;
        }
      }
      this.done = -1;
    }

    public void addDone(int i, int v) {
      this.cells[i] = v;
      this.used += 1;
      this.adjustDone();
    }

    public void removeDone(int i) {
      this.cells[i] = NONE;
      this.used -= 1;
      if ((this.done < 0) || (i < this.done)) {
        this.done = i;
      }
    }
  }

  // //////////////////////////////////////////////
  static class Node {
    public Point pos;

    public int id;

    public int[] links;

    public Node(Point pos, int id, int[] links) {
      this.pos = pos;
      this.id = id;
      this.links = (links != null ? links : new int[0]);
    }

    public void appendLink(int id) {
      int[] nlinks = new int[this.links.length + 1];
      System.arraycopy(this.links, 0, nlinks, 0, this.links.length);
      nlinks[this.links.length] = id;
      this.links = nlinks;
    }
    
    public String toString() {
      String lstr = "[";
      for (int i = 0; i < this.links.length; ++i) {
        if (i > 0) {
          lstr += ",";
        }
        lstr += i;
      }
      lstr += "]";
      return "<id=" + this.id + " " + this.pos + " " + lstr + ">";
    }
  }

  // //////////////////////////////////////////////
  static class Hex {
    public int size;

    public int count;

    public Node[] nodesById;

    public Map<Point, Node> nodesByPos;

    public Hex(int size) {
      this.size = size;
      this.count = 3 * size * (size - 1) + 1;
      this.nodesById = new Node[this.count];
      this.nodesByPos = new HashMap<Point, Node>();
      int id = 0;
      for (int y = 0; y < size; ++y) {
        for (int x = 0; x < size + y; ++x) {
          Point pos = new Point(x, y);
          Node node = new Node(pos, id, null);
          this.nodesByPos.put(pos, node);
          this.nodesById[id] = node;
          id += 1;
        }
      }
      for (int y = 1; y < size; ++y) {
        for (int x = y; x < size * 2 - 1; ++x) {
          int ry = size + y - 1;
          Point pos = new Point(x, ry);
          Node node = new Node(pos, id, null);
          this.nodesByPos.put(pos, node);
          this.nodesById[id] = node;
          id += 1;
        }
      }
    }

    public void linkNodes() {
      for (int i = 0; i < this.nodesById.length; ++i) {
        Node node = this.nodesById[i];
        Point p = node.pos;
        for (int d = 0; d < Dir.ALL.length; ++d) {
          Dir dir = Dir.ALL[d];
          int nx = p.x + dir.x;
          int ny = p.y + dir.y;
          if (this.containsPos(new Point(nx, ny))) {
            node.appendLink(this.nodesByPos.get(new Point(nx, ny)).id);
          }
        }
      }
    }

    public boolean containsPos(Point pos) {
      return this.nodesByPos.containsKey(pos);
    }

    public Node getByPos(Point pos) {
      return this.nodesByPos.get(pos);
    }

    public Node getById(int id) {
      return this.nodesById[id];
    }
  }

  // //////////////////////////////////////////////
  static class Tiles extends HashMap<Integer, Integer> {
    public static Tiles make() {
      Tiles ret = new Tiles();
      for (int i = -1; i < 7; ++i) {
        ret.put(i, 0);
      }
      return ret;
    }
  }

  // //////////////////////////////////////////////
  static class Pos {
    public Hex hex;

    public Tiles tiles;

    public Done done;

    public Pos(Hex hex, Tiles tiles, Done done) {
      this.hex = hex;
      this.tiles = tiles;
      this.done = done;
    }
  }

  // //////////////////////////////////////////////
  static class Move {
    public int cellId;

    public int value;

    public Move(int cellId, int value) {
      this.cellId = cellId;
      this.value = value;
    }
  }

  // //////////////////////////////////////////////
  private static final List<Move> NO_MOVES = new ArrayList<Move>();

  private static List<Move> findMoves(Pos pos) {
    Hex hex = pos.hex;
    Tiles tiles = pos.tiles;
    Done done = pos.done;
    int cellId = done.nextCell();
    if (cellId < 0) {
      return NO_MOVES;
    }

    List<Move> moves = new ArrayList<Move>();
    int[] cellsAround = null;
    int minPossible = 0;
    int maxPossible = 0;
    for (int i = -1; i < 7; ++i) {
      if (tiles.get(i) > 0) {
        boolean valid = true;
        if (i >= 0) {
          if (cellsAround == null) {
            cellsAround = hex.getById(cellId).links;
            maxPossible = cellsAround.length;
            for (int ca = 0; ca < cellsAround.length; ++ca) {
              int j = cellsAround[ca];
              if (done.alreadyDone(j)) {
                if (done.get(j) > 0) {
                  minPossible += 1;
                } else if (done.get(j) == 0) {
                  maxPossible = 0;
                  minPossible += 1;
                }
              }
            }
          }
          
          valid = (minPossible <= i) && (i <= maxPossible);
        }
        if (valid) {
          moves.add(new Move(cellId, i));
        }
      }
    }
    return moves;
  }

  private static void playMove(Pos pos, Move move) {
    pos.tiles.put(move.value, pos.tiles.get(move.value) - 1);
    pos.done.addDone(move.cellId, move.value);
  }

  private static void undoMove(Pos pos, Move move) {
    pos.tiles.put(move.value, pos.tiles.get(move.value) + 1);
    pos.done.removeDone(move.cellId);
  }

  private static final String SPACES = "                                                  ";

  private static void printPos(Pos pos) {
    Hex hex = pos.hex;
    Done done = pos.done;
    int size = hex.size;
    for (int y = 0; y < size; ++y) {
      System.out.print(SPACES.substring(0, size - y - 1));
      for (int x = 0; x < size + y; ++x) {
        Point pos2 = new Point(x, y);
        int id = hex.getByPos(pos2).id;
        if (done.alreadyDone(id) && (done.get(id) >= 0)) {
          System.out.print(done.get(id));
        } else {
          System.out.print(".");
        }
        System.out.print(" ");
      }
      System.out.println();
    }
    for (int y = 1; y < size; ++y) {
      System.out.print(SPACES.substring(0, y));
      for (int x = y; x < size * 2 - 1; ++x) {
        int ry = size + y - 1;
        Point pos2 = new Point(x, ry);
        int id = hex.getByPos(pos2).id;
        if (done.alreadyDone(id) && (done.get(id) >= 0)) {
          System.out.print(done.get(id));
        } else {
          System.out.print(".");
        }
        System.out.print(" ");
      }
      System.out.println();
    }
  }

  private static boolean solved(Pos pos) {
    Hex hex = pos.hex;
    Tiles tiles = pos.tiles;
    Done done = pos.done;
    int sum = 0;
    for (int i = 0; i < 7; ++i) {
      sum += tiles.get(i);
    }
    if (sum > 0) {
      return false;
    }
    for (int i = 0; i < hex.count; ++i) {
      Node node = hex.getById(i);
      int x = node.pos.x;
      int y = node.pos.y;
      int num = (done.alreadyDone(i) ? done.get(i) : -1);
      if (num >= 0) {
        for (int d = 0; d < Dir.ALL.length; ++d) {
          Dir dir = Dir.ALL[d];
          Point npos = new Point(x + dir.x, y + dir.y);
          if (hex.containsPos(npos)) {
            int nid = hex.getByPos(npos).id;
            if (done.alreadyDone(nid) && (done.get(nid) >= 0)) {
              num -= 1;
            }
          }
        }
        if (num != 0) {
          return false;
        }
      }
    }
    printPos(pos);
    return true;
  }

  private static boolean solveStep(Pos pos) {
    List<Move> moves = findMoves(pos);
    for (Move move : moves) {
      boolean ret = false;
      playMove(pos, move);
      if (solved(pos)) {
        ret = true;
      } else if (solveStep(pos)) {
        ret = true;
      }
      undoMove(pos, move);
      if (ret) {
        return ret;
      }
    }
    return false;
  }

  private static void checkValid(Pos pos) {
    Hex hex = pos.hex;
    Tiles tiles = pos.tiles;
    Done done = pos.done;
    int tot = done.used;
    // Fill missing entries in tiles
    for (int i = -1; i < 7; ++i) {
      if (tiles.containsKey(i)) {
        tot += tiles.get(i);
      } else {
        tiles.put(i, 0);
      }
    }
    // Check total
    if (tot != hex.count) {
      throw new RuntimeException("Invalid input. Expected " + hex.count + " tiles, got " + tot
          + ".");
    }
  }

  private static boolean solve(Pos pos) {
    checkValid(pos);
    return solveStep(pos);
  }

  private static Pos readFile(String file) throws IOException {
    List<String> lines = new ArrayList<String>();
    BufferedReader input = new BufferedReader(new FileReader(file));
    try {
      String line = null;
      while ((line = input.readLine()) != null) {
        lines.add(line);
      }
    } finally {
      input.close();
    }
    int size = Integer.parseInt(lines.get(0));
    Hex hex = new Hex(size);
    int linei = 1;
    Tiles tiles = Tiles.make();
    Done done = new Done(hex.count);
    for (int y = 0; y < size; ++y) {
      String line = lines.get(linei).substring(size - y - 1);
      int p = 0;
      for (int x = 0; x < size + y; ++x) {
        String tile = line.substring(p, p + 2);
        p += 2;
        int inctile = 0;
        if (tile.charAt(1) == '.') {
          inctile = -1;
        } else {
          inctile = Integer.parseInt(tile.substring(1));
        }
        if (tile.charAt(0) == '+') {
          System.out.printf("Adding locked tile: %d at pos %d, %d, id=%d\n", inctile, x, y,
              hex.getByPos(new Point(x, y)).id);
          done.addDone(hex.getByPos(new Point(x, y)).id, inctile);
        } else {
          tiles.put(inctile, tiles.get(inctile) + 1);
        }
      }
      linei += 1;
    }
    for (int y = 1; y < size; ++y) {
      int ry = size - 1 + y;
      String line = lines.get(linei).substring(y);
      int p = 0;
      for (int x = y; x < size * 2 - 1; ++x) {
        String tile = line.substring(p, p + 2);
        p += 2;
        int inctile = 0;
        if (tile.charAt(1) == '.') {
          inctile = -1;
        } else {
          inctile = Integer.parseInt(tile.substring(1));
        }
        if (tile.charAt(0) == '+') {
          System.out.printf("Adding locked tile: %d at pos %d, %d, id=%d\n", inctile, x, ry,
              hex.getByPos(new Point(x, ry)).id);
          done.addDone(hex.getByPos(new Point(x, ry)).id, inctile);
        } else {
          tiles.put(inctile, tiles.get(inctile) + 1);
        }
      }
      linei += 1;
    }
    hex.linkNodes();
    return new Pos(hex, tiles, done);
  }
  
  private static void solveFile(String file) {
    try {
      Pos pos = readFile(file);
      solve(pos);
    } catch (IOException e) {
      System.out.println("Cannot solve " + file + ", error reading");
      e.printStackTrace(System.out);
    }
  }
  
  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    for (String f : args) {
      System.out.println(" File : " + f);
      solveFile(f);
      System.out.println("----------------");
    }
    long end = System.currentTimeMillis();
    long fullDiffMilli = end - start;
    int milli = (int) (fullDiffMilli % 1000);
    int diffSeconds = (int) (fullDiffMilli / 1000);
    int sec = diffSeconds % 60;
    int min = diffSeconds / 60;
    System.out.printf("Real    %dm%d.%03ds\n", min, sec, milli);
    
  }
}
