/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tools.Delaunay;

import Tools.QuadTree.point.PointQuadTree;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Florian
 */
public class DelaunayTInROI {
    // starting edge for walk (see locate() method)
    private QuadEdge startingEdge = null;
    // list of quadEdge belonging to Delaunay triangulation
    private List<QuadEdge> quadEdge = new ArrayList<QuadEdge>();
    public PointQuadTree<PointT> m_treeTriangles = null;
    public ArrayList < PointT > m_allPoints = new ArrayList < PointT >();
    public ArrayList<TriangleT> m_triangles2 = new ArrayList<TriangleT>();
    public ArrayList<Point2D.Double[]> m_edges = new ArrayList<Point2D.Double[]>(), m_triangles = new ArrayList<Point2D.Double[]>(), m_voronoi = new ArrayList<Point2D.Double[]>();
    public ArrayList<CircleT> m_circles = new ArrayList<CircleT>();
    
    // Bounding box of the triangulation

    class BoundingBox {

        double minx, miny, maxx, maxy;
        Point2D.Double a = new Point2D.Double(); // lower left
        Point2D.Double b = new Point2D.Double(); // lower right
        Point2D.Double c = new Point2D.Double(); // upper right
        Point2D.Double d = new Point2D.Double(); // upper left
    }
    private BoundingBox bbox = new BoundingBox();

    /**
     * Constuctor:
     */
    public DelaunayTInROI() {

        bbox.minx = Double.MAX_VALUE;
        bbox.maxx = Double.MIN_VALUE;
        bbox.miny = Double.MAX_VALUE;
        bbox.maxy = Double.MIN_VALUE;

        // create the QuadEdge graph of the bounding box
        QuadEdge ab = QuadEdge.makeEdge(bbox.a, bbox.b);
        QuadEdge bc = QuadEdge.makeEdge(bbox.b, bbox.c);
        QuadEdge cd = QuadEdge.makeEdge(bbox.c, bbox.d);
        QuadEdge da = QuadEdge.makeEdge(bbox.d, bbox.a);
        QuadEdge.splice(ab.sym(), bc);
        QuadEdge.splice(bc.sym(), cd);
        QuadEdge.splice(cd.sym(), da);
        QuadEdge.splice(da.sym(), ab);

        this.startingEdge = ab;
    }
    
    public void execute( ArrayList < Point2D.Double > _points, int _w, int _h, Roi[] _rois ){
        m_treeTriangles = new PointQuadTree< PointT >(new Point2D.Double(0, 0), new Point2D.Double(_w, _h), 8, 6);
        for( Point2D.Double point : _points ){
            PointT p = new PointT( point.x, point.y );
            insertPoint( p );
            m_treeTriangles.insert( p.x, p.y, p );
            m_allPoints.add( p );
        }
        execute( _w, _h, _rois );  
    }
    
    public void executeWithInfo( ArrayList < Point2D.Double > _points, ArrayList < Integer > _infos, int _w, int _h, Roi[] _rois ){
        m_treeTriangles = new PointQuadTree< PointT >(new Point2D.Double(0, 0), new Point2D.Double(_w, _h), 8, 6);
        for( int n = 0; n < _points.size(); n++ ){
            PointT p = new PointTWithInfo( _points.get(n).x, _points.get(n).y, _infos.get(n) );
            insertPoint( p );
            m_treeTriangles.insert( p.x, p.y, p );
            m_allPoints.add( p );
        }
        execute( _w, _h, _rois );  
    }
    
