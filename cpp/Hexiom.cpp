#include "Hexiom.h"

#include <iostream>
using namespace std;

#define GECODE_EMPTY -1

Hexiom::Hexiom(t_pos *pos) :
	count(pos->hex.count),
	cells(*this, count, -1, 6)
{
	// Global cardinality;
	IntVarArgs counts(*this, 8, 0, count);
	IntArgs values(8, 0, 1, 2, 3, 4, 5, 6, GECODE_EMPTY);
	for (int v = 0; v < 8; ++v) 
	{
		rel(*this, counts[v], IRT_EQ, pos->tiles[v]);
	}
	Gecode::count(*this, cells, counts, values);

	// Auxiliary variables
	BoolVarArray used(*this, count, 0, 1);
    IntVarArray usedInt(*this, count, 0, 1);
	for (int i = 0; i < count; ++i)
	{
		rel(*this, cells[i], IRT_NQ, GECODE_EMPTY, used[i]);
		rel(*this, usedInt[i], IRT_EQ, 1, used[i]);
	}

	IntVarArray sums(*this, count, 0, 6);
	BoolVarArray goodSums(*this, count, 0, 1);
	for (int i = 0; i < count; ++i)
	{
		t_node *node = &pos->hex.nodes_by_id[i];
		int lc = node->link_count;
		IntVarArgs neighbors(lc);
		for (int j = 0; j < lc; ++j)
		{
			neighbors[j] = usedInt[node->links[j]];
		}
		Gecode::count(*this, neighbors, 1, IRT_EQ, sums[i]);
		rel(*this, sums[i], IRT_EQ, cells[i], goodSums[i]);
	}	

	for (int i = 0; i < count; ++i)
	{
		rel(*this, used[i], BOT_IMP, goodSums[i], 1);
	}

	// Fixed tiles
	for (int i = 0; i < count; ++i) 
	{
		if (pos->done.cells[i] != NONE) 
		{
			rel(*this, cells[i], IRT_EQ, pos->done.cells[i]);
		}
	}

	// Branching
	branch(*this, cells, INT_VAR_SIZE_MIN, INT_VAL_MAX);
}

Hexiom::Hexiom(bool share, Hexiom &s) :
	Space(share, s),
	count(s.count)
{
	cells.update(*this, share, s.cells);
}

Space *
Hexiom::copy(bool share)
{
	return new Hexiom(share, *this);
}

Hexiom::~Hexiom(void)
{
}

void
Hexiom::print(void) const 
{
	cout << cells << endl;
}

int
Hexiom::operator[](int i) const
{
	return cells[i].min();
}