package com.slowfrog.hexiom;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import JaCoP.constraints.Constraint;
import JaCoP.constraints.GCC;
import JaCoP.constraints.IfThenElse;
import JaCoP.constraints.Sum;
import JaCoP.constraints.XeqC;
import JaCoP.constraints.XeqY;
import JaCoP.core.BooleanVar;
import JaCoP.core.IntVar;
import JaCoP.core.Store;
import JaCoP.search.DepthFirstSearch;
import JaCoP.search.Indomain;
import JaCoP.search.IndomainList;
import JaCoP.search.IndomainMin;
import JaCoP.search.InputOrderSelect;
import JaCoP.search.Search;

public class Main3 {
  private static final int EMPTY = 7;

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
  public static class Done {
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

    public int getVal(int i) {
      switch (this.cells[i]) {
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
        throw new RuntimeException("Error getVal: " + this.cells[i]);
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
  public static class Pos {
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
      throw new RuntimeException("Invalid input. Expected " + hex.count + " tiles, got " + tot
          + ".");
    }
  }

  private static void solve(Pos pos) {
    checkValid(pos);
    problem = pos;

    int count = pos.hex.count;
    Store store = new Store();
    IntVar[] used = new IntVar[count];
    for (int i = 0; i < count; ++i) {
      used[i] = new BooleanVar(store, "used" + i);
    }
    IntVar[] cells = new IntVar[count];
    IntVar[] sums = new IntVar[count];
    for (int i = 0; i < count; ++i) {
      int mini = 0;
      int maxi = 7;
      if (pos.done.alreadyDone(i)) {
        mini = pos.done.getVal(i);
        maxi = mini;
      }
      cells[i] = new IntVar(store, "v" + i, mini, maxi);

      int[] cellsAround = pos.hex.nodesById[i].links;
      IntVar[] neighbors = new IntVar[cellsAround.length];
      for (int j = 0; j < cellsAround.length; ++j) {
        neighbors[j] = used[cellsAround[j]];
      }
      sums[i] = new IntVar(store, "s" + i, 0, 6);
      Sum sum = new Sum(neighbors, sums[i]);
      store.impose(sum);

      XeqC filled = new XeqC(used[i], 1);
      XeqY eqsum = new XeqY(cells[i], sums[i]);
      XeqC empty = new XeqC(cells[i], 7);
      IfThenElse ite = new IfThenElse(filled, eqsum, empty);
      store.impose(ite);
    }
    IntVar[] counts = new IntVar[8];
    for (int v = 0; v < 8; ++v) {
      counts[v] = new IntVar(store, "count" + v, pos.tiles[v], pos.tiles[v]);
    }
    Constraint cardinality = new GCC(cells, counts);
    store.impose(cardinality);

    store.consistency();
    //System.out.println("========== After consistency");
    //System.out.println(store.toString());

    Search<IntVar> search = new DepthFirstSearch<IntVar>();
    Indomain<IntVar> sorder = new IndomainList<IntVar>(new int[] { 0, 6, 5, 4, 3, 2, 1, 7 },
                                                       new IndomainMin<IntVar>());
    search.labeling(store, new InputOrderSelect<IntVar>(store, cells, sorder));
    //System.out.println("========== After search");
    //System.out.println(store.toString());

    for (int i = 0; i < count; ++i) {
      pos.done.setDone(i, cells[i].min());
    }
    printPos(pos);
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
      if (f.startsWith("-")) {
        if (f.equals("-u")) {
          System.out.println("Usage: -u      show usage");
        } else {
          System.out.println("Ignoring unknown option: " + f);
          System.out.println("Use -u to see available options");
        }
      } else {
        System.out.println(" File : " + f);
        solveFile(f);
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