    public void execute( int _w, int _h, Roi[] _rois ){
        // do not process edges pointing to/from surrouding triangle
        // --> mark them as already computed
        for (QuadEdge q : this.quadEdge) {
            q.mark = false;
            q.sym().mark = false;
            if (q.orig() == bbox.a || q.orig() == bbox.b || q.orig() == bbox.c || q.orig() == bbox.d) {
                q.mark = true;
            }
            if (q.dest() == bbox.a || q.dest() == bbox.b || q.dest() == bbox.c || q.dest() == bbox.d) {
                q.sym().mark = true;
            }
        }
        
        // compute the 2 triangles associated to each quadEdge
        for (QuadEdge qe : quadEdge) {
            // first triangle
            QuadEdge q1 = qe;
            QuadEdge q2 = q1.lnext();
            QuadEdge q3 = q2.lnext();
            if (!q1.mark && !q2.mark && !q3.mark) {
                PointT[] points = new PointT[3];
                points[0] = m_treeTriangles.get(q1.orig(), 0.1).getElement();
                points[1] = m_treeTriangles.get(q2.orig(), 0.1).getElement();
                points[2] = m_treeTriangles.get(q3.orig(), 0.1).getElement();
                double x = (points[0].getX() + points[1].getX() + points[2].getX()) / 3., y = (points[0].getY() + points[1].getY() + points[2].getY()) / 3.;
                if (isInROIWithHoles(x, y, _rois)) {
                    TriangleT tri = new TriangleT(points[0], points[1], points[2]);
                    m_triangles2.add(tri);
                    for (int n = 0; n < points.length; n++) {
                        ArrayList<TriangleT> tris = points[n].getTriangles();
                        for (int i = 0; i < tris.size(); i++) {
                            TriangleT t = tris.get(i);
                            if (tri.isNewNeighbor(t)) {
                                tri.addNeighbor(t);
                                t.addNeighbor(tri);
                            }
                        }
                    }
                }
            }

            // second triangle
            QuadEdge qsym1 = qe.sym();
            QuadEdge qsym2 = qsym1.lnext();
            QuadEdge qsym3 = qsym2.lnext();
            if (!qsym1.mark && !qsym2.mark && !qsym3.mark) {
                PointT[] points = new PointT[3];
                points[0] = m_treeTriangles.get(qsym1.orig(), 0.1).getElement();
                points[1] = m_treeTriangles.get(qsym2.orig(), 0.1).getElement();
                points[2] = m_treeTriangles.get(qsym3.orig(), 0.1).getElement();
                double x = (points[0].getX() + points[1].getX() + points[2].getX()) / 3., y = (points[0].getY() + points[1].getY() + points[2].getY()) / 3.;
                if (isInROIWithHoles(x, y, _rois)) {
                    TriangleT tri = new TriangleT(points[0], points[1], points[2]);
                    m_triangles2.add(tri);
                    for (int n = 0; n < points.length; n++) {
                        ArrayList<TriangleT> tris = points[n].getTriangles();
                        for (int i = 0; i < tris.size(); i++) {
                            TriangleT t = tris.get(i);
                            if (tri.isNewNeighbor(t)) {
                                tri.addNeighbor(t);
                                t.addNeighbor(tri);
                            }
                        }
                    }
                }
            }

            // mark as used
            qe.mark = true;
            qe.sym().mark = true;
        }
        //computeTriangles();
        for( int i = 0; i < m_triangles2.size(); i++ )
            m_triangles2.get( i ).setMarked( false );
    }
    
    public boolean isInROIWithHoles(double _x, double _y, Roi[] _rois){
        if(_rois == null)
            return true;
        FloatPolygon shape = _rois[0].getFloatPolygon();
        if (!shape.contains(_x, _y)) {
            return false;
        }
        if(_rois.length == 1)
            return true;
        for (int i = 1; i < _rois.length; i++) {
            if (_rois[i].getFloatPolygon().contains(_x, _y)) {
                return false; //the point x,y is inside a hole of the neuron
            }
        }
        return true;
    }
    
    public void clearDatas(){
        m_treeTriangles.clear();
        m_allPoints.clear();
        quadEdge.clear();
        m_triangles.clear();
        //m_triangles2.clear();
    }
    
