from __future__ import division, print_function
import sys

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

##################################
class Done(object):
    def __init__(self, count):
        self.count = count
        self.done = 0
        self.cells = count * [None]
        self.used = 0

    def already_done(self, i):
        return self.cells[i] is not None

    def next_cell(self):
        return self.done

    def __getitem__(self, i):
        return self.cells[i]

    def adjust_done(self):
        for i in xrange(self.done, self.count):
            if self.cells[i] is None:
                self.done = i
                return
        else:
            self.done = -1
    
    def add_done(self, i, v):
        self.cells[i] = v
        self.used += 1
        self.adjust_done()

    def remove_done(self, i):
        self.cells[i] = None
        self.used -= 1
        if (self.done < 0) or (i < self.done):
            self.done = i


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
        for y in xrange(size):
            for x in xrange(size + y):
                pos = (x, y)
                node = Node(pos, id, [])
                self.nodes_by_pos[pos] = node
                self.nodes_by_id[node.id] = node
                id += 1
        for y in xrange(1, size):
            for x in xrange(y, size * 2 - 1):
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
    
##################################
EMPTY = 7

def find_moves(pos):
    hex = pos.hex
    tiles = pos.tiles
    done = pos.done
    cell_id = done.next_cell()
    if cell_id < 0:
        return []
    
    moves = []
    cells_around = hex.get_by_id(cell_id).links
    max_possible = len(cells_around)
    min_possible = 0
    for j in cells_around:
        if done.already_done(j):
            dj = done[j]
            if (dj > 0) and (dj != EMPTY):
                min_possible += 1
            elif dj == 0:
                max_possible = 0
                min_possible += 1
            elif dj == EMPTY:
                max_possible -= 1
                
    for i in xrange(8):
        if tiles[i] > 0:
            if (i == EMPTY) or (min_possible <= i <= max_possible):
                moves.append((cell_id, i))
    return moves

def play_move(pos, move):
    (cell_id, i) = move
    pos.tiles[i] -= 1
    pos.done.add_done(cell_id, i)

def undo_move(pos, move):
    (cell_id, i) = move
    pos.tiles[i] += 1
    pos.done.remove_done(cell_id)

def print_pos(pos):
    hex = pos.hex
    done = pos.done
    size = hex.size
    for y in xrange(size):
        print(" " * (size - y - 1), end="")
        for x in xrange(size + y):
            pos2 = (x, y)
            id = hex.get_by_pos(pos2).id
            print("%s " % (str(done[id]) if (done.already_done(id) and (done[id] != EMPTY)) else "."),
                  end="")
        print()
    for y in xrange(1, size):
        print(" " * y, end="")
        for x in xrange(y, size * 2 - 1):
            ry = size + y - 1
            pos2 = (x, ry)
            id = hex.get_by_pos(pos2).id
            print("%s " % (str(done[id]) if (done.already_done(id) and (done[id] != EMPTY)) else "."),
                  end="")
        print()

OPEN = 0
SOLVED = 1
IMPOSSIBLE = -1
        
def solved(pos, verbose=False):
    hex = pos.hex
    tiles = pos.tiles
    done = pos.done
    exact = True
    for i in xrange(hex.count):
        if done.already_done(i):
            num = done[i]
            vmax = 0
            vmin = 0
            if num != EMPTY:
                cells_around = hex.get_by_id(i).links;
                for nid in cells_around:
                    if done.already_done(nid):
                        if done[nid] != EMPTY:
                            vmin += 1
                            vmax += 1
                    else:
                        vmax += 1

                if (num < vmin) or (num > vmax):
                    return IMPOSSIBLE
                if num != vmin:
                    exact = False

    if (sum(tiles[i] for i in xrange(7)) > 0) or not exact:
        return OPEN
    
    print_pos(pos)
    return SOLVED

def solve_step(pos):
    moves = find_moves(pos)
    for move in moves:
        ret = OPEN
        play_move(pos, move)
        cur_status = solved(pos)
        if cur_status != OPEN:
            ret = cur_status
        elif solve_step(pos) == SOLVED:
            ret = SOLVED
        undo_move(pos, move)
        if ret == SOLVED:
            return ret
    return IMPOSSIBLE

def check_valid(pos):
    hex = pos.hex
    tiles = pos.tiles
    done = pos.done
    # fill missing entries in tiles
    tot = done.used
    for i in xrange(8):
        if tiles[i] > 0:
            tot += tiles[i]
        else:
            tiles[i] = 0
    # check total
    if tot != hex.count:
        raise Exception("Invalid input. Expected %d tiles, got %d." % (hex.count, tot))

def solve(pos):
    check_valid(pos)
    return solve_step(pos)


# TODO Write an 'iterator' to go over all x,y positions

def read_file(file):
    with open(file, "rb") as input:
        lines = [line.strip("\r\n") for line in input]
    size = int(lines[0])
    hex = Hex(size)
    linei = 1
    tiles = 8 * [0]
    done = Done(hex.count)
    for y in xrange(size):
        line = lines[linei][size - y - 1:]
        p = 0
        for x in xrange(size + y):
            tile = line[p:p + 2];
            p += 2
            if tile[1] == ".":
                inctile = EMPTY
            else:
                inctile = int(tile)
            # Look for locked tiles    
            if tile[0] == "+":
                print("Adding locked tile: %d at pos %d, %d, id=%d" %
                      (inctile, x, y, hex.get_by_pos((x, y)).id))
                done.add_done(hex.get_by_pos((x, y)).id, inctile)
            else:
                tiles[inctile] += 1
        linei += 1
    for y in xrange(1, size):
        ry = size - 1 + y
        line = lines[linei][y:]
        p = 0
        for x in xrange(y, size * 2 - 1):
            tile = line[p:p + 2];
            p += 2
            if tile[1] == ".":
                inctile = EMPTY
            else:
                inctile = int(tile)
            # Look for locked tiles    
            if tile[0] == "+":
                print("Adding locked tile: %d at pos %d, %d, id=%d" %
                      (inctile, x, ry, hex.get_by_pos((x, ry)).id))
                done.add_done(hex.get_by_pos((x, ry)).id, inctile)
            else:
                tiles[inctile] += 1
        linei += 1
    hex.link_nodes()
    return Pos(hex, tiles, done)

def solve_file(file):
    pos = read_file(file)
    sys.stdout.flush()
    solve(pos)
    sys.stdout.flush()

def main():
    for f in sys.argv[1:]:
        print(" File : %s" % f)
        solve_file(f)
        print("-------------------")
    
if __name__ == "__main__":
    main()
