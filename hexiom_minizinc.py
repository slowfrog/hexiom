from __future__ import division, print_function
import sys, os.path, subprocess

##################################
class Dir(object):
    def __init__(self, x, y):
        self.x = x
        self.y = y
        
DIRS = [ Dir(1, 0),
         Dir(-1, 0),
         Dir(0, 1),
         Dir(0, -1),
         Dir(1, 1),
         Dir(-1, -1) ]

EMPTY = 7

##################################
class Done(object):
    def __init__(self, count, empty=False):
        self.count = count
        self.cells = None if empty else [[0, 1, 2, 3, 4, 5, 6, EMPTY] for i in range(count)]

    def clone(self):
        ret = Done(self.count, True)
        ret.cells = [self.cells[i][:] for i in range(self.count)]
        return ret

    def __getitem__(self, i):
        return self.cells[i]

    def set_done(self, i, v):
        self.cells[i] = [v]

    def already_done(self, i):
        return len(self.cells[i]) == 1

    def remove(self, i, v):
        if v in self.cells[i]:
            self.cells[i].remove(v)
            return True
        else:
            return False
        
    def remove_all(self, v):
        for i in range(self.count):
            self.remove(i, v)
        
    def remove_unfixed(self, v):
        changed = False
        for i in range(self.count):
            if not self.already_done(i):
                if self.remove(i, v):
                    changed = True
        return changed
        
    def filter_tiles(self, tiles):
        for v in range(8):
            if tiles[v] == 0:
                self.remove_all(v)

##################################
class Node(object):
    def __init__(self, pos, id, links):
        self.pos = pos
        self.id = id
        self.links = links

##################################
class Hex(object):
    def __init__(self, size):
        self.size = size
        self.count = 3 * size * (size - 1) + 1
        self.nodes_by_id = self.count * [None]
        self.nodes_by_pos = {}
        id = 0
        for y in range(size):
            for x in range(size + y):
                pos = (x, y)
                node = Node(pos, id, [])
                self.nodes_by_pos[pos] = node
                self.nodes_by_id[node.id] = node
                id += 1
        for y in range(1, size):
            for x in range(y, size * 2 - 1):
                ry = size + y - 1
                pos = (x, ry)
                node = Node(pos, id, [])
                self.nodes_by_pos[pos] = node
                self.nodes_by_id[node.id] = node
                id += 1

    def link_nodes(self):
        for node in self.nodes_by_id:
            (x, y) = node.pos
            for dir in DIRS:
                nx = x + dir.x
                ny = y + dir.y
                if self.contains_pos((nx, ny)):
                    node.links.append(self.nodes_by_pos[(nx, ny)].id)

    def contains_pos(self, pos):
        return pos in self.nodes_by_pos
                    
    def get_by_pos(self, pos):
        return self.nodes_by_pos[pos]

    def get_by_id(self, id):
        return self.nodes_by_id[id]

        
##################################
class Pos(object):
    def __init__(self, hex, tiles, done = None):
        self.hex = hex
        self.tiles = tiles
        self.done = Done(hex.count) if done is None else done

    def clone(self):
        return Pos(self.hex, self.tiles, self.done.clone())
    
##################################
def print_pos(pos):
    hex = pos.hex
    done = pos.done
    size = hex.size
    for y in range(size):
        print(" " * (size - y - 1), end="")
        for x in range(size + y):
            pos2 = (x, y)
            id = hex.get_by_pos(pos2).id
            if done.already_done(id):
                c = str(done[id][0]) if (0 <= done[id][0] <= 6) else "."
            else:
                c = "?"
            print("%s " % c, end="")
        print()
    for y in range(1, size):
        print(" " * y, end="")
        for x in range(y, size * 2 - 1):
            ry = size + y - 1
            pos2 = (x, ry)
            id = hex.get_by_pos(pos2).id
            if done.already_done(id):
                c = str(done[id][0]) if (0 <= done[id][0] <= 6) else "."
            else:
                c = "?"
            print("%s " % c, end="")
        print()

def check_valid(pos):
    hex = pos.hex
    tiles = pos.tiles
    done = pos.done
    # fill missing entries in tiles
    tot = 0
    for i in range(8):
        if tiles[i] > 0:
            tot += tiles[i]
        else:
            tiles[i] = 0
    # check total
    if tot != hex.count:
        raise Exception("Invalid input. Expected %d tiles, got %d." % (hex.count, tot))


# TODO Write an 'iterator' to go over all x,y positions

