import random
from com.slowfrog.hexiom import Main2

# Game logic
ORIG_X = 200
ORIG_Y = [None, None, 144, 87, 59, 54, 51]
HEX_DX = [None, None, 48, 48, 40, 31, 25]
HEX_DY = [None, None, 28, 28, 23, 18, 15]

def get_hex_pos(size, x, y):
    x0 = ORIG_X
    y0 = ORIG_Y[size]
    hx = x0 + HEX_DX[size] * (x - y)
    hy = y0 + HEX_DY[size] * (x + y)
    return (hx, hy)

def get_nearest_xy(size, hx, hy):
    x0 = ORIG_X
    y0 = ORIG_Y[size]
    diff_xy = int(round(float(hx - x0) / HEX_DX[size]))
    sum_xy = int(round(float(hy - y0) / HEX_DY[size]))
    return ((sum_xy + diff_xy) / 2, (sum_xy - diff_xy) / 2)

# Game graphics
PLAY = Pattern("play.png").similar(0.85)
EMPTY4 = Pattern("empty4.png").similar(0.75)
ZERO4 = Pattern("zero4.png").similar(0.95)
ONE4 = Pattern("one4.png").similar(0.85)
TWO4 = Pattern("two4.png").similar(0.85)
THREE4 = Pattern("three4.png").similar(0.85)
FOUR4 = Pattern("four4.png").similar(0.85)
FIVE4 = Pattern("five4.png").similar(0.85)
SIX4 = Pattern("six4.png").similar(0.95)
CLAW4 = Pattern("claw4.png").similar(0.85).targetOffset(0, 18)
CLAW4RED = Pattern("claw4red.png").similar(0.85).targetOffset(0, 18)
CLAW4YELLOW = Pattern("claw4yellow.png").similar(0.85).targetOffset(0, 18)

RANDOM_X = [None, None, None, 126, 176, 225, 275]


def new_browser_tab(browser_name, url):
    browser = App(browser_name)
    reg = browser.window()
    browser.focus()
    type("t", KeyModifier.CTRL)
    paste(url)
    type(Key.ENTER)
    return reg

def new_firefox_tab(url):
    return new_browser_tab("Firefox", url)

def new_game_tab():
    return new_firefox_tab("http://www.kongregate.com/games/Moonkey/hexiom")

NONE = -1
EMPTY = 7

def cleanup(cell):
    if cell == "":
        return "+."
    ret = "+" if "+" in cell else " "
    for i in cell:
        if i in ".0123456":
            ret += i
    ret = (ret + "?")[:2]
    return ret

def find_next(pb, locked, fromi, val):
    for i in xrange(fromi, pb.count):
        if (pb.getVal(i) == val) and not locked[i]:
            return i
    raise Exception("Not found: %d" % val)

