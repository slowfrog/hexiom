This project contains the source code for my Hexiom solver, in Python, Java and C, plus some simple scripts.

Search Programs
===============

Those are the ones using the search algorithm described in http://slowfrog.blogspot.com/2012/01/solving-hexiom-perhaps-you-can-help.html

Python
------

Just run ``python hexiom.py level01.txt`` to solve one level, or alternatively ``pypy hexiom.py level01.txt``.
If you have pypy in your path, you can also run ``. runpy.sh``. That will run the solver on all forty levels, except number 38 and 40.


Java
----

You should compile the source file to ``build/main/classes``, or use the included Eclipse project. Then you can run an individual test with ``java -cp build/main/classes com.slowfrog.hexiom.Main level01.txt``.
You can also run ``. runj.sh`` to solve the 38 solvable levels.


C
-

You can create a MSVC++ solution, for a console application, with only the ``hexiom.c`` source file. On Linux, or with Cygwin, you can simply compile with ``gcc -O3 -Wall -o hexiom hexiom.c``.
You can solve one level with ``hexiom level01.txt`` or solve the 38 good levels with ``. runc.sh``


Constraint Solving Programs
===========================

Those are the ones using the constraint solving plus search algorithm described in http://slowfrog.blogspot.com/2012/02/solving-hexiom-using-constraints.html

Python
------

Just run ``python hexiom2.py level01.txt`` to solve one level, or alternatively ``pypy hexiom2.py level01.txt``.
Running ``python hexiom2.py -u`` will show you all available command-line arguments.
If you have pypy in your path, you can also run ``. runpy2.sh``. That will run the solver on all forty levels, except number 38 and 40.


Java
----

You should compile the source file to ``build/main/classes``, or use the included Eclipse project. Then you can run an individual test with ``java -cp build/main/classes com.slowfrog.hexiom.Main2 level01.txt``.
Running ``java -cp build/main/classes com.slowfrog.hexiom.Main2 -u`` will show you available options.
You can also run ``. runj2.sh`` to solve the 38 solvable levels.


Data
====

All forty known levels are available in this directory, named levelXX.txt.

The expected results (at least with the current algorithm) are also here, named resultc.txt, resultj.txt and resultpy.txt.

There is also a ``profile.sh`` script that runs the Java version with the JIP profiler.

