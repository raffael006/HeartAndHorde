import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class PathFinder {
    public static final int GRID_SIZE = 20; // Ukuran area Graph per cell
    public static final int MAX_COL = 3000 / GRID_SIZE;
    public static final int MAX_ROW = 3000 / GRID_SIZE;

    public static class Node implements Comparable<Node> {
        public int col, row;
        public int gCost, hCost;
        public Node parent;
        public boolean solid;

        public Node(int col, int row) {
            this.col = col;
            this.row = row;
        }

        public int getFCost() { return gCost + hCost; }

        @Override
        public int compareTo(Node other) {
            int compare = Integer.compare(this.getFCost(), other.getFCost());
            if (compare == 0) {
                compare = Integer.compare(this.hCost, other.hCost);
            }
            return compare;
        }
    }

    public static List<Point> findPath(double startX, double startY, double targetX, double targetY, List<Building> buildings) {
        int startCol = Math.max(0, Math.min((int) (startX / GRID_SIZE), MAX_COL - 1));
        int startRow = Math.max(0, Math.min((int) (startY / GRID_SIZE), MAX_ROW - 1));
        int targetCol = Math.max(0, Math.min((int) (targetX / GRID_SIZE), MAX_COL - 1));
        int targetRow = Math.max(0, Math.min((int) (targetY / GRID_SIZE), MAX_ROW - 1));

        Node[][] grid = new Node[MAX_COL][MAX_ROW];
        for (int c = 0; c < MAX_COL; c++) {
            for (int r = 0; r < MAX_ROW; r++) {
                grid[c][r] = new Node(c, r);
            }
        }

        // Terapkan Hitbox Rumah ke Node (Menjadi Rintangan/Obstacle)
        for (Building b : buildings) {
            Rectangle wall = b.getSolidHitbox();

            // FIX: Padding/Dinding Gaib Dihapus agar pasukan tidak nyangkut dari titik awal!
            int minCol = Math.max(0, (wall.x) / GRID_SIZE);
            int minRow = Math.max(0, (wall.y) / GRID_SIZE);
            int maxCol = Math.min(MAX_COL - 1, (wall.x + wall.width) / GRID_SIZE);
            int maxRow = Math.min(MAX_ROW - 1, (wall.y + wall.height) / GRID_SIZE);

            for (int c = minCol; c <= maxCol; c++) {
                for (int r = minRow; r <= maxRow; r++) {
                    grid[c][r].solid = true;
                }
            }
        }

        // Buka blokir di titik awal dan tujuan agar algoritma tidak panik
        grid[startCol][startRow].solid = false;
        grid[targetCol][targetRow].solid = false;

        PriorityQueue<Node> openList = new PriorityQueue<>();
        boolean[][] closedList = new boolean[MAX_COL][MAX_ROW];

        Node startNode = grid[startCol][startRow];
        Node targetNode = grid[targetCol][targetRow];
        openList.add(startNode);

        while (!openList.isEmpty()) {
            Node currentNode = openList.poll();
            closedList[currentNode.col][currentNode.row] = true;

            if (currentNode == targetNode) {
                return trackPath(targetNode);
            }

            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) continue;

                    int checkCol = currentNode.col + i;
                    int checkRow = currentNode.row + j;

                    if (checkCol >= 0 && checkCol < MAX_COL && checkRow >= 0 && checkRow < MAX_ROW) {
                        Node neighbor = grid[checkCol][checkRow];
                        if (neighbor.solid || closedList[checkCol][checkRow]) continue;

                        // Mencegah tembus sudut rumah (Corner cutting)
                        if (Math.abs(i) == 1 && Math.abs(j) == 1) {
                            if (grid[currentNode.col + i][currentNode.row].solid || grid[currentNode.col][currentNode.row + j].solid) {
                                continue;
                            }
                        }

                        int moveCost = (Math.abs(i) == 1 && Math.abs(j) == 1) ? 14 : 10;
                        int newCost = currentNode.gCost + moveCost;

                        if (newCost < neighbor.gCost || !openList.contains(neighbor)) {
                            neighbor.gCost = newCost;
                            neighbor.hCost = (Math.abs(checkCol - targetCol) + Math.abs(checkRow - targetRow)) * 10;
                            neighbor.parent = currentNode;
                            if (!openList.contains(neighbor)) openList.add(neighbor);
                        }
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    private static List<Point> trackPath(Node targetNode) {
        List<Point> path = new ArrayList<>();
        Node currentNode = targetNode;
        while (currentNode != null) {
            path.add(new Point(currentNode.col * GRID_SIZE + (GRID_SIZE / 2), currentNode.row * GRID_SIZE + (GRID_SIZE / 2)));
            currentNode = currentNode.parent;
        }
        Collections.reverse(path);
        return path;
    }
}