class Context(object):
    def start_game(self):
        loops_left = 10
        self.reg = new_game_tab()
        match = None
        while loops_left > 0:
            loops_left -= 1
            try:
                match = self.reg.find(PLAY)
                print("Match %s" % match)
                break
            except FindFailed:
                print("No match")
                sleep(3)
        
        if match is None:
            return
    
        self.goff = Location(match.x - 170, match.y - 270)
        self.reg.click(Location(self.goff.x + 200, self.goff.y + 285))
        sleep(6)

    def start_random_game(self, size):
        self.size = size
        random_loc = Location(self.goff.x + 200, self.goff.y + 328)
        self.reg.click(random_loc)
        sleep(0.5)
        self.reg.click(Location(self.goff.x + RANDOM_X[size], self.goff.y + 195))


    def print_board(self, board, filename):
        count = 3 * self.size * (self.size - 1) + 1
        ret = Main2.Done(count, False)
        locked = [False] * count
        with open(filename, "wb") as out:
            size = self.size
            print >> out, size
            i = 0
            for y in xrange(size):
                line = " " * (size - y - 1)
                for x in xrange(size + y):
                    val = cleanup(board[y][x])
                    line += "%s" % val
                    if val[1] == ".":
                        ret.setDone(i, EMPTY)
                    else:
                        ret.setDone(i, int(val[1]))
                    locked[i] = (val[0] == "+")
                    i += 1
                print >> out, line
            for y in xrange(1, size):
                ry = size + y - 1
                line = " " * y
                for x in xrange(y, size * 2 - 1):
                    val = cleanup(board[ry][x])
                    line += "%s" % val
                    if val[1] == ".":
                        ret.setDone(i, EMPTY)
                    else:
                        ret.setDone(i, int(val[1]))
                    locked[i] = (val[0] == "+")
                    i += 1
                print >> out, line
        return (ret, locked)

    def drag_drop(self, x0, y0, x1, y1):
        (hx0, hy0) = get_hex_pos(self.size, x0, y0)
        (hx1, hy1) = get_hex_pos(self.size, x1, y1)
        self.reg.dragDrop(Location(self.goff.x + hx0, self.goff.y + hy0),
                          Location(self.goff.x + hx1, self.goff.y + hy1))
        
        
                
    def find_elements(self):
        board = [[""] * (2 * self.size - 1) for y in xrange(2 * self.size - 1)]
        key_pats = { "0": ZERO4, 
                     "1": ONE4,
                     "2": TWO4, 
                     "3": THREE4, 
                     "4": FOUR4, 
                     "5": FIVE4, 
                     "6": SIX4, 
                     ".": EMPTY4, 
                     "+": [ CLAW4, CLAW4RED, CLAW4YELLOW ]
               }
        for key in key_pats:
            pats = key_pats[key]
            if not isinstance(pats, list):
                pats = [pats]
            for pat in pats:  
                print("Looking for %s" % key)
                try:
                    matches = list(self.reg.findAll(pat))
                    for match in matches:
                        if match.getScore() > 0.8:
                            loc = match.getTarget()
                            (x, y) = get_nearest_xy(self.size, loc.x - self.goff.x, loc.y - self.goff.y)
                            print("[%d, %d]=>(%d, %d)->%s" % (loc.x, loc.y, x, y, key))
                            board[y][x] += key
                            #match.highlight()        
                    #sleep(2)
                    #for match in matches:
                    #    if match.getScore() > 0.8:
                    #        match.highlight()        
                except FindFailed:
                    pass
        (pb, locked) = self.print_board(board, "input.txt")
        Main2.main(["-sfirst", "input.txt"])
        sol = Main2.solution;
        
        i = 0
        size = self.size
        i_to_xy = {}
        xy_to_i = {}
        for y in xrange(size):
            for x in xrange(size + y):
                i_to_xy[i] = (x, y)
                xy_to_i[(x, y)] = i
                i += 1
        for y in xrange(1, size):
            ry = size + y - 1
            for x in xrange(y, size * 2 - 1):
                i_to_xy[i] = (x, ry)
                xy_to_i[(x, ry)] = i
                i += 1

        i = 0
        for y in xrange(size):
            for x in xrange(size + y):
                v = sol.getVal(i)
                if pb.getVal(i) != v:
                    j = find_next(pb, locked, i + 1, v)
                    (x1, y1) = i_to_xy[j]
                    if v == EMPTY:
                        self.drag_drop(x, y, x1, y1)
                    else:
                        self.drag_drop(x1, y1, x, y)
                    pb.setDone(j, pb.getVal(i))
                    pb.setDone(i, v)
                i += 1
        for y in xrange(1, size):
            ry = size + y - 1
            for x in xrange(y, size * 2 - 1):
                v = sol.getVal(i)
                if pb.getVal(i) != v:
                    j = find_next(pb, locked, i + 1, v)
                    (x1, y1) = i_to_xy[j]
                    if v == EMPTY:
                        self.drag_drop(x, ry, x1, y1)
                    else:
                        self.drag_drop(x1, y1, x, ry)
                    pb.setDone(j, pb.getVal(i))
                    pb.setDone(i, v)
                i += 1


def main():
    Settings.ActionLogs = False
    Settings.MoveMouseDelay = 0
    Settings.DelayAfterDrag = 0
    Settings.DelayBeforfeDrop = 0
    ctx = Context()
    ctx.start_game()
    ctx.start_random_game(4)
    ctx.find_elements()
    sleep(1)

main()
