import java.util.*;

public final class RouteAndBenchmark {
    static Map<String, Object> graph(int n, int m, long seed) {
        Random random = new Random(seed);
        List<List<Long>> edges = new ArrayList<>(m);
        for (int v = 1; v < n; v++) edges.add(List.of((long) v - 1, (long) v,
                (long) random.nextInt(2_000_000_001) - 1_000_000_000L));
        while (edges.size() < m) edges.add(List.of((long) random.nextInt(n),
                (long) random.nextInt(n),
                (long) random.nextInt(2_000_000_001) - 1_000_000_000L));
        return Map.of("n", n, "edges", edges);
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void verifyForest(Map<String, Object> input, Map<String, Object> output) {
        int n = ((Number) input.get("n")).intValue();
        List<?> edges = (List<?>) input.get("edges");
        List<?> ids = (List<?>) output.get("edge_ids");
        require(((Number) output.get("components")).intValue() == 1, "connected graph");
        require(ids.size() == n - 1, "forest size");
        Set<Integer> seenIds = new HashSet<>();
        List<List<Integer>> neighbours = new ArrayList<>(n);
        for (int v = 0; v < n; v++) neighbours.add(new ArrayList<>());
        long cost = 0;
        for (Object value : ids) {
            int id = ((Number) value).intValue();
            require(id >= 0 && id < edges.size() && seenIds.add(id), "original unique IDs");
            List<?> edge = (List<?>) edges.get(id);
            int u = ((Number) edge.get(0)).intValue(), v = ((Number) edge.get(1)).intValue();
            require(u != v, "no loops");
            neighbours.get(u).add(v);
            neighbours.get(v).add(u);
            cost += ((Number) edge.get(2)).longValue();
        }
        boolean[] visited = new boolean[n];
        int[] queue = new int[n];
        int head = 0, tail = 1;
        visited[0] = true;
        while (head < tail) for (int v : neighbours.get(queue[head++])) {
            if (!visited[v]) { visited[v] = true; queue[tail++] = v; }
        }
        require(tail == n, "all vertices spanned; n-1 edges therefore acyclic");
        require(cost == ((Number) output.get("cost")).longValue(), "sum of selected weights");
    }

    static double median(double[] values) {
        double[] copy = values.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    static void routes() {
        List<List<Long>> edges = List.of(List.of(0L, 1L, 5L), List.of(0L, 1L, 2L),
                List.of(1L, 2L, -3L), List.of(0L, 2L, -1L), List.of(2L, 3L, 4L));
        Map<String, Object> result = Algorithms.dag(Map.of("n", 5, "source", 0, "edges", edges));
        int[] parents = (int[]) result.get("parent_edge");
        Long[] distances = (Long[]) result.get("distance");
        System.out.println("ROUTE input edges=" + edges);
        System.out.println("ROUTE distance=" + Arrays.toString(distances));
        System.out.println("ROUTE parent_edge=" + Arrays.toString(parents));
        for (int target : new int[] {1, 3, 0, 4}) {
            List<Integer> route = Algorithms.reconstructRoute(0, target, parents, edges);
            if (target == 0) require(route.equals(List.of(0)), "source route");
            else if (target == 4) require(route.isEmpty(), "unreachable route");
            else {
                require(route.get(0) == 0 && route.get(route.size() - 1) == target, "endpoints");
                long cost = 0;
                List<Integer> selected = new ArrayList<>();
                for (int i = 1; i < route.size(); i++) {
                    int id = parents[route.get(i)];
                    List<Long> edge = edges.get(id);
                    require(edge.get(0).intValue() == route.get(i - 1), "edge direction");
                    cost += edge.get(2);
                    selected.add(id);
                }
                require(cost == distances[target], "optimal route cost");
                System.out.println("ROUTE selected IDs to " + target + "=" + selected + "; cost=" + cost);
            }
            System.out.println("PASS reconstructRoute(0," + target + ")=" + route);
        }
    }

    static void deepGraph() {
        int n = 100000;
        List<List<Long>> edges = new ArrayList<>();
        for (int v = 1; v < n; v++) edges.add(List.of((long) v - 1, (long) v, -1L));
        Map<String, Object> input = Map.of("n", n, "source", 0, "edges", edges);
        int[] order = (int[]) Algorithms.topo(input).get("order");
        int[] labels = (int[]) Algorithms.scc(input).get("component");
        Long[] distances = (Long[]) Algorithms.dag(input).get("distance");
        Set<Integer> components = new HashSet<>();
        require(order.length == n, "deep topo size");
        for (int v = 0; v < n; v++) {
            require(order[v] == v, "deep topo order");
            require(distances[v] == -(long) v, "deep distance");
            components.add(labels[v]);
        }
        require(components.size() == n, "deep SCC count");
        System.out.println("PASS deep DFS: 100000-vertex chain, topo/SCC/DAG");
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        routes();
        deepGraph();
        System.out.println("ENV Java=" + System.getProperty("java.runtime.version")
                + "; VM=" + System.getProperty("java.vm.name")
                + "; OS=" + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                + "; maxHeap=" + Runtime.getRuntime().maxMemory());
        int[][] sizes = {{1000, 4000}, {10000, 40000}, {50000, 200000}};
        for (int index = 0; index < sizes.length; index++) {
            int n = sizes[index][0], m = sizes[index][1];
            long seed = 20261016L + index;
            Map<String, Object> input = graph(n, m, seed);
            Map<String, Object> kruskal = Algorithms.kruskal(input), prim = Algorithms.prim(input);
            verifyForest(input, kruskal);
            verifyForest(input, prim);
            require(kruskal.get("cost").equals(prim.get("cost")), "same optimum");
            System.out.println("PASS benchmark forest checks n=" + n + ",m=" + m
                    + ",seed=" + seed + ",cost=" + kruskal.get("cost"));
            for (int warmup = 0; warmup < 2; warmup++) {
                Algorithms.kruskal(input);
                Algorithms.prim(input);
            }
            double[] k = new double[3], p = new double[3];
            for (int run = 0; run < 3; run++) {
                for (int turn = 0; turn < 2; turn++) {
                    boolean useKruskal = ((run + turn) % 2 == 0);
                    long start = System.nanoTime();
                    Map<String, Object> result = useKruskal ? Algorithms.kruskal(input) : Algorithms.prim(input);
                    double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                    if (useKruskal) k[run] = elapsed; else p[run] = elapsed;
                    require(result.get("cost").equals(kruskal.get("cost")), "timed result");
                }
            }
            System.out.printf("BENCH n=%d,m=%d,seed=%d; Kruskal=%s; median=%.3f ms; Prim=%s; median=%.3f ms%n",
                    n, m, seed, Arrays.toString(k), median(k), Arrays.toString(p), median(p));
        }
    }
}