def read_file(file):
    with open(file, "rt") as input:
        lines = [line.strip("\r\n") for line in input]
    size = int(lines[0])
    hex = Hex(size)
    linei = 1
    tiles = 8 * [0]
    done = Done(hex.count)
    for y in range(size):
        line = lines[linei][size - y - 1:]
        p = 0
        for x in range(size + y):
            tile = line[p:p + 2];
            p += 2
            if tile[1] == ".":
                inctile = EMPTY
            else:
                inctile = int(tile)
            tiles[inctile] += 1
            # Look for locked tiles    
            if tile[0] == "+":
                print("Adding locked tile: %d at pos %d, %d, id=%d" %
                      (inctile, x, y, hex.get_by_pos((x, y)).id))
                done.set_done(hex.get_by_pos((x, y)).id, inctile)
            
        linei += 1
    for y in range(1, size):
        ry = size - 1 + y
        line = lines[linei][y:]
        p = 0
        for x in range(y, size * 2 - 1):
            tile = line[p:p + 2];
            p += 2
            if tile[1] == ".":
                inctile = EMPTY
            else:
                inctile = int(tile)
            tiles[inctile] += 1
            # Look for locked tiles    
            if tile[0] == "+":
                print("Adding locked tile: %d at pos %d, %d, id=%d" %
                      (inctile, x, ry, hex.get_by_pos((x, ry)).id))
                done.set_done(hex.get_by_pos((x, ry)).id, inctile)
        linei += 1
    hex.link_nodes()
    done.filter_tiles(tiles)
    return Pos(hex, tiles, done)

def generate_minizinc(filename, outfile):
    pos = read_file(filename)
    check_valid(pos)
    count = pos.hex.count
    sys.stdout.flush()
    with open(outfile, "w") as out:
        print("% Hexiom {}".format(filename), file=out)
        print("include \"global_cardinality.mzn\";", file=out)
        print("array[1..8] of int: VALUES = [ -1, 0, 1, 2, 3, 4, 5, 6 ];", file=out)
        print("set of int: IndexRange = 0..{};".format(count - 1), file=out)
        print("array[IndexRange] of var -1..6: cells;", file=out)
        print("array[1..{}] of set of int: neighbors = [".format(count), file=out)
        for i in range(count):
            cells_around = pos.hex.get_by_id(i).links
            print("  { " + ", ".join(str(k) for k in cells_around) + " }",
                  end=("\n" if (i == count - 1) else ",\n"),
                  file=out)
        print("];", file=out)
        print("array[1..8] of int: counts = [ " +
              ", ".join(str(t) for t in [pos.tiles[EMPTY]] + pos.tiles[0:EMPTY]) +
              " ];",
              file=out)
        print("constraint global_cardinality(cells, VALUES, counts);", file=out)
        print("constraint", file=out)
        print("  forall (i in IndexRange) (", file=out)
        print("    (cells[i] > -1) -> cells[i] = sum([bool2int(cells[j] > -1) | j in neighbors[i + 1]])", file=out)
        print("  );", file=out)
        for i in range(count):
            if pos.done.already_done(i):
                val = pos.done[i][0]
                print("constraint cells[{}] = {};".format(i, val if val != EMPTY else -1), file=out);
        print("solve :: int_search(cells, input_order, indomain_max, complete)", file=out)
        print("  satisfy;", file=out)
        print("output [\"cells\", show(cells)];", file=out)
    return pos

def solve_file(filename):
    mini = minizinc_filename(filename)
    pos = generate_minizinc(filename, mini)
    subprocess.call(["mzn2fzn.bat", mini])
    flat = flatzinc_filename(mini)
    result = change_extension(flat, "res")
    subprocess.call(["fz.exe", "-o", result, flat])
    with open(result, "r") as result_file:
        lines = result_file.readlines()
    result_line = lines[0]
    result_array = result_line[result_line.find("[") + 1:result_line.find("]")]
    result_elems = [int(v) for v in result_array.split(", ")]
    for i in range(pos.hex.count):
        pos.done.set_done(i, result_elems[i])
    print_pos(pos)
    
def change_extension(filename, ext):
    return os.path.splitext(filename)[0] + "." + (ext if not ext.startswith(".") else ext[1:])
    
def minizinc_filename(filename):
    return change_extension(filename, ".mzn")
    
def flatzinc_filename(filename):
    return change_extension(filename, "fzn")
    
def main():
    for f in sys.argv[1:]:
        print(" File : {orig}".format(orig=f))
        solve_file(f)
        print("-------------------")
    
if __name__ == "__main__":
    main()
