#include "Main.h"
#include "Hexiom.h"

#include <iostream>
using namespace std;

#include <gecode/int.hh>
#include <gecode/search.hh>
using namespace Gecode;


void done_init(t_done *self, int count) {
	int i;

	self->done = 0;
	self->count = count;
	for (i = 0; i < count; ++i) {
		self->cells[i] = NONE;
	}
	self->used = 0;
}

void adjust_done(t_done *self) {
	int i;

	for (i = self->done; i < self->count; ++i) {
		if (self->cells[i] == NONE) {
			self->done = i;
			return;
		}
	}
	self->done = -1;
}

void add_done(t_done *self, int i, int v) {
	self->cells[i] = v;
	self->used += 1;
	adjust_done(self);
}

void remove_done(t_done *self, int i) {
	self->cells[i] = NONE;
	self->used -= 1;
	if ((self->done < 0) || (i < self->done)) {
		self->done = i;
	}
}

////////////////////////////////////////
void node_init(t_node *self, int pos, int id, int link_count, int links[]) {
	int i;

	self->pos = pos;
	self->id = id;
	self->link_count = link_count;
	for (i = 0; i < link_count; ++i) {
		self->links[i] = links[i];
	}
}

void append_link(t_node *self, int id) {
	self->links[self->link_count] = id;
	self->link_count += 1;
}


////////////////////////////////////////
void hex_init(t_hex *self, int size) {
	int i, x, y, ry, pos;
	t_node *node;
	int id = 0;

	for (i = 0; i < MAX_CODE; ++i) {
		self->nodes_by_pos[i] = NULL;
	}

	self->size = size;
	self->count = 3 * size * (size - 1) + 1;
	for (y = 0; y < size; ++y) {
		for (x = 0; x < size + y; ++x) {
			pos = make_point(x, y);
			node = &self->nodes_by_id[id];
			node_init(node, pos, id, 0, NULL);
			self->nodes_by_pos[pos] = node;
			id += 1;
		}
	}
	for (y = 1; y < size; ++y) {
		ry = size - 1 + y;
		for (x = y; x < 2 * size - 1; ++x) {
			pos = make_point(x, ry);
			node = &self->nodes_by_id[id];
			node_init(node, pos, id, 0, NULL);
			self->nodes_by_pos[pos] = node;
			id += 1;
		}
	}
}


void link_nodes(t_hex *self) {
	int i, p, d, nx, ny, ncode;

	// TODO rewrite with direct pointer iteration
	for (i = 0; i < self->count; ++i) {
		t_node *node = &self->nodes_by_id[i];
		p = node->pos;
		for (d = 0; d < NB_DIRS; ++d) {
			nx = point_x(p) + DIRS[d][0];
			ny = point_y(p) + DIRS[d][1];
			ncode = make_point(nx, ny);
			if (contains_pos(self, ncode)) {
				append_link(node, self->nodes_by_pos[ncode]->id);
			}
		}
	}
}

////////////////////////////////////////

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
void pos_init(t_pos *self) {
	self->sum_tiles = sum_tiles(&self->tiles);
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
			if (already_done(done, id) && (done->cells[id] != EMPTY)) {
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
			if (already_done(done, id) && (done->cells[id] != EMPTY)) {
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

void print_tiles(t_tiles *tiles) {
	printf("Tiles: 0:%d, 1:%d, 2:%d, 3:%d, 4:%d, 5:%d, 6:%d, 7:%d\n",
		(*tiles)[0], (*tiles)[1], (*tiles)[2], (*tiles)[3], 
		(*tiles)[4], (*tiles)[5], (*tiles)[6], (*tiles)[7]);
}

int check_valid(t_pos *pos) {
	t_hex *hex = &pos->hex;
	t_tiles *tiles = &pos->tiles;
	t_done *done = &pos->done;
	int tot = 0;
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


#define MAX_LINE_SIZE 256

t_pos *read_file(char *file) {
	int size, x, y, ry, p, inctile;
	char line[MAX_LINE_SIZE];
	t_pos *ret = (t_pos *) malloc(sizeof(t_pos));
	t_hex *hex = &ret->hex;
	t_tiles *tiles = &ret->tiles;
	t_done *done = &ret->done;
	FILE *input;

	tiles_init(tiles);


	input = fopen(file, "rb");
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
				inctile = EMPTY;
			} else {
				inctile = tile[1] - '0';
			}
			if (tile[0] == '+') {
				printf("Adding locked tile: %d at pos %d, %d, id=%d\n",
					inctile, x, y, get_by_pos(hex, make_point(x, y))->id);
				add_done(done, get_by_pos(hex, make_point(x, y))->id, inctile);
			}
			(*tiles)[inctile] += 1;
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
				inctile = EMPTY;
			} else {
				inctile = tile[1] - '0';
			}
			if (tile[0] == '+') {
				printf("Adding locked tile: %d at pos %d, %d, id=%d\n",
					inctile, x, ry, get_by_pos(hex, make_point(x, ry))->id);
				add_done(done, get_by_pos(hex, make_point(x, ry))->id, inctile);
			}
			(*tiles)[inctile] += 1;
		}
	}
	fclose(input);

	pos_init(ret);
	link_nodes(hex);
	return ret;
}

int solve(t_pos *pos) {
	if (check_valid(pos)) {
		Hexiom *h = new Hexiom(pos);
		DFS<Hexiom> e(h);
		delete h;

		while (h = e.next())
		{
			//h->print();
			for (int i = 0; i < pos->hex.count; ++i) {
				int v = (*h)[i];
				pos->done.cells[i] = (v >= 0 ? v : EMPTY);
			}
			print_pos(pos);
			delete h;
			return SOLVED;
		}

	}
	cout << "No solution found." << endl;
	return IMPOSSIBLE;
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

Main::Main()
{
}

int	Main::run(int argc, char *argv[]) 
{
	int i;
	for (i = 1; i < argc; ++i) {
		cout << " File : " << argv[i] << endl;
		solve_file(argv[i]);
		cout << "-------------------" << endl;
	}
	return 0;
}

Main::~Main(void)
{
}

extern "C" int main(int argc, char *argv[]) 
{
	Main m;
	return m.run(argc, argv);
}
