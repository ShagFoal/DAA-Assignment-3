import java.util.*;

public final class Algorithms {
    private Algorithms() {}

    private static int number(Object value) { return ((Number) value).intValue(); }
    private static int vertexCount(Map<String, Object> input) { return number(input.get("n")); }
    private static List<?> rows(Map<String, Object> input, String key) {
        return (List<?>) input.get(key);
    }

    private static final class UnionFind {
        final int[] parent, size;
        int components, largest;
        UnionFind(int n) {
            parent = new int[n];
            size = new int[n];
            components = n;
            largest = n == 0 ? 0 : 1;
            for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
        }
        int find(int vertex) {
            int root = vertex;
            while (root != parent[root]) root = parent[root];
            while (vertex != root) {
                int next = parent[vertex];
                parent[vertex] = root;
                vertex = next;
            }
            return root;
        }
        boolean union(int u, int v) {
            int a = find(u), b = find(v);
            if (a == b) return false;
            if (size[a] < size[b]) { int temporary = a; a = b; b = temporary; }
            parent[b] = a;
            size[a] += size[b];
            largest = Math.max(largest, size[a]);
            components--;
            return true;
        }
    }

    static Map<String, Object> dsu(Map<String, Object> input) {
        UnionFind sets = new UnionFind(vertexCount(input));
        List<int[]> states = new ArrayList<>();
        for (Object row : rows(input, "operations")) {
            List<?> operation = (List<?>) row;
            sets.union(number(operation.get(0)), number(operation.get(1)));
            states.add(new int[] {sets.components, sets.largest});
        }
        return Map.of("states", states);
    }

    private record Edge(int u, int v, long weight, int id) {}
    private static final Comparator<Edge> EDGE_ORDER =
            Comparator.comparingLong(Edge::weight).thenComparingInt(Edge::id);

    private static Edge[] weightedEdges(Map<String, Object> input) {
        List<?> values = rows(input, "edges");
        Edge[] edges = new Edge[values.size()];
        for (int id = 0; id < edges.length; id++) {
            List<?> row = (List<?>) values.get(id);
            edges[id] = new Edge(number(row.get(0)), number(row.get(1)),
                    ((Number) row.get(2)).longValue(), id);
        }
        return edges;
    }

    static Map<String, Object> kruskal(Map<String, Object> input) {
        UnionFind sets = new UnionFind(vertexCount(input));
        Edge[] edges = weightedEdges(input);
        Arrays.sort(edges, EDGE_ORDER);
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        for (Edge edge : edges) {
            if (sets.union(edge.u(), edge.v())) {
                selected.add(edge.id());
                cost += edge.weight();
            }
        }
        return Map.of("cost", cost, "edge_ids", selected, "components", sets.components);
    }


    private static int[][] edgeAdjacency(int n, Edge[] edges, boolean undirected) {
        int[] degree = new int[n];
        for (Edge edge : edges) {
            degree[edge.u()]++;
            if (undirected) degree[edge.v()]++;
        }
        int[][] adjacency = new int[n][];
        for (int v = 0; v < n; v++) adjacency[v] = new int[degree[v]];
        Arrays.fill(degree, 0);
        for (Edge edge : edges) {
            adjacency[edge.u()][degree[edge.u()]++] = edge.id();
            if (undirected) adjacency[edge.v()][degree[edge.v()]++] = edge.id();
        }
        return adjacency;
    }

    static Map<String, Object> prim(Map<String, Object> input) {
        int n = vertexCount(input);
        Edge[] edges = weightedEdges(input);
        int[][] adjacency = edgeAdjacency(n, edges, true);
        boolean[] visited = new boolean[n];
        PriorityQueue<Edge> candidates = new PriorityQueue<>(EDGE_ORDER);
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        int components = 0;
        for (int root = 0; root < n; root++) {
            if (visited[root]) continue;
            components++;
            visited[root] = true;
            for (int id : adjacency[root]) {
                Edge edge = edges[id];
                int other = edge.u() == root ? edge.v() : edge.u();
                if (!visited[other]) candidates.add(edge);
            }
            while (!candidates.isEmpty()) {
                Edge edge = candidates.remove();
                if (visited[edge.u()] && visited[edge.v()]) continue;
                int next = visited[edge.u()] ? edge.v() : edge.u();
                visited[next] = true;
                selected.add(edge.id());
                cost += edge.weight();
                for (int id : adjacency[next]) {
                    Edge outgoing = edges[id];
                    int other = outgoing.u() == next ? outgoing.v() : outgoing.u();
                    if (!visited[other]) candidates.add(outgoing);
                }
            }
        }
        return Map.of("cost", cost, "edge_ids", selected, "components", components);
    }

