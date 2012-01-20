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
typedef struct pos {
  int sum_tiles;
} t_pos;

int solve(t_pos pos) {
  return IMPOSSIBLE;
}

t_pos read_file(char *file) {
  t_pos ret;
  return ret;
}

void solve_file(char *file) {
  t_pos pos = read_file(file);
  solve(pos);
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
