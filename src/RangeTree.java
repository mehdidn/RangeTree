import java.awt.geom.Point2D;
import java.util.*;

// class Range Tree
public class RangeTree {
    // Constructor
    public RangeTree(Point2D[] point2DS) {
        this.point2DS = point2DS;
        Arrays.sort(point2DS, xCompare);
        linkFraction = new Fraction[point2DS.length];
        createFraction();
    }

    // Range Tree nodes
    class RangeTreeNode {
        int src, dest;

        RangeTreeNode(RangeTreeNode node) {
            src = node.src;
            dest = node.dest;
        }

        RangeTreeNode(int src, int dest) {
            this.src = src;
            this.dest = dest;
        }

        RangeTreeNode getRightChild() {
            if (dest - src == 1)
                return null;
            return become(pos() + 1, dest);
        }

        RangeTreeNode getLeftChild() {
            if (dest - src == 1)
                return null;
            return become(src, pos());
        }

        int pos() {
            return src + (dest - src + 1) / 2 - 1;
        }

        Point2D get() {
            return point2DS[pos()];
        }

        RangeTreeNode become(int src, int dest) {
            if (src == dest)
                return null;

            this.src = src;
            this.dest = dest;
            return this;
        }
    }

    // inner static class Fraction
    static class Fraction {
        Point2D[] point2DS;
        int[] left, right;

        Fraction(Point2D[] point2DS) {
            this.point2DS = point2DS;
            Arrays.sort(point2DS, RangeTree.yCompare);
            right = new int[point2DS.length];
            left = new int[point2DS.length];
        }
    }

    // Attributes
    Point2D[] point2DS;
    Fraction[] linkFraction;

    // x comparator
    static Comparator<Point2D> xCompare = new Comparator<Point2D>() {
        @Override
        public int compare(Point2D o1, Point2D o2) {
            return ComparisonChain.start().compare(o1.getX(), o2.getX()).compare(o1.getY(), o2.getY()).result();
        }
    };

    // y comparator
    static Comparator<Point2D> yCompare = new Comparator<Point2D>() {
        @Override
        public int compare(Point2D o1, Point2D o2) {
            return ComparisonChain.start().compare(o1.getY(), o2.getY()).compare(o1.getX(), o2.getX()).result();
        }
    };

    public void createFraction() {
        RangeTreeNode root = root();
        linkFraction[root.pos()] = new Fraction(Arrays.copyOf(point2DS, point2DS.length));
        createFraction(root);
    }

    public void createFraction(RangeTreeNode n) {
        int index = n.pos(), src = n.src, dest = n.dest;
        if (n.getLeftChild() != null) {
            linkFraction[n.pos()] = new Fraction(Arrays.copyOfRange(point2DS, n.src, n.dest));
            linkFraction(linkFraction[index], linkFraction[index].left, linkFraction[n.pos()]);
            createFraction(n);
        } else
            Arrays.fill(linkFraction[index].left, -1);
        n.become(src, dest);
        if (n.getRightChild() != null) {
            linkFraction[n.pos()] = new Fraction(Arrays.copyOfRange(point2DS, n.src, n.dest));
            linkFraction(linkFraction[index], linkFraction[index].right, linkFraction[n.pos()]);
            createFraction(n);
        } else Arrays.fill(linkFraction[index].right, -1);
    }

    public void linkFraction(Fraction parent, int[] parentLinks, Fraction child) {
        int par = 0, ch = 0;
        while (par < parentLinks.length && ch < child.point2DS.length) {
            if (child.point2DS[ch].getY() >= parent.point2DS[par].getY()) {
                parentLinks[par] = ch;
                par++;
            } else
                ch++;
        }
        for (; par < parentLinks.length; par++)
            parentLinks[par] = -1;
    }

    public RangeTreeNode root() {
        return create(0, point2DS.length);
    }

    public RangeTreeNode create(int src, int dest) {
        if (src == dest)
            return null;
        return new RangeTreeNode(src, dest);
    }

    private RangeTreeNode split(double start, double end) {
        return split(root(), start, end);
    }

    private RangeTreeNode split(RangeTreeNode node, double start, double end) {
        if (node == null)
            return node;
        Point2D point2D = node.get();
        if (point2D.getX() >= end)
            return split(node.getLeftChild(), start, end);
        else if (point2D.getX() < start)
            return split(node.getRightChild(), start, end);
        else
            return node;
    }

    private void get(List<Point2D> out, RangeTreeNode node, int yMinX, double yMax) {
        if (node == null || yMinX == -1) return;
        Point2D[] add = linkFraction[node.pos()].point2DS;
        for (int i = yMinX; i < add.length && add[i].getY() < yMax; i++)
            out.add(add[i]);
    }

    private void getSmaller(List<Point2D> out, RangeTreeNode node, double data, int yMinX, double yMin, double yMax) {
        if (node == null || yMinX == -1)
            return;
        Fraction frac = linkFraction[node.pos()];
        Point2D point2D = node.get();
        if (point2D.getX() < data) {
            int src = node.src, dest = node.dest;
            if (point2D.getY() < yMax && point2D.getY() >= yMin)
                out.add(point2D);
            getSmaller(out, node.getRightChild(), data, frac.right[yMinX], yMin, yMax);
            node.become(src, dest);
            get(out, node.getLeftChild(), frac.left[yMinX], yMax);
        } else {
            getSmaller(out, node.getLeftChild(), data, frac.left[yMinX], yMin, yMax);
        }
    }

