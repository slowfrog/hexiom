#pragma once

#include <gecode/int.hh>
using namespace Gecode;

#include "Types.h"

class Hexiom :
	public Space
{
protected:
	int count;
	IntVarArray cells;
public:
	Hexiom(t_pos *pos);
	Hexiom(bool share, Hexiom &s);
	~Hexiom(void);
	virtual Space *copy(bool share);
	void print(void) const;
	int operator[](int) const;
};