    private static int[][] directedAdjacency(int n, List<?> edges, boolean transpose) {
        int[] degree = new int[n];
        int from = transpose ? 1 : 0, to = transpose ? 0 : 1;
        for (Object value : edges) degree[number(((List<?>) value).get(from))]++;
        int[][] graph = new int[n][];
        for (int v = 0; v < n; v++) graph[v] = new int[degree[v]];
        Arrays.fill(degree, 0);
        for (Object value : edges) {
            List<?> edge = (List<?>) value;
            int u = number(edge.get(from));
            graph[u][degree[u]++] = number(edge.get(to));
        }
        return graph;
    }


    private static int[] finishOrder(int[][] graph, boolean rejectCycles) {
        int n = graph.length, count = 0;
        byte[] state = new byte[n];
        int[] stack = new int[n], nextNeighbour = new int[n], order = new int[n];
        for (int root = 0; root < n; root++) {
            if (state[root] != 0) continue;
            int top = 0;
            stack[0] = root;
            state[root] = 1;
            while (top >= 0) {
                int u = stack[top];
                if (nextNeighbour[u] == graph[u].length) {
                    state[u] = 2;
                    order[count++] = u;
                    top--;
                } else {
                    int v = graph[u][nextNeighbour[u]++];
                    if (state[v] == 0) {
                        state[v] = 1;
                        stack[++top] = v;
                    } else if (rejectCycles && state[v] == 1) {
                        return null;
                    }
                }
            }
        }
        return order;
    }

    static Map<String, Object> scc(Map<String, Object> input) {
        int n = vertexCount(input);
        List<?> edges = rows(input, "edges");
        int[][] graph = directedAdjacency(n, edges, false);
        int[][] transpose = directedAdjacency(n, edges, true);
        int[] finish = finishOrder(graph, false);
        int[] component = new int[n], stack = new int[n];
        Arrays.fill(component, -1);
        int label = 0;
        for (int i = n - 1; i >= 0; i--) {
            int root = finish[i];
            if (component[root] != -1) continue;
            int size = 0;
            stack[size++] = root;
            component[root] = label;
            while (size > 0) {
                int u = stack[--size];
                for (int v : transpose[u]) {
                    if (component[v] == -1) {
                        component[v] = label;
                        stack[size++] = v;
                    }
                }
            }
            label++;
        }
        Set<Long> seen = new HashSet<>();
        List<int[]> condensation = new ArrayList<>();
        for (Object value : edges) {
            List<?> edge = (List<?>) value;
            int a = component[number(edge.get(0))], b = component[number(edge.get(1))];
            long pair = ((long) a << 32) | (b & 0xffffffffL);
            if (a != b && seen.add(pair)) condensation.add(new int[] {a, b});
        }
        return Map.of("component", component, "condensation", condensation);
    }

    static Map<String, Object> topo(Map<String, Object> input) {
        int[][] graph = directedAdjacency(vertexCount(input), rows(input, "edges"), false);
        int[] order = finishOrder(graph, true);
        if (order == null) return Map.of("acyclic", false, "order", new int[0]);
        reverse(order);
        return Map.of("acyclic", true, "order", order);
    }

    private static void reverse(int[] values) {
        for (int i = 0, j = values.length - 1; i < j; i++, j--) {
            int temporary = values[i]; values[i] = values[j]; values[j] = temporary;
        }
    }

    static Map<String, Object> dag(Map<String, Object> input) {
        int n = vertexCount(input), source = number(input.get("source"));
        int[][] graph = directedAdjacency(n, rows(input, "edges"), false);
        int[] finish = finishOrder(graph, true);
        if (finish == null) return Map.of("acyclic", false, "distance", new Long[0],
                "parent_edge", new int[0]);
        Edge[] edges = weightedEdges(input);
        int[][] adjacency = edgeAdjacency(n, edges, false);
        Long[] distance = new Long[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;
        for (int i = n - 1; i >= 0; i--) {
            int u = finish[i];
            if (distance[u] == null) continue;
            for (int id : adjacency[u]) {
                Edge edge = edges[id];
                long candidate = distance[u] + edge.weight();
                if (distance[edge.v()] == null || candidate < distance[edge.v()]) {
                    distance[edge.v()] = candidate;
                    parentEdge[edge.v()] = id;
                }
            }
        }
        return Map.of("acyclic", true, "distance", distance, "parent_edge", parentEdge);
    }


    static List<Integer> reconstructRoute(int source, int target, int[] parentEdge,
                                         List<?> inputEdges) {
        if (target != source && parentEdge[target] == -1) return List.of();
        List<Integer> route = new ArrayList<>();
        int current = target;
        while (current != source) {
            route.add(current);
            int id = parentEdge[current];
            if (id < 0) throw new IllegalArgumentException("Broken parent-edge path");
            List<?> edge = (List<?>) inputEdges.get(id);
            if (number(edge.get(1)) != current) throw new IllegalArgumentException("Wrong edge direction");
            current = number(edge.get(0));
            if (route.size() > parentEdge.length) throw new IllegalArgumentException("Parent-edge cycle");
        }
        route.add(source);
        Collections.reverse(route);
        return route;
    }
}
