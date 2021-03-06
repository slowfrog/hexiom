package com.slowfrog.hexiom;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main2 {
  private static final int DESCENDING = -1;
  private static final int ASCENDING = 1;
  private static final int EMPTY = 7;

  private static final int OPEN = 0;
  private static final int SOLVED = 1;
  private static final int IMPOSSIBLE = -1;

  // //////////////////////////////////////////////
  public static int makePoint(int x, int y) {
    return ((x & 0xf) << 4) | (y & 0xf);
  }

  public static int pointX(int point) {
    return (point >> 4) & 0xf;
  }

  public static int pointY(int point) {
    return point & 0xf;
  }

  // ///////////////////////////////////////////////
  public static class Done implements Cloneable {
    public static final int MIN_CHOICE_STRATEGY = 0;
    public static final int MAX_CHOICE_STRATEGY = 1;
    public static final int HIGHEST_VALUE_STRATEGY = 2;
    public static final int FIRST_STRATEGY = 3;
    public static final int MAX_NEIGHBORS_STRATEGY = 4;
    public static final int MIN_NEIGHBORS_STRATEGY = 5;

    private static final int[] BITCOUNT = new int[256];

    static {
      for (int i = 0; i < 256; ++i) {
        int count = 0;
        for (int j = 1; j < 255; j <<= 1) {
          if ((j & i) != 0) {
            ++count;
          }
        }
        BITCOUNT[i] = count;
      }
    }

    public int count;
    private int[] cells;

    public Done(int count, boolean empty) {
      this.count = count;
      if (!empty) {
        this.cells = new int[this.count];
        Arrays.fill(this.cells, 0xff);
      }
    }

    public Done clone() {
      Done ret = new Done(this.count, true);
      ret.cells = new int[this.count];
      System.arraycopy(this.cells, 0, ret.cells, 0, this.count);
      return ret;
    }

    public int get(int i) {
      return this.cells[i];
    }

    public int getVal(int i) {
      switch (this.get(i)) {
      case 0x01:
        return 0;
      case 0x02:
        return 1;
      case 0x04:
        return 2;
      case 0x08:
        return 3;
      case 0x10:
        return 4;
      case 0x20:
        return 5;
      case 0x40:
        return 6;
      case 0x80:
        return EMPTY;
      default:
        throw new RuntimeException("Error getVal: " + this.get(i));
      }
    }

    public void setDone(int i, int v) {
      this.cells[i] = (0x1 << v);
    }

    public boolean isSet(int i, int v) {
      return (this.cells[i] & (0x1 << v)) != 0;
    }

    public int countChoices(int i) {
      return BITCOUNT[this.cells[i]];
    }

    public boolean alreadyDone(int i) {
      return this.countChoices(i) == 1;
    }

    public boolean remove(int i, int v) {
      int bitv = 0x1 << v;
      if ((this.cells[i] & bitv) != 0) {
        this.cells[i] &= ~bitv;
        return true;
      } else {
        return false;
      }
    }

    public void removeAll(int v) {
      for (int i = 0; i < this.count; ++i) {
        this.remove(i, v);
      }
    }

    public boolean removeUnfixed(int v) {
      boolean changed = false;
      for (int i = 0; i < this.count; ++i) {
        if (!this.alreadyDone(i)) {
          if (this.remove(i, v)) {
            changed = true;
          }
        }
      }
      return changed;
    }

    public void filterTiles(int[] tiles) {
      for (int v = 0; v < tiles.length; ++v) {
        if (tiles[v] == 0) {
          this.removeAll(v);
        }
      }
    }

    public int nextCellMinChoice() {
      int minlen = 10;
      int mini = -1;
      for (int i = 0; i < this.count; ++i) {
        int choices = this.countChoices(i);
        if ((1 < choices) && (choices < minlen)) {
          minlen = choices;
          mini = i;
        }
      }
      return mini;
    }

    public int nextCellMaxChoice() {
      int maxlen = 1;
      int maxi = -1;
      for (int i = 0; i < this.count; ++i) {
        int choices = this.countChoices(i);
        if (maxlen < choices) {
          maxlen = choices;
          maxi = i;
        }
      }
      return maxi;
    }

    public int nextCellHighestValue() {
      int maxval = -1;
      int maxi = -1;
      for (int i = 0; i < this.count; ++i) {
        if (!this.alreadyDone(i)) {
          int maxvali = 0;
          for (int j = 0; j < 7; ++j) {
            if (this.isSet(i, j)) {
              maxvali = j;
            }
          }
          if (maxval < maxvali) {
            maxval = maxvali;
            maxi = i;
          }
        }
      }
      return maxi;
    }

    public int nextCellFirst() {
      for (int i = 0; i < this.count; ++i) {
        if (!this.alreadyDone(i)) {
          return i;
        }
      }
      return -1;
    }
    
    public int nextCellMaxNeighbors(Pos pos) {
      int maxn = -1;
      int maxi = -1;
      for (int i = 0; i < this.count; ++i) {
        if (!this.alreadyDone(i)) {
          int n = 0;
          int[] cellsAround = pos.hex.getById(i).links;
          for (int c = 0; c < cellsAround.length; ++c) {
            int nid = cellsAround[c];
            if (this.alreadyDone(nid) && (this.getVal(nid) != EMPTY)) {
              n += 1;
            }
          }
          if (n > maxn) {
            maxn = n;
            maxi = i;
          }
        }
      }
      return maxi;
    }

    public int nextCellMinNeighbors(Pos pos) {
      int minn = 7;
      int mini = -1;
      for (int i = 0; i < this.count; ++i) {
        if (!this.alreadyDone(i)) {
          int n = 0;
          int[] cellsAround = pos.hex.getById(i).links;
          for (int c = 0; c < cellsAround.length; ++c) {
            int nid = cellsAround[c];
            if (this.alreadyDone(nid) && (this.getVal(nid) != EMPTY)) {
              n += 1;
            }
          }
          if (n < minn) {
            minn = n;
            mini = i;
          }
        }
      }
      return mini;
    }

    public int nextCell(Pos pos, int strategy) {
      switch (strategy) {
      case MIN_CHOICE_STRATEGY:
        return this.nextCellMinChoice();
      case MAX_CHOICE_STRATEGY:
        return this.nextCellMaxChoice();
      case HIGHEST_VALUE_STRATEGY:
        return this.nextCellHighestValue();
      case FIRST_STRATEGY:
        return this.nextCellFirst();
      case MIN_NEIGHBORS_STRATEGY:
        return this.nextCellMinNeighbors(pos);
      case MAX_NEIGHBORS_STRATEGY:
        return this.nextCellMaxNeighbors(pos);
      default:
        throw new RuntimeException("Wrong strategy: " + strategy);
      }
    }

    public String toString() {
      String str = "Cells=[";
      for (int i = 0; i < this.count; ++i) {
        if (i > 0) {
          str += ", ";
        }
        str += "[ ";
        for (int v = 0; v < 8; ++v) {
          if (this.isSet(i, v)) {
            str += v + " ";
          }
        }
        str += "]";
      }

      str += "]";
      return str;
    }
  }

  // //////////////////////////////////////////////
  static class Node {
    public int pos;

    public int id;

    public int[] links;

    public Node(int pos, int id, int[] links) {
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

    public Node[] nodesByPos;

    public Hex(int size) {
      this.size = size;
      this.count = 3 * size * (size - 1) + 1;
      this.nodesById = new Node[this.count];
      int maxCode = 256;
      this.nodesByPos = new Node[maxCode];
      int id = 0;
      for (int y = 0; y < size; ++y) {
        for (int x = 0; x < size + y; ++x) {
          int pos = makePoint(x, y);
          Node node = new Node(pos, id, null);
          this.nodesByPos[pos] = node;
          this.nodesById[id] = node;
          id += 1;
        }
      }
      for (int y = 1; y < size; ++y) {
        for (int x = y; x < size * 2 - 1; ++x) {
          int ry = size + y - 1;
          int pos = makePoint(x, ry);
          Node node = new Node(pos, id, null);
          this.nodesByPos[pos] = node;
          this.nodesById[id] = node;
          id += 1;
        }
      }
    }

    public void linkNodes() {
      final int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { -1, -1 } };

      for (int i = 0; i < this.nodesById.length; ++i) {
        Node node = this.nodesById[i];
        int p = node.pos;
        for (int d = 0; d < dirs.length; ++d) {
          int nx = pointX(p) + dirs[d][0];
          int ny = pointY(p) + dirs[d][1];
          int ncode = makePoint(nx, ny);
          if (this.containsPos(ncode)) {
            node.appendLink(this.nodesByPos[ncode].id);
          }
        }
      }
    }

    public boolean containsPos(int code) {
      return (code >= 0) && (code < this.nodesByPos.length) && (this.nodesByPos[code] != null);
    }

    public Node getByPos(int pos) {
      return this.nodesByPos[pos];
    }

    public Node getById(int id) {
      return this.nodesById[id];
    }
  }

  // //////////////////////////////////////////////
  private static int[] makeTiles() {
    return new int[8];
  }

  public static int sumTiles(int[] tiles) {
    int sum = 0;
    for (int i = 0; i < 7; ++i) {
      sum += tiles[i];
    }
    return sum;
  }

  // //////////////////////////////////////////////
  public static class Pos implements Cloneable {
    public Hex hex;

    public int[] tiles;

    public int sumTiles;

    public Done done;

    public Pos(Hex hex, int[] tiles, Done done) {
      this.hex = hex;
      this.tiles = tiles;
      this.sumTiles = sumTiles(tiles);
      this.done = done;
    }

    public Pos clone() {
      return new Pos(this.hex, this.tiles, this.done.clone());
    }
    
    public int getVal(int i) {
      return this.done.getVal(i);
    }
  }
  
  public static Pos problem;
  public static Pos solution;
  
  public static Pos getSolution() {
    System.out.println("Getting solution");
    if (solution == null) {
      System.out.println("NULL!!!!");
    }
    return solution;
  }

  // ////////////////////////////////////////////////////
  private static boolean constraintPass(Pos pos, int lastMove) {
    boolean changed = false;
    int[] left = new int[pos.tiles.length];
    System.arraycopy(pos.tiles, 0, left, 0, left.length);
    Done done = pos.done;

    // Remove impossible values from free cells
    int[] freeCells;
    if (lastMove >= 0) {
      freeCells = new int[] { lastMove };
    } else {
      freeCells = new int[pos.done.count];
      for (int i = 0; i < freeCells.length; ++i) {
        freeCells[i] = i;
      }
    }
    for (int ii = 0; ii < freeCells.length; ++ii) {
      int i = freeCells[ii];
      if (!done.alreadyDone(i)) {
        int vmax = 0;
        int vmin = 0;
        int[] cellsAround = pos.hex.getById(i).links;
        for (int ni = 0; ni < cellsAround.length; ++ni) {
          int nid = cellsAround[ni];
          if (done.alreadyDone(nid)) {
            if (done.getVal(nid) != EMPTY) {
              vmin += 1;
              vmax += 1;
            }
          } else {
            vmax += 1;
          }
        }
        for (int num = 0; num < 7; ++num) {
          if ((num < vmin) || (num > vmax)) {
            if (done.remove(i, num)) {
              changed = true;
            }
          }
        }
      }
    }

    // Computes how many of each value is still free
    for (int i = 0; i < done.count; ++i) {
      if (done.countChoices(i) == 1) {
        left[done.getVal(i)] -= 1;
      }
    }

    for (int v = 0; v < 8; ++v) {
      // If there is none, remove the possibility from all tiles
      if ((pos.tiles[v] > 0) && (left[v] == 0)) {
        if (done.removeUnfixed(v)) {
          changed = true;
        }
      } else {
        int possible = 0;
        for (int i = 0; i < done.count; ++i) {
          if (done.isSet(i, v)) {
            possible += 1;
          }
        }
        // If the number of possible cells for a value is exactly the number of
        // available tiles put a tile in each cell
        if (pos.tiles[v] == possible) {
          for (int i = 0; i < done.count; ++i) {
            if ((!done.alreadyDone(i)) && done.isSet(i, v)) {
              done.setDone(i, v);
              changed = true;
            }
          }
        }
      }
    }
    
    // Force empty or non-empty around filled cells
    int[] filledCells;
    if (lastMove >= 0) {
      filledCells = new int[] { lastMove };
    } else {
      filledCells = new int[pos.done.count];
      for (int i = 0; i < filledCells.length; ++i) {
        filledCells[i] = i;
      }
    }
    
    for (int ii = 0; ii < filledCells.length; ++ii) {
      int i = filledCells[ii];
      if (done.alreadyDone(i)) {
        int num = done.getVal(i);
        int empties = 0;
        int filled = 0;
        List<Integer> unknown = new ArrayList<Integer>(6);
        int[] cellsAround = pos.hex.getById(i).links;
        for (int c = 0; c < cellsAround.length; ++c) {
          int nid = cellsAround[c];
          if (done.alreadyDone(nid)) {
            if (done.getVal(nid) == EMPTY) {
              empties += 1;
            } else {
              filled += 1;
            }
          } else {
            unknown.add(nid);
          }
        }
        if (unknown.size() > 0) {
          if (num == filled) {
            for (int u : unknown) {
              if (done.isSet(u, EMPTY)) {
                done.setDone(u, EMPTY);
                changed = true;
              }
            }
          } else if (num == filled + unknown.size()) {
            for (int u : unknown) {
              if (done.remove(u, EMPTY)) {
                changed = true;
              }
            }
          }
        }
      }
    }

    return changed;
  }

  private static int findMoves(Pos pos, int[] moves, int strategy, int order) {
    int count = 0;
    int index = 0;
    Done done = pos.done;
    int cellId = done.nextCell(pos, strategy);
    if (cellId < 0) {
      return count;
    }

    if (order == ASCENDING) {
      for (int v = 0; v < 8; ++v) {
        if (done.isSet(cellId, v)) {
          moves[index] = cellId;
          moves[index + 1] = v;
          count += 1;
          index += 2;
        }
      }
    } else {
      for (int v = 6; v >= 0; --v) {
        if (done.isSet(cellId, v)) {
          moves[index] = cellId;
          moves[index + 1] = v;
          count += 1;
          index += 2;
        }
      }
      if (done.isSet(cellId, EMPTY)) {
        moves[index] = cellId;
        moves[index + 1] = EMPTY;
        count += 1;
      }
    }
    return count;
  }

  private static void playMove(Pos pos, int cellId, int value) {
    pos.done.setDone(cellId, value);
  }

  private static final String SPACES = "                                                  ";

  private static void printPos(Pos pos) {
    Hex hex = pos.hex;
    Done done = pos.done;
    int size = hex.size;
    for (int y = 0; y < size; ++y) {
      System.out.print(SPACES.substring(0, size - y - 1));
      for (int x = 0; x < size + y; ++x) {
        int pos2 = makePoint(x, y);
        int id = hex.getByPos(pos2).id;
        if (done.alreadyDone(id)) {
          if (done.getVal(id) != EMPTY) {
            System.out.print(done.getVal(id));
          } else {
            System.out.print(".");
          }
        } else {
          System.out.print("?");
        }
        System.out.print(" ");
      }
      System.out.println();
    }
    for (int y = 1; y < size; ++y) {
      System.out.print(SPACES.substring(0, y));
      for (int x = y; x < size * 2 - 1; ++x) {
        int ry = size + y - 1;
        int pos2 = makePoint(x, ry);
        int id = hex.getByPos(pos2).id;
        if (done.alreadyDone(id)) {
          if (done.getVal(id) != EMPTY) {
            System.out.print(done.getVal(id));
          } else {
            System.out.print(".");
          }
        } else {
          System.out.print("?");
        }
        System.out.print(" ");
      }
      System.out.println();
    }
  }

  private static int solved(Pos pos) {
    Hex hex = pos.hex;
    int[] tiles = new int[pos.tiles.length];
    System.arraycopy(pos.tiles, 0, tiles, 0, tiles.length);
    Done done = pos.done;
    boolean exact = true;
    boolean allDone = true;
    for (int i = 0; i < hex.count; ++i) {
      if (done.countChoices(i) == 0) {
        return IMPOSSIBLE;
      } else if (done.alreadyDone(i)) {
        int num = done.getVal(i);
        tiles[num] -= 1;
        if (tiles[num] < 0) {
          return IMPOSSIBLE;
        }
        int max = 0;
        int min = 0;
        if (num != EMPTY) {
          int[] cellsAround = hex.getById(i).links;
          for (int d = 0; d < cellsAround.length; ++d) {
            int nid = cellsAround[d];
            if (done.alreadyDone(nid)) {
              if (done.getVal(nid) != EMPTY) {
                min += 1;
                max += 1;
              }
            } else {
              max += 1;
            }
          }
          if ((num < min) || (num > max)) {
            return IMPOSSIBLE;
          }
          if (num != min) {
            exact = false;
          }
        }
      } else {
        allDone = false;
      }
    }
    if ((!allDone) || (!exact)) {
      return OPEN;
    }
    solution = pos;
    printPos(pos);
    return SOLVED;
  }

  private static int solveStep(Pos prev, int strategy, int order, boolean first) {
    Pos pos;
    if (first) {
      pos = prev.clone();
      while (constraintPass(pos, -1)) {
      }
    } else {
      pos = prev;
    }

    int[] moves = new int[16];
    int count = findMoves(pos, moves, strategy, order);
    if (count == 0) {
      return solved(pos);
    } else {
      for (int i = 0; i < count; ++i) {
        int cellId = moves[2 * i];
        int value = moves[2 * i + 1];
        int ret = OPEN;
        Pos newPos = pos.clone();
        playMove(newPos, cellId, value);
        while (constraintPass(newPos, cellId)) {
        }
        int curStatus = solved(newPos);
        if (curStatus != OPEN) {
          ret = curStatus;
        } else {
          ret = solveStep(newPos, strategy, order, false);
        }
        if (ret == SOLVED) {
          return ret;
        }
      }
    }
    return IMPOSSIBLE;
  }

  private static void checkValid(Pos pos) {
    Hex hex = pos.hex;
    int[] tiles = pos.tiles;
    int tot = 0;
    // Fill missing entries in tiles
    for (int i = 0; i < 8; ++i) {
      if (tiles[i] > 0) {
        tot += tiles[i];
      } else {
        tiles[i] = 0;
      }
    }
    // Check total
    if (tot != hex.count) {
      throw new RuntimeException("Invalid input. Expected " + hex.count + " tiles, got " + tot +
                                 ".");
    }
  }

  private static int solve(Pos pos, int strategy, int order) {
    checkValid(pos);
    problem = pos;
    return solveStep(pos, strategy, order, true);
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
    int[] tiles = makeTiles();
    Done done = new Done(hex.count, false);
    for (int y = 0; y < size; ++y) {
      String line = lines.get(linei).substring(size - y - 1);
      int p = 0;
      for (int x = 0; x < size + y; ++x) {
        String tile = line.substring(p, p + 2);
        p += 2;
        int inctile = 0;
        if (tile.charAt(1) == '.') {
          inctile = EMPTY;
        } else {
          inctile = Integer.parseInt(tile.substring(1));
        }
        tiles[inctile] += 1;
        if (tile.charAt(0) == '+') {
          System.out.printf("Adding locked tile: %d at pos %d, %d, id=%d\n", inctile, x, y,
              hex.getByPos(makePoint(x, y)).id);
          done.setDone(hex.getByPos(makePoint(x, y)).id, inctile);
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
          inctile = EMPTY;
        } else {
          inctile = Integer.parseInt(tile.substring(1));
        }
        tiles[inctile] += 1;
        if (tile.charAt(0) == '+') {
          System.out.printf("Adding locked tile: %d at pos %d, %d, id=%d\n", inctile, x, ry,
              hex.getByPos(makePoint(x, ry)).id);
          done.setDone(hex.getByPos(makePoint(x, ry)).id, inctile);
        }
      }
      linei += 1;
    }
    hex.linkNodes();
    done.filterTiles(tiles);
    return new Pos(hex, tiles, done);
  }

  private static void solveFile(String file, int strategy, int order) {
    try {
      Pos pos = readFile(file);
      solve(pos, strategy, order);
    } catch (IOException e) {
      System.out.println("Cannot solve " + file + ", error reading");
      e.printStackTrace(System.out);
    }
  }

  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    int order = DESCENDING;
    int strategy = Done.HIGHEST_VALUE_STRATEGY;

    for (String f : args) {
      if (f.startsWith("-")) {
        if (f.equals("-u")) {
          System.out.println("Usage: -u      show usage");
          System.out.println("       -smin   use 'minimum choices' strategy");
          System.out.println("       -smax   use 'maximum choices' strategy");
          System.out.println("       -shigh  use 'highest value' strategy [default]");
          System.out.println("       -sfirst use 'first' strategy");
          System.out.println("       -sminnb use 'minimum neighbors' strategy");
          System.out.println("       -smaxnb use 'maximum neighbors' strategy");
          System.out.println("       -oasc   use ascending order");
          System.out.println("       -odesc  use descending order [default]");
          return;
        } else if (f.equals("-smin")) {
          strategy = Done.MIN_CHOICE_STRATEGY;
        } else if (f.equals("-smax")) {
          strategy = Done.MAX_CHOICE_STRATEGY;
        } else if (f.equals("-shigh")) {
          strategy = Done.HIGHEST_VALUE_STRATEGY;
        } else if (f.equals("-sfirst")) {
          strategy = Done.FIRST_STRATEGY;
        } else if (f.equals("-sminnb")) {
          strategy = Done.MIN_NEIGHBORS_STRATEGY;
        } else if (f.equals("-smaxnb")) {
          strategy = Done.MAX_NEIGHBORS_STRATEGY;
        } else if (f.equals("-oasc")) {
          order = ASCENDING;
        } else if (f.equals("-odesc")) {
          order = DESCENDING;
        } else {
          System.out.println("Ignoring unknown option: " + f);
          System.out.println("Use -u to see available options");
        }
      } else {
        System.out.println(" File : " + f);
        solveFile(f, strategy, order);
        System.out.println("-------------------");
      }
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