    private void getHigherOrEqual(List<Point2D> out, RangeTreeNode node, double data, int yMinX, double yMin, double yMax) {
        if (node == null || yMinX == -1)
            return;
        Fraction frac = linkFraction[node.pos()];
        Point2D point2D = node.get();
        if (point2D.getX() >= data) {
            int src = node.src, dest = node.dest;
            if (point2D.getY() < yMax && point2D.getY() >= yMin) out.add(point2D);
            getHigherOrEqual(out, node.getLeftChild(), data, frac.left[yMinX], yMin, yMax);
            node.become(src, dest);
            get(out, node.getRightChild(), frac.right[yMinX], yMax);
        } else
            getHigherOrEqual(out, node.getRightChild(), data, frac.right[yMinX], yMin, yMax);
    }

    public ArrayList<Point2D> rangeSearch(double xMin, double xMax, double yMin, double yMax) {
        ArrayList<Point2D> out = new ArrayList<>();
        rangeSearch(out, xMin, xMax, yMin, yMax);
        return out;
    }

    public void rangeSearch(List<Point2D> out, double xMin, double xMax, double yMin, double yMax) {
        RangeTreeNode node = split(xMin, xMax);
        if (node == null)
            return;
        Fraction frac = linkFraction[node.pos()];
        Point2D[] ypoints = linkFraction[node.pos()].point2DS;

        int yMinX = Arrays.binarySearch(ypoints, new Point2D.Double(Double.NEGATIVE_INFINITY, yMin), yCompare);
        if (yMinX < 0) yMinX = -1 * yMinX - 1;
        if (yMinX == ypoints.length)
            return;
        Point2D point2D = node.get();
        if (point2D.getY() < yMax && point2D.getY() >= yMin)
            out.add(point2D);
        RangeTreeNode leftChild = new RangeTreeNode(node).getLeftChild();
        RangeTreeNode rightChild = new RangeTreeNode(node).getRightChild();
        getSmaller(out, rightChild, xMax, frac.right[yMinX], yMin, yMax);
        getHigherOrEqual(out, leftChild, xMin, frac.left[yMinX], yMin, yMax);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();

        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; ++i) {
            x[i] = scanner.nextDouble();
        }

        for (int i = 0; i < n; ++i) {
            y[i] = scanner.nextDouble();
        }

        Point2D[] point2DS = new Point2D[n];
        for (int i = 0; i < n; ++i) {
            point2DS[i] = new Point2D.Double(x[i], y[i]);
        }
        RangeTree rangeTree = new RangeTree(point2DS);

        int k = scanner.nextInt();
        for (int i = 0; i < k; ++i) {
            double minX = scanner.nextDouble();
            minX -= 0.000001;
            double minY = scanner.nextDouble();
            minY -= 0.000001;
            double maxX = scanner.nextDouble();
            maxX += 0.000001;
            double maxY = scanner.nextDouble();
            maxY += 0.000001;

            ArrayList<Point2D> point2DList = rangeTree.rangeSearch(minX, maxX, minY, maxY);
            if (point2DList.size() == 0) {
                System.out.println("None");
                continue;
            }

            ArrayList<Point2D> points = new ArrayList<>();
            for (Point2D point2D : point2DList) {
                if (points.size() == 0)
                    points.add(point2D);
                else
                    for (int j = 0; j < points.size(); ++j) {
                        if (point2D.getY() < points.get(j).getY()) {
                            points.add(j, point2D);
                            break;
                        }

                        if (j == points.size() - 1) {
                            points.add(point2D);
                            break;
                        }
                    }
            }

            for (Point2D point2D : points) {
                double xPoint = point2D.getX();
                System.out.print(xPoint + " ");
            }
            System.out.println();

            for (Point2D point2D : points) {
                double yPoint = point2D.getY();
                System.out.print(yPoint + " ");
            }
            System.out.println();
        }
    }
}

abstract class ComparisonChain {
    public abstract ComparisonChain compare(double left, double right);
    public abstract int result();

    private static final ComparisonChain ACTIVE = new ComparisonChain() {
                @Override
                public ComparisonChain compare(double left, double right) {
                    return classify(Double.compare(left, right));
                }

                ComparisonChain classify(int result) {
                    return (result < 0) ? LESS : (result > 0) ? GREATER : ACTIVE;
                }

                @Override
                public int result() {
                    return 0;
                }
            };

    public static ComparisonChain start() {
        return ACTIVE;
    }

    public static final ComparisonChain LESS = new InactiveComparisonChain(-1);
    public static final ComparisonChain GREATER = new InactiveComparisonChain(1);

    private static final class InactiveComparisonChain extends ComparisonChain {
        final int result;

        InactiveComparisonChain(int result) {
            this.result = result;
        }

        @Override
        public ComparisonChain compare(double left, double right) {
            return this;
        }

        @Override
        public int result() {
            return result;
        }
    }
}