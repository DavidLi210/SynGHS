# CS 6380: Distributed Computing

# Section 001

# Project 2

## Instructor: Neeraj Mittal

I do this project in C, C++ or Java. Each student is expected to demonstrate the
operation of this project to the instructor or the TA. Since the project involves socket programming,
I can only use machinesdcXX.utdallas.edu, whereXX∈ {01, 02, .., 45}, for running the program.
The demonstration has to be ondcXX


## 1 Project Description

This project consists of two parts: (a) build a message-passing synchronous distributed system in
which nodes are arranged in a certain topology (given in a configuration file), and (b) implement
SynchGHSalgorithm as described in the textbook for constructing a minimum spanning tree (MST).
I assume that all links are bidirectional. As in the first project, I need to use a
synchronizer to simulate a synchronous system.

Output: Upon termination, each node should print the following to the screen: the subset of its
edges that are part of the MST.

## 2 How to run it

Run the main of Node.java meanwhile passing node id as input to start the node with id.

## 3 Sample Configuration File

\### #

\# Configuration file for CS 6380 Project 2 (Spring 2018)
\#


\# As per the “shell” convention, anything following a hash sign is
\# a comment and should be ignored by the parser.
\#

\# Number of nodes
7

\# Here we list the individual nodes
\#
\# Format is:
\# UID Hostname Port
5 dc02.utdallas.edu 1234
200 dc03.utdallas.edu 1233
8 dc04.utdallas.edu 1233
184 dc05.utdallas.edu 1232
9 dc06.utdallas.edu 1233
37 dc07.utdallas.edu 1235
78 dc08.utdallas.edu 1236

\# List of edges and their weight, one per line. An edge is denoted
\# by (smaller uid, larger uid)


(5,200) 5
(5,8) 3
(5,37) 10
(8,184) 1
(8,78) 3
(184,200) 3
(37,78) 1
(9,78) 2
(9,200) 5
```
Notice: for this project, I simply delay each round for seconds to make it a sync network, a better way would be to use synchronizer in async network.
```

```
Feel free to email me about more details about this project
```
