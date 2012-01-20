#include <stdio.h>

#define NONE -2
#define OPEN 0
#define SOLVED 1
#define IMPOSSIBLE -1

////////////////////////////////////////
#define make_point(x, y) (((x & 0xf) << 4) | (y & 0xf))
#define point_x(point) ((point >> 4) & 0xf)
#define point_y(point) (point & 0xf)

////////////////////////////////////////
#define MAX_CELLS (3 * (6 * (6 - 1)) + 1)

typedef struct done {
  int done;
  int count;
  int cells[MAX_CELLS];
  int used;
} t_done;

void done_init(t_done *this, int count) {
  int i;
  
  this->done = 0;
  this->count = count;
  for (i = 0; i < count; ++i) {
    this->cells[i] = NONE;
  }
  this->used = 0;
}

int already_done(t_done *this, int i) {
  return (this->cells[i] != NONE);
}

int next_cell(t_done *this) {
  return this->done;
}

void adjust_done(t_done *this) {
  int i;

  for (i = this->done; i < this->count; ++i) {
    if (this->cells[i] == NONE) {
      this->done = i;
      return;
    }
  }
  this->done = -1;
}

void add_done(t_done *this, int i, int v) {
  this->cells[i] = v;
  this->used += 1;
  adjust_done(this);
}

void remove_done(t_done *this, int i) {
  this->cells[i] = NONE;
  this->used -= 1;
  if ((this->done < 0) || (i < this->done)) {
    this->done = i;
  }
}

////////////////////////////////////////
#define MAX_LINKS 6

typedef struct node {
  int pos;
  int id;
  int link_count;
  int links[MAX_LINKS];
} t_node;

void node_init(t_node *this, int pos, int id, int link_count, int links[]) {
  int i;
  
  this->pos = pos;
  this->id = id;
  this->link_count = link_count;
  for (i = 0; i < link_count; ++i) {
    this->links[i] = links[i];
  }
}

void append_link(t_node *this, int id) {
  this->links[this->link_count] = id;
  this->link_count += 1;
}


////////////////////////////////////////
#define MAX_NODES MAX_CELLS
#define MAX_CODE 256
#define NB_DIRS 6
static int DIRS[NB_DIRS][2] =
  { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { -1, -1 } };

typedef struct hex {
  int size;
  int count;
  t_node nodes_by_id[MAX_NODES];
  t_node *nodes_by_pos[MAX_CODE];
} t_hex;

void hex_init(t_hex *this, int size) {
  int i, x, y, ry, pos;
  int id = 0;

  for (i = 0; i < MAX_CODE; ++i) {
    this->nodes_by_pos[i] = NULL;
  }
  
  this->size = size;
  this->count = 3 * size * (size - 1) + 1;
  for (y = 0; y < size; ++y) {
    for (x = 0; x < size + y; ++x) {
      pos = make_point(x, y);
      t_node *node = &this->nodes_by_id[id];
      node_init(node, pos, id, 0, NULL);
      this->nodes_by_pos[pos] = node;
      id += 1;
    }
  }
  for (y = 1; y < size; ++y) {
    ry = size - 1 + y;
    for (x = y; x < 2 * size - 1; ++x) {
      pos = make_point(x, ry);
      t_node *node = &this->nodes_by_id[id];
      node_init(node, pos, id, 0, NULL);
      this->nodes_by_pos[pos] = node;
      id += 1;
    }
  }
}

int contains_pos(t_hex *this, int code) {
  return (code >= 0) && (code < MAX_CODE) && (this->nodes_by_pos[code] != NULL);
}

void link_nodes(t_hex *this) {
  int i, p, d, nx, ny, ncode;

  // TODO rewrite with direct pointer iteration
  for (i = 0; i < this->count; ++i) {
    t_node *node = &this->nodes_by_id[i];
    p = node->pos;
    for (d = 0; d < NB_DIRS; ++d) {
      nx = point_x(p) + DIRS[d][0];
      ny = point_y(p) + DIRS[d][1];
      ncode = make_point(nx, ny);
      if (contains_pos(this, ncode)) {
        append_link(node, this->nodes_by_pos[ncode]->id);
      }
    }
  }
}

t_node *get_by_pos(t_hex *this, int pos) {
  return this->nodes_by_pos[pos];
}

t_node *get_by_id(t_hex *this, int id) {
  return &this->nodes_by_id[id];
}

////////////////////////////////////////
typedef int t_tiles[8];

void tiles_init(t_tiles *tiles) {
  int i;

  for (i = 0; i < 7; ++i) {
    (*tiles)[i] = 0;
  }
}

