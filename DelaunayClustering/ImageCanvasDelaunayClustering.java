/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DelaunayClustering;

import Tools.Delaunay.DelaunayTInROI;
import Tools.Delaunay.PointT;
import Tools.Delaunay.PointTWithInfo;
import Tools.Delaunay.TriangleT;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author Florian
 */
public class ImageCanvasDelaunayClustering extends ImageCanvas implements KeyListener{
    protected BufferedImage offScreenImage = null;
    protected int offScreenImage_width,  offScreenImage_height;
    protected DelaunayTInROI m_delau = null;
    protected boolean m_displayEdges = true, m_displayTriangles = false, m_display2Colors = false, m_displayClusters = true, m_displayConvexHulls = true, m_displayOriginalROIs = false;
    protected boolean [] m_selectedTriangles = null;
    protected ArrayList <PointTWithInfo[]> m_edges = null;
    protected ArrayList < ArrayList < PointTWithInfo > > m_clusters = null;
    protected ArrayList < FloatPolygon > m_convexHulls = null;
    protected ArrayList < Color > m_colors = null;
    protected FloatPolygon[] m_originalROIs = null;
    protected int m_radiusSelection = 40, m_radiusDisplay = 4;
    
    public ImageCanvasDelaunayClustering( ImagePlus _imp, DelaunayTInROI _delau ){
        super( _imp );
        m_delau = _delau;
        this.addKeyListener( this );
    }
    
    @Override
    public void paint(Graphics g) {
        final int srcRectWidthMag = (int) (srcRect.width * magnification);
        final int srcRectHeightMag = (int) (srcRect.height * magnification);

        if (offScreenImage == null || offScreenImage_width != srcRectWidthMag || offScreenImage_height != srcRectHeightMag) {
            offScreenImage = new BufferedImage(srcRectWidthMag, srcRectHeightMag, BufferedImage.TYPE_INT_ARGB);//createImage(srcRectWidthMag, srcRectHeightMag);
            offScreenImage_width = srcRectWidthMag;
            offScreenImage_height = srcRectHeightMag;
        }

        Graphics2D offScreenGraphics = offScreenImage.createGraphics();//.getGraphics();
        offScreenGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        offScreenGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        super.paint(offScreenGraphics);
        Stroke s = offScreenGraphics.getStroke();
        offScreenGraphics.setStroke(new BasicStroke(1.f));
        
        drawOriginalROIs(offScreenGraphics, m_originalROIs);
        drawDelaunay(offScreenGraphics, m_delau);
        drawEdges(offScreenGraphics, m_edges);
        drawClusters(offScreenGraphics, m_clusters);
        drawConvexHulls(offScreenGraphics, m_convexHulls);
        
        g.drawImage(offScreenImage, 0, 0, this);
    }
    
    protected void drawOriginalROIs( Graphics2D g, FloatPolygon[] _rois){
        if(m_displayOriginalROIs && _rois != null){
            g.setColor(Color.yellow);
            for(FloatPolygon roi : _rois)
                drawFloatPolygon(g, roi);
        }
    }
    
    protected void drawDelaunay( Graphics2D g, DelaunayTInROI _dt) {
        if ( m_displayTriangles ) {
            g.setColor(Color.red);
            ArrayList < TriangleT > triangles = _dt.m_triangles2;
            for( int i = 0; i < triangles.size(); i++ ){
                TriangleT t = triangles.get( i );
                PointT a = t.getA(), b = t.getB(), c = t.getC();
                
                draw( g, a.x, a.y, b.x, b.y );
                draw( g, b.x, b.y, c.x, c.y );
                draw( g, c.x, c.y, a.x, a.y );
            }
            
            ArrayList < PointT > points = _dt.m_allPoints;
            for(PointT point : points)
                fill( g, point.x, point.y, m_radiusDisplay );

        }
    }
    
    protected void drawEdges( Graphics2D g, ArrayList <PointTWithInfo[]> _edges) {
        if ( m_displayEdges && _edges != null ) {
            g.setColor(Color.blue);
            for(PointT[] edge : _edges){
                draw( g, edge[0].x, edge[0].y, edge[1].x, edge[1].y );
            }
        }
    }
    
    protected void drawClusters( Graphics2D g, ArrayList < ArrayList < PointTWithInfo > > _clusters) {
        if (m_displayClusters && _clusters != null ) {
            if(m_display2Colors){
                Color c1 = Color.red, c2 = Color.green, c3 = Color.blue;
                for(ArrayList < PointTWithInfo > cluster : _clusters){
                    for(PointTWithInfo point : cluster){
                        if(point.getInfo() == 0)
                            g.setColor(c1);
                        else if(point.getInfo() == 1)
                            g.setColor(c2);
                        else if(point.getInfo() == 2)
                            g.setColor(c3);
                        fill( g, point.x, point.y, m_radiusDisplay );
                    }
                }
            }
            else{
                int cpt = 0;
                for(ArrayList < PointTWithInfo > cluster : _clusters){
                    //if(cluster.size() > 1) continue;
                    g.setColor(m_colors.get(cpt++));
                    for(PointTWithInfo point : cluster){
                        fill( g, point.x, point.y, m_radiusDisplay );
                    }
                }
            }
        }
    }
    
    protected void drawConvexHulls( Graphics2D g, ArrayList < FloatPolygon > _convexHulls) {
        if (m_displayConvexHulls && _convexHulls != null ) {
            int cpt = 0;
            //g.setColor(Color.red);
            for(FloatPolygon convexH : _convexHulls){
                g.setColor(m_colors.get(cpt++));
                drawFloatPolygon(g, convexH);
            }
        }
    }
    
