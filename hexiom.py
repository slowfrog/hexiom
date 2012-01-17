from __future__ import division, print_function
import sys

DIRS = [ { "x":  1, "y": 0 },
         { "x": -1, "y": 0 },
         { "x":  0, "y": 1 },
         { "x":  0, "y": -1 },
         { "x":  1, "y":  1 },
         { "x": -1, "y": -1 } ]
         

def make_node(pos, id, links):
    return { "pos":  pos,
             "id":    id,
             "links": links }

def make_hex(size):
    ret = { "size": size }
    id = 0
    for y in xrange(size):
        for x in xrange(size + y):
            pos = (x, y)
            node = make_node(pos, id, [])
            ret[pos] = node
            ret[node["id"]] = node
            id += 1
    for y in xrange(1, size):
        for x in xrange(y, size * 2 - 1):
            ry = size + y - 1
            pos = (x, ry)
            node = make_node(pos, id, [])
            ret[pos] = node
            ret[node["id"]] = node
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
        (x, y) = node["pos"]
        for dir in DIRS:
            nx = x + dir["x"]
            ny = y + dir["y"]
            if (nx, ny) in hex:
                node["links"].append(hex[(nx, ny)]["id"])

def make_pos(hex, tiles):
    return (hex, tiles, [])

def find_moves(pos):
    (hex, tiles, done) = pos
    moves = []
    for i in xrange(-1, 7):
        if tiles[i] > 0:
            moves.append(i)
    return moves

def play_move(pos, move):
    (hex, tiles, done) = pos
    ntiles = dict(tiles)
    ntiles[move] -= 1
    return (hex, ntiles, done + [move])

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
            id = hex[pos]["id"]
            print("%s " % (str(done[id]) if (id < len(done)) and (done[id] >= 0) else "."), end="")
        print()
    for y in xrange(1, size):
        print(" " * y, end="")
        for x in xrange(y, size * 2 - 1):
            ry = size + y - 1
            pos = (x, ry)
            id = hex[pos]["id"]
            print("%s " % (str(done[id]) if (id < len(done)) and (done[id] >= 0) else "."), end="")
        print()
        

def solved(pos, verbose=False):
    (hex, tiles, done) = pos
    if sum(tiles[i] for i in xrange(0, 7)) > 0:
        return False
    for i in xrange(hex["count"]):
        node = hex[i]
        (x, y) = node["pos"]
        num = done[i] if i < len(done) else -1
        if num > 0:
            for dir in DIRS:
                nx = x + dir["x"]
                ny = y + dir["y"]
                npos = (nx, ny)
                if npos in hex:
                    nid = hex[(nx, ny)]["id"]
                    if (nid < len(done)) and (done[nid] >= 0):
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


if __name__ == "__main__":
    main()
