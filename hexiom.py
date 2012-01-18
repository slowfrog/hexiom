from __future__ import division, print_function
import sys

DIRS = [ { "x":  1, "y": 0 },
         { "x": -1, "y": 0 },
         { "x":  0, "y": 1 },
         { "x":  0, "y": -1 },
         { "x":  1, "y":  1 },
         { "x": -1, "y": -1 } ]

##################################
class Done(object):
    def __init__(self, count):
        self.count = count
        self.done = 0
        self.cells = count * [None]

    def already_done(self, i):
        return self.cells[i] is not None

    def next_cell(self):
        for i in xrange(self.done, self.count):
            if self.cells[i] is None:
                self.done = i
                return i
        return -1

    def __getitem__(self, i):
        return self.cells[i]

    def add_done(self, i, v):
        ret = Done(self.count)
        ret.done = self.done
        ret.cells = self.cells[:]
        ret.cells[i] = v
        if i == ret.done + 1:
            ret.done += 1
        return ret

##################################
# class Hex:
#     def __init__(self, size):
#         self.size = size
#         self.nodes = {}
#         id = 0
#         for y in xrange(size):
#             for x in xrange(size + y):
#                 pos = (x, y)
#                 node = make_node(pos, id, [])
#                 self.nodes[pos] = node
#                 self.nodes[node["id"]] = node
#                 id += 1
#         for y in xrange(1, size):
#             for x in xrange(y, size * 2 - 1):
#                 ry = size + y - 1
#                 pos = (x, ry)
#                 node = make_node(pos, id, [])
#                 self.nodes[pos] = node
#                 self.nodes[node["id"]] = node
#                 id += 1
#         self.count = id

##################################
class Node(object):
    def __init__(self, pos, id, links):
        self.pos = pos
        self.id = id
        self.links = links

        
def make_hex(size):
    ret = { "size": size }
    id = 0
    for y in xrange(size):
        for x in xrange(size + y):
            pos = (x, y)
            node = Node(pos, id, [])
            ret[pos] = node
            ret[node.id] = node
            id += 1
    for y in xrange(1, size):
        for x in xrange(y, size * 2 - 1):
            ry = size + y - 1
            pos = (x, ry)
            node = Node(pos, id, [])
            ret[pos] = node
            ret[node.id] = node
            id += 1
    ret["count"] = id
    return ret

def make_linked_hex(size):
    ret = make_hex(size)
    link_nodes(ret)
    return ret

def link_nodes(hex):
    for i in xrange(hex["count"]):
        node = hex[i]
        (x, y) = node.pos
        for dir in DIRS:
            nx = x + dir["x"]
            ny = y + dir["y"]
            if (nx, ny) in hex:
                node.links.append(hex[(nx, ny)].id)

def make_pos(hex, tiles):
    return (hex, tiles, Done(hex["count"]))

def find_moves(pos):
    (hex, tiles, done) = pos
    cell_id = done.next_cell()
    if cell_id < 0:
        return []
    
    moves = []
    for i in xrange(-1, 7):
        if tiles[i] > 0:
            valid = True
            if i >= 0:
                valid = False
                cells_around = hex[cell_id].links
                min_possible = sum(1 if (done.already_done(j) and (done[j] >= 0)) else 0
                                   for j in cells_around)
                if i >= min_possible:
                    max_possible = len(cells_around)
                    if i <= max_possible:
                        valid = True
                    #else:
                    #    print("Max possible at %d is %d" % (cell_id, max_possible))
                #else:
                #    print("Min possible at %d is %d" % (cell_id, min_possible))
            if valid:
                moves.append((cell_id, i))
    return moves

def play_move(pos, move):
    (hex, tiles, done) = pos
    ntiles = dict(tiles)
    (j, v) = move
    ntiles[v] -= 1
    return (hex, ntiles, done.add_done(j, v))

def play_moves(pos, moves):
    ret = pos
    for move in moves:
        ret = play_move(ret, move)
    return ret

def solve_step(pos):
    moves = find_moves(pos)
    for move in moves:
        next = play_move(pos, move)
        if solved(next):
            return True
        if solve_step(next):
            return True
    return False

def check_valid(hex, tiles):
    # fill missing entries in tiles
    tot = 0
    for i in xrange(-1, 7):
        if i in tiles:
            tot += tiles[i]
        else:
            tiles[i] = 0
    # check total
    if tot != hex["count"]:
        raise Exception("Invalid input. Expected %d tiles, got %d." % (hex["count"], tot))

def solve(hex, tiles):
    check_valid(hex, tiles)
    return solve_step(make_pos(hex, tiles))

def print_pos(pos):
    (hex, tiles, done) = pos
    size = hex["size"]
    for y in xrange(size):
        print(" " * (size - y - 1), end="")
        for x in xrange(size + y):
            pos = (x, y)
            id = hex[pos].id
            print("%s " % (str(done[id]) if (done.already_done(id) and (done[id] >= 0)) else "."),
                  end="")
        print()
    for y in xrange(1, size):
        print(" " * y, end="")
        for x in xrange(y, size * 2 - 1):
            ry = size + y - 1
            pos = (x, ry)
            id = hex[pos].id
            print("%s " % (str(done[id]) if (done.already_done(id) and (done[id] >= 0)) else "."),
                  end="")
        print()
        

def solved(pos, verbose=False):
    (hex, tiles, done) = pos
    if sum(tiles[i] for i in xrange(0, 7)) > 0:
        return False
    for i in xrange(hex["count"]):
        node = hex[i]
        (x, y) = node.pos
        num = done[i] if done.already_done(i) else -1
        if num > 0:
            for dir in DIRS:
                nx = x + dir["x"]
                ny = y + dir["y"]
                npos = (nx, ny)
                if npos in hex:
                    nid = hex[(nx, ny)].id
                    if done.already_done(nid) and (done[nid] >= 0):
                        num -= 1
            if num != 0:
                if verbose:
                    print("At pos %d,%d: %s, expected %d but was %d" %
                          (x, y,
                           "too many links" if num < 0 else "missing links",
                           done[i], num - done[i]))
                return False
    print_pos(pos)
    return True

def read_file(file):
    with open(file, "rb") as input:
        lines = [line.strip("\r\n") for line in input]
    size = int(lines[0])
    hex = make_hex(size)
    linei = 1
    tiles = { -1: 0, 0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 6: 0 }
    for y in xrange(size):
        line = lines[linei][size - y - 1:]
        p = 0
        for x in xrange(size + y):
            tile = line[p:p + 2];
            p += 2
            if tile == " .":
                inctile = -1
            else:
                inctile = int(tile)
            tiles[inctile] += 1
        linei += 1
    for y in xrange(1, size):
        ry = size - 1 + y
        line = lines[linei][y:]
        p = 0
        for x in xrange(y, size * 2 - 1):
            tile = line[p:p + 2];
            p += 2
            if tile == " .":
                inctile = -1
            else:
                inctile = int(tile)
            tiles[inctile] += 1
        linei += 1
    link_nodes(hex)
    return (hex, tiles, [])

def solve_file(file):
    (hex, tiles, done) = read_file(file)
    solve(hex, tiles)

def main():
    for f in sys.argv[1:]:
        print(" File : %s" % f)
        solve_file(f)
        print("-------------------")
    
if __name__ == "__main__":
    main()
