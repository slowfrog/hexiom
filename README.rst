This project contains the source code for my Hexiom solver, in Python, Java and C, plus some simple scripts.

Programs
========

Python
------

Just run ``python hexiom.py level01.txt`` to solve one level, or alternatively ``pypy hexiom.py level01.txt``.
If you have pypy in your path, you can also run ``. runpy.sh``. That will run the solver on all forty levels, execpt number 38 and 40.


Java
----

You should compile the source file to ``build/main/java``, or use the included Eclipse project. Then you can run an individual test with ``java -cp build/main/java com.slowflog.hexiom.Main level01.txt``.
You can also run ``. runj.sh`` to solve the 38 solvable levels.


C
-

You can create a MSVC++ solution, for a console application, with only the ``hexiom.c`` source file. On Linux, or with Cygwin, you can simply compile with ``gcc -O3 -Wall -o hexiom hexiom.c``.
You can solve one level with ``hexiom level01.txt`` or solve the 38 good levels with ``. runc.sh``


Data
====

All forty known levels are available in this directory, named levelXX.txt.

The expected results (at least with the current algorithm) are also here, named resultc.txt, resultj.txt and resultpy.txt.

There is also a ``profile.sh`` script that runs the Java version with the JIP profiler.