    void draw(Graphics2D g, double ax, double ay, double bx, double by) {
        if (g == null) {
            ImageProcessor ip = imp.getProcessor();
            ip.drawLine((int) ax,
                    (int) ay,
                    (int) bx,
                    (int) by);
            return;
        }

        double m = magnification;
        double x0 = (ax - srcRect.x) * m;
        double y0 = (ay - srcRect.y) * m;
        double x1 = (bx - srcRect.x) * m;
        double y1 = (by - srcRect.y) * m;
        /*double x0 = screenXD((int)ax);
        double y0 = screenXD((int)ay);
        double x1 = screenXD((int)bx);
        double y1 = screenXD((int)by);*/
        //g.drawLine((int) x0, (int) y0, (int) x1, (int) y1);
        g.draw(new Line2D.Double( x0, y0, x1, y1 ) );
    }
    
    void draw(Graphics2D g, double ax, double ay, double pointRadius) {
        double m = magnification;
        double x0 = (ax - srcRect.x) * m;
        double y0 = (ay - srcRect.y) * m;
        //g.setColor(imp.getRoi().getColor());
        //g.fillOval(x - pointRadius, y - pointRadius, pointRadius + pointRadius, pointRadius + pointRadius);
        /*double x0 = screenXD((int)ax);
        double y0 = screenXD((int)ay);*/
        g.draw( new Ellipse2D.Double( x0 - pointRadius, y0 - pointRadius, pointRadius + pointRadius, pointRadius + pointRadius) );
    }
    void fill(Graphics2D g, double ax, double ay, double pointRadius) {
        double m = magnification;
        double x0 = (ax - srcRect.x) * m;
        double y0 = (ay - srcRect.y) * m;
        //g.setColor(imp.getRoi().getColor());
        //g.fillOval(x - pointRadius, y - pointRadius, pointRadius + pointRadius, pointRadius + pointRadius);
        /*double x0 = srcRect.x + screenXD((int)ax);
        double y0 = srcRect.x + screenXD((int)ay);*/
        g.fill( new Ellipse2D.Double( x0 - pointRadius, y0 - pointRadius, pointRadius + pointRadius, pointRadius + pointRadius) );
    }
    
    protected void drawFloatPolygon( Graphics2D g, FloatPolygon p ) {
        int[] xs = new int[p.npoints];
        int[] ys = new int[p.npoints];
        for (int j = 0; j < p.npoints; j++) {
            xs[j] = screenX( ( int )( p.xpoints[j] ) );
            ys[j] = screenY( ( int )( p.ypoints[j] ) );
        }
        g.draw(new Polygon(xs, ys, xs.length));
    }

    @Override
    public void keyTyped(KeyEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void keyPressed(KeyEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void keyReleased(KeyEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        String modifiers = MouseEvent.getMouseModifiersText(e.getModifiers());
        if( ((modifiers.indexOf("Ctrl") != -1) && (e.getButton() == MouseEvent.BUTTON1))){
            m_displayEdges = !m_displayEdges;
            repaint();
        }
        else
            super.mousePressed(e);
    }
    
    public void setSelectedTriangles(boolean[] _select){
        m_selectedTriangles = _select;
    }
    
    public DelaunayTInROI getDelaunay(){
        return m_delau;
    }
    
    public ArrayList <PointTWithInfo[]> getEdges(){
        return m_edges;
    }
    public void setEdges(ArrayList <PointTWithInfo[]> _edges){
        m_edges = _edges;
    }
    
    public ArrayList < ArrayList < PointTWithInfo > > getClusters(){
        return m_clusters;
    }
    public void setClusters(ArrayList < ArrayList < PointTWithInfo > > _clusters){
        m_clusters = _clusters;
    }
    
    public ArrayList < FloatPolygon > getConvexHulls(){
        return m_convexHulls;
    }
    public void setConvexHulls(ArrayList < FloatPolygon > _convexHulls){
        m_convexHulls = _convexHulls;
    }
    
    public ArrayList < Color > getColors(){
        return m_colors;
    }
    public void setColors(ArrayList < Color > _colors){
        m_colors = _colors;
    }
    
    public boolean isTrianglesDisplayed(){
        return m_displayTriangles;
    }
    public void setTrianglesDisplayed(boolean _val){
        m_displayTriangles = _val;
    }
    
    public boolean isCluster2ColorsSelected(){
        return m_display2Colors;
    }
    public void setCluster2Colors(boolean _val){
        m_display2Colors = _val;
    }
    
    public boolean isEdgesSelected(){
        return m_displayEdges;
    }
    public void setEdgesSelected(boolean _val){
        m_displayEdges = _val;
    }
    
    public boolean isClustersSelected(){
        return m_displayClusters;
    }
    public void setClustersSelected(boolean _val){
        m_displayClusters = _val;
    }
    
    public boolean isConvexHullsSelected(){
        return m_displayConvexHulls;
    }
    public void setConvexHullsSelected(boolean _val){
        m_displayConvexHulls = _val;
    }
    
    public boolean isOriginalROIsDisplayed(){
        return m_displayOriginalROIs;
    }
    public void setOriginalROIsDisplayed(boolean _val){
        m_displayOriginalROIs = _val;
    }
    
    public void setOriginalROIs(FloatPolygon[] _rois){
        m_originalROIs = _rois;
    }
}