    /**
     *  Inserts a new point into a Delaunay triangulation
     *  (Guibas and Stolfi)
     *
     * @param p the point to insert
     */
    public void insertPoint(Point2D.Double p) {
        QuadEdge e = locate(p);

        // point is a duplicate -> nothing to do
        if (p.x == e.orig().x && p.y == e.orig().y) {
            return;
        }
        if (p.x == e.dest().x && p.y == e.dest().y) {
            return;
        }

        // point is on an existing edge -> remove the edge
        if (QuadEdge.isOnLine(e, p)) {
            e = e.oprev();
            this.quadEdge.remove(e.onext().sym());
            this.quadEdge.remove(e.onext());
            QuadEdge.deleteEdge(e.onext());
        }

        // Connect the new point to the vertices of the containing triangle
        // (or quadrilateral in case of the point is on an existing edge)
        QuadEdge base = QuadEdge.makeEdge(e.orig(), p);
        this.quadEdge.add(base);

        QuadEdge.splice(base, e);
        this.startingEdge = base;
        do {
            base = QuadEdge.connect(e, base.sym());
            this.quadEdge.add(base);
            e = base.oprev();
        } while (e.lnext() != startingEdge);

        // Examine suspect edges to ensure that the Delaunay condition is satisfied.
        do {
            QuadEdge t = e.oprev();

            if (QuadEdge.isAtRightOf(e, t.dest())
                    && QuadEdge.inCircle(e.orig(), t.dest(), e.dest(), p)) {
                // flip triangles
                QuadEdge.swapEdge(e);
                e = e.oprev();
            } else if (e.onext() == startingEdge) {
                return; // no more suspect edges
            } else {
                e = e.onext().lprev();  // next suspect edge
            }
        } while (true);
    }
    
    /**
     * Returns an edge e of the triangle containing the point p
     * (Guibas and Stolfi)
     *
     * @param p the point to localte
     * @return the edge of the triangle
     */
    private QuadEdge locate(Point2D.Double p) {

        /* outside the bounding box ? */
        if (p.x < bbox.minx || p.x > bbox.maxx || p.y < bbox.miny || p.y > bbox.maxy) {
            updateBoundigBox(p);
        }

        QuadEdge e = startingEdge;
        while (true) {
            /* duplicate point ? */
            if (p.x == e.orig().x && p.y == e.orig().y) {
                return e;
            }
            if (p.x == e.dest().x && p.y == e.dest().y) {
                return e;
            }

            /* walk */
            if (QuadEdge.isAtRightOf(e, p)) {
                e = e.sym();
            } else if (!QuadEdge.isAtRightOf(e.onext(), p)) {
                e = e.onext();
            } else if (!QuadEdge.isAtRightOf(e.dprev(), p)) {
                e = e.dprev();
            } else {
                return e;
            }
        }
    }
    
    // update the size of the bounding box (cf locate() method)
    private void updateBoundigBox(Point2D.Double p) {
        double minx = Math.min(bbox.minx, p.x);
        double maxx = Math.max(bbox.maxx, p.x);
        double miny = Math.min(bbox.miny, p.y);
        double maxy = Math.max(bbox.maxy, p.y);
        setBoundigBox(minx, miny, maxx, maxy);
        //System.out.println("resizing bounding-box: "+minx+" "+miny+" "+maxx+" "+maxy);
    }
    
    /**
     * update the dimension of the bounding box
     *
     * @param minx,miny,maxx,maxy summits of the rectangle
     */
    public void setBoundigBox(double minx, double miny, double maxx, double maxy) {
        // update saved values
        bbox.minx = minx;
        bbox.maxx = maxx;
        bbox.miny = miny;
        bbox.maxy = maxy;

        // extend the bounding-box to surround min/max
        double centerx = (minx + maxx) / 2.;
        double centery = (miny + maxy) / 2.;
        double x_min = ((minx - centerx - 1.) * 10. + centerx);
        double x_max = ((maxx - centerx + 1.) * 10. + centerx);
        double y_min = ((miny - centery - 1.) * 10. + centery);
        double y_max = ((maxy - centery + 1.) * 10. + centery);

        // set new positions
        bbox.a.x = x_min;
        bbox.a.y = y_min;
        bbox.b.x = x_max;
        bbox.b.y = y_min;
        bbox.c.x = x_max;
        bbox.c.y = y_max;
        bbox.d.x = x_min;
        bbox.d.y = y_max;
    }
}