int sum_tiles(t_tiles *tiles) {
  int i;
  int sum = 0;

  for (i = 0; i < 7; ++i) {
    sum += (*tiles)[i];
  }
  return sum;
}

////////////////////////////////////////
typedef struct pos {
  t_hex hex;
  t_tiles tiles;
  int sum_tiles;
  t_done done;
} t_pos;

void pos_init(t_pos *this) {
  this->sum_tiles = sum_tiles(&this->tiles);
}

////////////////////////////////////////
int solve(t_pos *pos) {
  return IMPOSSIBLE;
}


void print_pos(t_pos *pos) {
  int i, x, y, ry, pos2, id;
  
  t_hex *hex = &pos->hex;
  t_done *done = &pos->done;
  int size = hex->size;
  for (y = 0; y < size; ++y) {
    for (i = 0; i < size - y - 1; ++i) {
      putchar(' ');
    }
    for (x = 0; x < size + y; ++x) {
      pos2 = make_point(x, y);
      id = get_by_pos(hex, pos2)->id;
      if (already_done(done, id) && (done->cells[id] < 7)) {
        putchar('0' + done->cells[id]);
      } else {
        putchar('.');
      }
      putchar(' ');
    }
    putchar('\n');
  }
  for (y = 1; y < size; ++y) {
    ry = size - 1 + y;
    for (i = 0; i < y; ++i) {
      putchar(' ');
    }
    for (x = y; x < 2 * size - 1; ++x) {
      pos2 = make_point(x, ry);
      id = get_by_pos(hex, pos2)->id;
      if (already_done(done, id) && (done->cells[id] < 7)) {
        putchar('0' + done->cells[id]);
      } else {
        putchar('.');
      }
      putchar(' ');
    }
    putchar('\n');
  }
  fflush(stdout);
}

#define MAX_LINE_SIZE 256

t_pos read_file(char *file) {
  int size, x, y, ry, p, inctile;
  char line[MAX_LINE_SIZE];
  t_pos ret;
  t_hex *hex = &(ret.hex);
  t_done *done = &(ret.done);
  // MAKE TILES

  
  FILE *input = fopen(file, "rb");
  if (input == NULL) {
    // TODO manage error cases
    printf("ERROR OPENING FILE %s!", file);
    return ret;
  }

  fgets(line, MAX_LINE_SIZE, input);
  sscanf(line, "%d", &size);
  hex_init(hex, size);
  done_init(done, hex->count);

  for (y = 0; y < size; ++y) {
    if (fgets(line, MAX_LINE_SIZE, input) == NULL) {
      // FAIL
      printf("ERROR READING LINE %d!", y);
      return ret;
    }
    p = size - 1 - y;
    for (x = 0; x < size + y; ++x) {
      char *tile = line + p;
      p += 2;
      inctile = 0;
      if (tile[1] == '.') {
        inctile = 7;
      } else {
        inctile = tile[1] - '0';
      }
      if (tile[0] == '+') {
        printf("Adding locked tile: %d at pos %d, %d, id=%d\n",
               inctile, x, y, get_by_pos(hex, make_point(x, y))->id);
        add_done(done, get_by_pos(hex, make_point(x, y))->id, inctile);
      }
    }
  }
  for (y = 1; y < size; ++y) {
    ry = size - 1 + y;
    if (fgets(line, MAX_LINE_SIZE, input) == NULL) {
      // FAIL
      printf("ERROR READING LINE %d!", ry);
      return ret;
    }
    p = y;
    for (x = y; x < 2 * size - 1; ++x) {
      char *tile = line + p;
      p += 2;
      inctile = 0;
      if (tile[1] == '.') {
        inctile = 7;
      } else {
        inctile = tile[1] - '0';
      }
      if (tile[0] == '+') {
        printf("Adding locked tile: %d at pos %d, %d, id=%d\n",
               inctile, x, ry, get_by_pos(hex, make_point(x, ry))->id);
        add_done(done, get_by_pos(hex, make_point(x, ry))->id, inctile);
      }
    }
  }
  fclose(input);
  
  link_nodes(hex);
  return ret;
}

void solve_file(char *file) {
  t_pos pos = read_file(file);
  print_pos(&pos);
  solve(&pos);
}

int main(int argc, char *argv[]) {
  int i;
  for (i = 1; i < argc; ++i) {
    printf(" File : %s\n", argv[i]);
    solve_file(argv[i]);
    printf("-------------------\n");
  }
  return 0;
}
