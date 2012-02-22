#define NONE -2
#define OPEN 0
#define SOLVED 1
#define IMPOSSIBLE -1

#define EMPTY 7

////////////////////////////////////////
#define make_point(x, y) (((x & 0xf) << 4) | (y & 0xf))
#define point_x(point) ((point >> 4) & 0xf)
#define point_y(point) (point & 0xf)

////////////////////////////////////////
#define MAX_CELLS (3 * (6 * (6 - 1)) + 1)

typedef struct _done {
	int done;
	int count;
	int cells[MAX_CELLS];
	int used;
} t_done;

#define already_done(self, i) (self->cells[i] != NONE)

#define next_cell(self) (self->done)

#define MAX_LINKS 6

typedef struct node {
	int pos;
	int id;
	int link_count;
	int links[MAX_LINKS];
} t_node;

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

#define contains_pos(self, code) \
	((code >= 0) && (code < MAX_CODE) && (self->nodes_by_pos[code] != NULL))

#define get_by_pos(self, pos) (self->nodes_by_pos[pos])
#define get_by_id(self, id) (self->nodes_by_id + id)

typedef int t_tiles[8];

typedef struct pos {
	t_hex hex;
	t_tiles tiles;
	int sum_tiles;
	t_done done;
} t_pos;

