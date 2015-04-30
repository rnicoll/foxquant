= Fox Quant =

This is intended as example code of a way of implementing a strategy runner against
Interactive Broker's TWS API. This is an old experiment, and is not maintaned code,
nor is it intended as working application. It is ONLY intended to provide some ideas
on how this automated trading can be implemented.

== Directory Structure ==

Right, that out of the way... the directory structure looks like:

LICENSE - License details for the source code and libraries.
PROBLEMS - Internal notes on problems that need resolving.
README - This file
build.xml - is the build file for ant ( http://ant.apache.org/ ).
etc/ - contains useful files that go with FoxQuant, but are not libraries or source code.
src/ contains the source code to FoxQuant.

== Code ==

The two files probably most interesting to look at are:
  src/org/lostics/foxquant/ib/ConnectionManager.java
  src/org/lostics/foxquant/ib/TWSContractManager.java

ConnectionManager.java handles most of the communcation with the TWS API. TWSContractManager.java
filters incoming prices and order statuses about a single contract (something tradeable) and
converts them into 5 second or minute bars, and tracks orders in progress.

