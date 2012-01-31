#include <stdio.h>
#include <stdlib.h>

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

#define already_done(this, i) (this->cells[i] != NONE)
//int already_done(t_done *this, int i) {
//  return (this->cells[i] != NONE);
//}

#define next_cell(this) (this->done)
//int next_cell(t_done *this) {
//  return this->done;
//}

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

#define contains_pos(this, code) \
  ((code >= 0) && (code < MAX_CODE) && (this->nodes_by_pos[code] != NULL))
//int contains_pos(t_hex *this, int code) {
//  return (code >= 0) && (code < MAX_CODE) && (this->nodes_by_pos[code] != NULL);
//}

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

#define get_by_pos(this, pos) (this->nodes_by_pos[pos])
//t_node *get_by_pos(t_hex *this, int pos) {
//  return this->nodes_by_pos[pos];
//}

#define get_by_id(this, id) (this->nodes_by_id + id)
//t_node *get_by_id(t_hex *this, int id) {
//  return &this->nodes_by_id[id];
//}

////////////////////////////////////////
typedef int t_tiles[8];

void tiles_init(t_tiles *tiles) {
  int i;

  for (i = 0; i < 8; ++i) {
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
int find_moves(t_pos *pos, int *moves) {
  int count = 0;
  int index = 0;
  t_hex *hex = &pos->hex;
  t_tiles *tiles = &pos->tiles;
  t_done *done = &pos->done;
  int cell_id = next_cell(done);
  int cells_count = -1;
  int *cells_around = NULL;
  int min_possible = 0;
  int max_possible = 0;
  int i, j, ca, dj, valid;
  
  if (cell_id < 0) {
    return count;
  }
  
  for (i = 0; i < 8; ++i) {
    if ((*tiles)[i] > 0) {
      valid = 1;
      if (i < 7) {
        if (cells_around == NULL) {
          cells_around = get_by_id(hex, cell_id)->links;
          cells_count = get_by_id(hex, cell_id)->link_count;
          max_possible = cells_count;
          for (ca = 0; ca < cells_count; ++ca) {
            j = cells_around[ca];
            if (already_done(done, j)) {
              dj = done->cells[j];
              if ((dj > 0) && (dj < 7)) {
                min_possible += 1;
              } else if (dj == 0) {
                max_possible = 0;
                min_possible += 1;
              }
            }
          }
        }

        valid = (min_possible <= i) && (i <= max_possible);
      }
      if (valid) {
        moves[index] = cell_id;
        moves[index + 1] = i;
        count += 1;
        index += 2;
      }
    }
  }
  return count;
}

void play_move(t_pos *pos, int cell_id, int value) {
  pos->tiles[value] -= 1;
  if (value < 7) {
    pos->sum_tiles -= 1;
  }
  add_done(&pos->done, cell_id, value);
}

void undo_move(t_pos *pos, int cell_id, int value) {
  pos->tiles[value] += 1;
  if (value < 7) {
    pos->sum_tiles += 1;
  }
  remove_done(&pos->done, cell_id);
}

void print_tiles(t_tiles *tiles) {
  printf("Tiles: -1:%d, 0:%d, 1:%d, 2:%d, 3:%d, 4:%d, 5:%d, 6:%d\n",
         (*tiles)[7], (*tiles)[0], (*tiles)[1], (*tiles)[2],
         (*tiles)[3], (*tiles)[4], (*tiles)[5], (*tiles)[6]);
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

  //print_tiles(&pos->tiles);
  //printf("Sum tiles: %d\n", pos->sum_tiles);
}

int solved(t_pos *pos) {
  t_hex *hex = &pos->hex;
  t_done *done = &pos->done;
  int exact = 1;
  int i, num, min, max, d, nid;
  int cells_count;
  int *cells_around;

  for (i = 0; i < hex->count; ++i) {
    if (already_done(done, i)) {
      num = done->cells[i];
      max = 0;
      min = 0;
      if (num < 7) {
        cells_around = get_by_id(hex, i)->links;
        cells_count = get_by_id(hex, i)->link_count;
        for (d = 0; d < cells_count; ++d) {
          nid = cells_around[d];
          if (already_done(done, nid)) {
            if (done->cells[nid] < 7) {
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
          exact = 0;
        }
      }
    }
  }
  
  if ((pos->sum_tiles > 0) || !exact) {
    return OPEN;
  }
  
  print_pos(pos);
  return SOLVED;
}

void print_moves(int count, int *moves) {
  int i;
  printf("Moves: %d\n", count);
  for (i = 0; i < count; ++i) {
    printf("%d->%d\n", moves[2 * i], moves[2 * i + 1]);
  }
  printf("=====\n");
}

#define MAX_MOVES 8
int solve_step(t_pos *pos) {
  int moves[MAX_MOVES * 2];
  int count = find_moves(pos, moves);
  int i, cell_id, value, ret, cur_status;
  //print_moves(count, moves);
  for (i = 0; i < count; ++i) {
    cell_id = moves[2 * i];
    value = moves[2 * i + 1];
    ret = OPEN;
    play_move(pos, cell_id, value);
    cur_status = solved(pos);
    if (cur_status != OPEN) {
      ret = cur_status;
    } else if (solve_step(pos) == SOLVED) {
      ret = SOLVED;
    }
    undo_move(pos, cell_id, value);
    if (ret == SOLVED) {
      return ret;
    }
  }
  return IMPOSSIBLE;
}

int check_valid(t_pos *pos) {
  t_hex *hex = &pos->hex;
  t_tiles *tiles = &pos->tiles;
  t_done *done = &pos->done;
  int tot = done->used;
  int i;
  for (i = 0; i < 8; ++i) {
    if ((*tiles)[i] > 0) {
      tot += (*tiles)[i];
    } else {
      (*tiles)[i] = 0;
    }
  }
  if (tot != hex->count) {
    printf("Invalid input. Expected %d tiles, got %d.\n", hex->count, tot);
    print_tiles(tiles);
    return 0;
  }
  return 1;
}

int solve(t_pos *pos) {
  if (check_valid(pos)) {
    return solve_step(pos);
  } else {
    return IMPOSSIBLE;
  }
}


#define MAX_LINE_SIZE 256

t_pos *read_file(char *file) {
  int size, x, y, ry, p, inctile;
  char line[MAX_LINE_SIZE];
  t_pos *ret = (t_pos *) malloc(sizeof(t_pos));
  t_hex *hex = &ret->hex;
  t_tiles *tiles = &ret->tiles;
  t_done *done = &ret->done;

  tiles_init(tiles);
  
  
  FILE *input = fopen(file, "rb");
  if (input == NULL) {
    free(ret);
    return NULL;
  }

  fgets(line, MAX_LINE_SIZE, input);
  sscanf(line, "%d", &size);
  hex_init(hex, size);
  done_init(done, hex->count);

  for (y = 0; y < size; ++y) {
    if (fgets(line, MAX_LINE_SIZE, input) == NULL) {
      printf("ERROR READING LINE %d!\n", y);
      free(ret);
      return NULL;
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
      } else {
        (*tiles)[inctile] += 1;
      }
    }
  }
  for (y = 1; y < size; ++y) {
    ry = size - 1 + y;
    if (fgets(line, MAX_LINE_SIZE, input) == NULL) {
      printf("ERROR READING LINE %d!\n", ry);
      free(ret);
      return NULL;
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
      } else {
        (*tiles)[inctile] += 1;
      }
    }
  }
  fclose(input);

  pos_init(ret);
  link_nodes(hex);
  return ret;
}

void solve_file(char *file) {
  t_pos *pos = read_file(file);
  if (pos == NULL) {
    printf("Cannot solve %s, error reading\n", file);
    return;
  }
  fflush(stdout);
  //print_pos(pos);
  //printf("++++++++++++++++\n");
  solve(pos);
  free(pos);
  fflush(stdout);
}

int main(int argc, char *argv[]) {
  int i;
  for (i = 1; i < argc; ++i) {
    printf(" File : %s\n", argv[i]);
    fflush(stdout);
    solve_file(argv[i]);
    printf("-------------------\n");
    fflush(stdout);
  }
  return 0;
}
