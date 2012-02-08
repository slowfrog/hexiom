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

EMPTY = 7

##################################
class Done(object):
    def __init__(self, count, empty=False):
        self.count = count
        self.cells = None if empty else [[0, 1, 2, 3, 4, 5, 6, EMPTY] for i in xrange(count)]
        self.used = 0

    def clone(self):
        ret = Done(self.count, True)
        ret.cells = [self.cells[i][:] for i in xrange(self.count)]
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
        for i in xrange(self.count):
            self.remove(i, v)
        
    def remove_unfixed(self, v):
        changed = False
        for i in xrange(self.count):
            if len(self.cells[i]) > 1:
                if self.remove(i, v):
                    changed = True
        return changed
        
    def filter_tiles(self, tiles):
        for v in xrange(8):
            if tiles[v] == 0:
                self.remove_all(v)

    def next_cell(self):
        minlen = 10
        mini = -1
        for i in xrange(len(self.cells)):
            if 1 < len(self.cells[i]) < minlen:
                minlen = len(self.cells[i])
                mini = i
        return mini

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

    def clone(self):
        return Pos(self.hex, self.tiles, self.done.clone())
    
##################################
def constraint_pass(pos, last_move = None):
    changed = False
    left = pos.tiles[:]
    done = pos.done

    # Remove impossible values from free cells
    free_cells = (range(len(done.cells)) if last_move is None
                  else pos.hex.get_by_id(last_move).links)
    for i in free_cells:
        if not done.already_done(i):
            vmax = 0
            vmin = 0
            cells_around = pos.hex.get_by_id(i).links;
            for nid in cells_around:
                if done.already_done(nid):
                    if done[nid][0] != EMPTY:
                        vmin += 1
                        vmax += 1
                else:
                    vmax += 1

            for num in xrange(7):
                if (num < vmin) or (num > vmax):
                    if done.remove(i, num):
                        changed = True
            
    # Computes how many of each value is still free
    for cell in pos.done.cells:
        if len(cell) == 1:
            left[cell[0]] -= 1
            
    for v in xrange(8):
        # If there is none, remove the possibility from all tiles
        if (pos.tiles[v] > 0) and (left[v] == 0):
            if pos.done.remove_unfixed(v):
                changed = True
        else:
            possible = sum((1 if v in cell else 0) for cell in pos.done.cells)
            # If the number of possible cells for a value is exactly the number of available tiles
            # put a tile in each cell
            if pos.tiles[v] == possible:
                for i in xrange(len(pos.done.cells)):
                    cell = pos.done.cells[i]
                    if (len(cell) > 1) and (v in cell):
                        pos.done.set_done(i, v)
                        changed = True
                       
    return changed
        
def find_moves(pos):
    hex = pos.hex
    tiles = pos.tiles
    done = pos.done
    cell_id = done.next_cell()
    if cell_id < 0:
        return []

    return [(cell_id, v) for v in done[cell_id]]

def play_move(pos, move):
    (cell_id, i) = move
    pos.done.set_done(cell_id, i)

def print_pos(pos):
    hex = pos.hex
    done = pos.done
    size = hex.size
    for y in xrange(size):
        print(" " * (size - y - 1), end="")
        for x in xrange(size + y):
            pos2 = (x, y)
            id = hex.get_by_pos(pos2).id
            print("%s " % (str(done[id][0]) if (done.already_done(id) and (done[id][0] != EMPTY)) else "."),
                  end="")
        print()
    for y in xrange(1, size):
        print(" " * y, end="")
        for x in xrange(y, size * 2 - 1):
            ry = size + y - 1
            pos2 = (x, ry)
            id = hex.get_by_pos(pos2).id
            print("%s " % (str(done[id][0]) if (done.already_done(id) and (done[id][0] != EMPTY)) else "."),
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
    all_done = True
    for i in xrange(hex.count):
        if done.already_done(i):
            num = done[i][0]
            vmax = 0
            vmin = 0
            if num != EMPTY:
                cells_around = hex.get_by_id(i).links;
                for nid in cells_around:
                    if done.already_done(nid):
                        if done[nid][0] != EMPTY:
                            vmin += 1
                            vmax += 1
                    else:
                        vmax += 1

                if (num < vmin) or (num > vmax):
                    return IMPOSSIBLE
                if num != vmin:
                    exact = False
        else:
            all_done = False

    if (not all_done) or (not exact):
        return OPEN
    
    print_pos(pos)
    return SOLVED

def solve_step(prev):
    pos = prev
    #pos = prev.clone()
    #while constraint_pass(pos):
    #    pass
    
    moves = find_moves(pos)
    for move in moves:
        ret = OPEN
        new_pos = pos.clone()
        play_move(new_pos, move)
        while constraint_pass(new_pos, move[0]):
            pass
        cur_status = solved(new_pos)
        if cur_status != OPEN:
            ret = cur_status
        else:
            ret = solve_step(new_pos)
        if ret == SOLVED:
            return SOLVED
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
            tiles[inctile] += 1
            # Look for locked tiles    
            if tile[0] == "+":
                print("Adding locked tile: %d at pos %d, %d, id=%d" %
                      (inctile, x, y, hex.get_by_pos((x, y)).id))
                done.set_done(hex.get_by_pos((x, y)).id, inctile)
            
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
