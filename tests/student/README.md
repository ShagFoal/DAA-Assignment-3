# Own tests

- own_cases.jsonl: 18 requests.
- expected.json: named cases with manually determined expected answers.
- actual.jsonl: actual program outputs, in the same order.
- check_student.py: checks these answers and repeated requests with the provided semantic validator.
- RouteAndBenchmark.java: direct route-helper tests, deep DFS and required MST measurements.

| Cases | Expected behaviour |
|---|---|
| DSU | Self/repeated unions leave counts unchanged; successful merges update sizes. |
| Kruskal/Prim | Disconnected example: cost -1, components 3; tied triangle: cost 2. Loops excluded, original edge IDs preserved. |
| SCC | Labels [0,0,1,2,3], cross-component pairs [0,1] and [2,3], or an equivalent relabelling. |
| Topo | Diamond respects every edge; a disconnected cycle or self-loop is rejected. |
| DAG | Routes example: [0,2,-1,3,null]; unreachable cycle rejected; large distance 3000000000; singleton distance 0. |
| Empty graphs | Empty outputs; forests cost 0 with 0 components. DAG uses a singleton instead because n must be positive. |

All 18 cases passed. The checker accepts different valid optima/labels/orders and rejects deliberately incorrect MST, SCC, topo and DAG witnesses. It sends all inputs followed by their reversed sequence in one process: 36 requests passed.

Run from the repository root with Python and JDK 17+ on PATH:

```sh
python3 tests/student/check_student.py
```

This verifies results and refreshes actual.jsonl. To print outputs directly:

```sh
python3 tools/run.py --input tests/student/own_cases.jsonl
```

For the helper, 100000-vertex chain and benchmark:

```sh
python3 tools/run.py --setup
javac --release 17 -cp Java/build -d Java/build tests/student/RouteAndBenchmark.java
java -Xmx384m -cp Java/build RouteAndBenchmark
```

Expected deep-chain results: topo order 0..99999, 100000 SCCs, distance[v]=-v. All passed. Actual route calls, measurements and timing conditions are recorded in ANALYSIS.md.
