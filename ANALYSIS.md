# Analysis

## Algorithms

### DSU
Parent and size arrays represent the components; find compresses paths and union attaches the smaller tree to the larger one. Only merging different roots changes the component count and largest size, so repeated/self unions do nothing. Empty input returns no states. Time is O(n + q alpha(n)) amortized and memory including output is O(n+q).

### Kruskal
Edges are sorted by weight while preserving their original IDs. DSU accepts an edge only between different components: this preserves a forest, and the cut property makes each chosen edge safe. All components and isolates are included; loops are skipped, and negative/parallel edges are supported. Time is O(n + m log(m+1)) and memory is O(n+m).

### Prim
Adjacency lists and a min-priority queue grow one component at a time. The cheapest edge crossing from visited to unvisited vertices is safe by the cut property; stale candidates are skipped. Restarting at each unvisited vertex handles disconnected graphs and isolates, including empty input. This lazy implementation uses O(n + m log(m+1)) time and O(n+m) memory.

### Kosaraju SCC
The first DFS records finish order; the second DFS visits the transpose in reverse finish order, finding one SCC per traversal. Scanning original edges builds the condensation, with a hash set removing repeated cross-component pairs and internal edges excluded. Isolates get separate labels; empty input returns empty arrays. Expected time and memory are O(n+m).

### DFS topological order
DFS tracks unvisited, active and finished vertices; an edge to an active vertex detects a cycle. Reversing finish order gives a valid topological order because every edge goes from an earlier to a later vertex. All components are checked, cycles return false with an empty order, and an empty graph is acyclic. Time and memory are O(n+m).

### DAG shortest paths
The whole graph is checked for cycles, even outside source reachability, then edges are relaxed in topological order. Each vertex is processed after its predecessors, making its distance final even with negative weights; unreachable vertices are skipped and represented by null. Parent values are original edge IDs; the helper follows them backwards and reverses the route, returning [source] for the source and [] for an unreachable target. Time and memory are O(n+m), plus O(route length) for reconstruction.

Every request creates fresh local state. DFS uses explicit stacks to handle deep graphs without recursive stack overflow. Costs and distances use long; n and m denote vertices and edges, q denotes DSU operations, and memory estimates include output.

## Kruskal versus Prim

Generator: [RouteAndBenchmark.java](tests/student/RouteAndBenchmark.java). Each graph starts with a path, then adds random edges using java.util.Random and the seeds below; weights range from -1000000000 to 1000000000. Before timing, both forests are checked for connectivity, unique original IDs, n-1 edges, no loops, correct weight sums and equal costs.

Environment: Intel i5-12500H, Linux amd64, Microsoft OpenJDK 25.0.4.1, 384 MiB heap, compiled with --release 17. Each size uses two warmups and three measured runs per algorithm, alternating their order. System.nanoTime includes the method's input conversion, graph construction and result creation; it excludes generation, compilation, JVM startup, JSON parsing, I/O, validation and warmup equally for both methods.

| n | m | Seed | Kruskal runs (ms) | Median | Prim runs (ms) | Median |
|---:|---:|---:|---|---:|---|---:|
| 1000 | 4000 | 20261016 | 1.588137, 1.552811, 1.543528 | 1.553 | 2.559201, 2.657639, 1.555405 | 2.559 |
| 10000 | 40000 | 20261017 | 10.312257, 14.083226, 9.724232 | 10.312 | 10.396720, 10.781676, 11.003167 | 10.782 |
| 50000 | 200000 | 20261018 | 63.249217, 44.538395, 46.601071 | 46.601 | 82.885145, 66.969602, 70.240840 | 70.241 |

Kruskal had lower medians for these graphs. Allocation, heap operations and JVM warmup affect timings; three runs on this graph family do not prove asymptotic complexity or a universal winner.

Run from the repository root:

```sh
python3 tools/run.py --setup
javac --release 17 -cp Java/build -d Java/build tests/student/RouteAndBenchmark.java
java -Xmx384m -cp Java/build RouteAndBenchmark
```

## Tests and routes

[tests/student/README.md](tests/student/README.md) explains the 18 own cases, inputs, expected/actual results and commands. They cover empties, disconnected graphs, repeated/self unions, loops, parallel/tied edges, negative weights, 64-bit distances, condensation, cycles and unreachable vertices. All passed, including 36 mixed/repeated requests in one process. Alternative valid answers are accepted and deliberately wrong witnesses rejected. RouteAndBenchmark also verifies a 100000-vertex chain for SCC, topo and DAG; the 36 public cases and state-reset batch passed.

The direct helper test uses edges [[0,1,5],[0,1,2],[1,2,-3],[0,2,-1],[2,3,4]], source 0. Actual distance=[0,2,-1,3,null] and parent_edge=[-1,1,3,4,-1].

| Helper target | Actual vertex route | Selected edge IDs | Cost |
|---:|---|---|---:|
| 1 | [0,1] | [1] | 2 |
| 3 | [0,2,3] | [3,4] | 3 |
| 0 | [0] | [] | 0 |
| 4 | [] (unreachable) | [] | null |

All match the expected routes for the selected parents. The alternative [0,1,2,3] also costs 3; the helper follows actual parent-edge IDs, including parallel edges, and checks direction and total cost.

## Use of AI and sources

ChatGPT was used to create tests and improve the wording of this report.

Sources: the assignment specification, CONTRACTS.md, TESTING.md, DAA lectures 6-7 and Lecture_Clarifications_EN_RU.md.
