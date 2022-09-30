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
import Tools.GUI.Packer;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.YesNoCancelDialog;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.process.FloatPolygon;
import ij.text.TextWindow;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author Florian
 */
public class InteractionDialog extends Frame implements ActionListener{
    protected ImageCanvasDelaunayClustering m_icd = null;
    protected JTextField m_TFdistanceMax = null;
    protected JButton m_ButtonExecute = null, m_ButtonExportStats = null;
    protected JCheckBox m_checkboxTriangles = null, m_checkbox2Colors = null, m_checkboxEdges = null, m_checkboxClusters = null, m_checkboxConvexHulls = null, m_checkboxOriginalROIs = null; 
    protected Calibration m_cal = null;
    protected int m_indexBorder;
    
    public InteractionDialog( ImageCanvasDelaunayClustering _icd, Calibration _cal, int _indexBorder){
        m_icd = _icd;
        m_cal = _cal;
        m_indexBorder = _indexBorder;
        
        JPanel optionPanel = new JPanel();
        optionPanel.applyComponentOrientation( ComponentOrientation.LEFT_TO_RIGHT );
        optionPanel.setBorder( BorderFactory.createTitledBorder( "Display options" ) );
        Packer pkOption = new Packer( optionPanel );
        m_checkboxTriangles = new JCheckBox("Display whole triangulation", false);
        m_checkbox2Colors = new JCheckBox("Display 2 colors clusters", false);
        m_checkboxEdges = new JCheckBox("Display cluster edges", true);
        m_checkboxClusters = new JCheckBox("Display cluster points", true);
        m_checkboxConvexHulls = new JCheckBox("Display convex hulls", true);
        m_checkboxOriginalROIs = new JCheckBox("Display original ROIs", false);
        pkOption.pack( m_checkboxTriangles ).gridx( 0 ).gridy( 0 ).fillx().inset( 3, 3, 3, 3 );
        pkOption.pack( m_checkbox2Colors ).gridx( 0 ).gridy( 1 ).fillx().inset( 3, 3, 3, 3 );
        pkOption.pack( m_checkboxEdges ).gridx( 0 ).gridy( 2 ).fillx().inset( 3, 3, 3, 3 );
        pkOption.pack( m_checkboxClusters ).gridx( 0 ).gridy( 3 ).fillx().inset( 3, 3, 3, 3 );
        pkOption.pack( m_checkboxConvexHulls ).gridx( 0 ).gridy( 4 ).fillx().inset( 3, 3, 3, 3 );
        pkOption.pack( m_checkboxOriginalROIs ).gridx( 0 ).gridy( 5 ).fillx().inset( 3, 3, 3, 3 );
       
        JPanel analyzePanel = new JPanel();
        analyzePanel.applyComponentOrientation( ComponentOrientation.LEFT_TO_RIGHT );
        analyzePanel.setBorder( BorderFactory.createTitledBorder( "Analyze clusters" ) );
        Packer pkAnalyze = new Packer( analyzePanel );
        JLabel lblDistanceMax = new JLabel( "Distance max (" + m_cal.getXUnit() + "):" );
        m_TFdistanceMax = new JTextField("0");
        m_ButtonExecute = new JButton( "Execute" );
        m_ButtonExportStats = new JButton( "Export results" );
        pkAnalyze.pack( lblDistanceMax ).gridx( 0 ).gridy( 0 ).fillx().inset( 3, 3, 3, 3 );
        pkAnalyze.pack( m_TFdistanceMax ).gridx( 0 ).gridy( 1 ).fillx().inset( 3, 3, 3, 3 );
        pkAnalyze.pack( m_ButtonExecute ).gridx( 0 ).gridy( 2 ).fillx().inset( 3, 3, 3, 3 );
        pkAnalyze.pack( m_ButtonExportStats ).gridx( 0 ).gridy( 3 ).fillx().inset( 3, 3, 3, 3 );
    
        m_ButtonExecute.addActionListener( this );
        m_ButtonExportStats.addActionListener( this );
        m_checkboxTriangles.addActionListener( this );
        m_checkbox2Colors.addActionListener( this );
        m_checkboxEdges.addActionListener( this );
        m_checkboxClusters.addActionListener( this );
        m_checkboxConvexHulls.addActionListener( this );
        m_checkboxOriginalROIs.addActionListener( this );
        
        JPanel panel = new JPanel();
        Packer pk = new Packer( panel );
        pk.pack( optionPanel ).gridx( 0 ).gridy( 0 ).fillboth().inset( 3, 3, 3, 3 );
        pk.pack( analyzePanel ).gridx( 0 ).gridy( 1 ).fillboth().inset( 3, 3, 3, 3 );
        this.add( panel );
        this.setSize( 400, 400 );
        this.setTitle( "Interaction Dialog" );
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if( o instanceof JButton ){
            if(o == m_ButtonExecute){
                double dMax = getDistanceMax() * m_cal.pixelWidth;
                determinePointsSelected(dMax);
                determineClusters(dMax);
            }
            else if(o == m_ButtonExportStats){
                exportStats();
            }
        }
        else if(o instanceof JCheckBox){
            if(o == m_checkboxTriangles)
                m_icd.setTrianglesDisplayed(m_checkboxTriangles.isSelected());
            if(o == m_checkbox2Colors)
                m_icd.setCluster2Colors(m_checkbox2Colors.isSelected());
            if(o == m_checkboxEdges)
                m_icd.setEdgesSelected(m_checkboxEdges.isSelected());
            if(o == m_checkboxClusters)
                m_icd.setClustersSelected(m_checkboxClusters.isSelected());
            if(o == m_checkboxConvexHulls)
                m_icd.setConvexHullsSelected(m_checkboxConvexHulls.isSelected());
            if(o == m_checkboxOriginalROIs)
                m_icd.setOriginalROIsDisplayed(m_checkboxOriginalROIs.isSelected());
            m_icd.repaint();
        }
    }
    
    public double getDistanceMax(){
        return Double.parseDouble( m_TFdistanceMax.getText() );
    }
    
    public void determinePointsSelected(double _dMax){
        DelaunayTInROI delau = m_icd.getDelaunay();
        ArrayList <PointTWithInfo[]> connexions = new ArrayList <PointTWithInfo[]>();

        int cpt = 0;
        for(PointT point : delau.m_allPoints){
            PointTWithInfo pinfo = (PointTWithInfo)point;
            pinfo.setMarked(false);
            pinfo.setIndex(cpt++);
        }
        
        //HashSet <Integer> test = new HashSet <Integer>();

        for(TriangleT t : delau.m_triangles2){
            PointTWithInfo a = (PointTWithInfo)t.getA(), b = (PointTWithInfo)t.getB(), c = (PointTWithInfo)t.getC();
            boolean isBorderA = false, isBorderB = false, isBorderC = false;
            isBorderA = a.getInfo() == m_indexBorder;
            isBorderB = b.getInfo() == m_indexBorder;
            isBorderC = c.getInfo() == m_indexBorder;
            double d1 = Point2D.Double.distance(a.x, a.y, b.x, b.y);
            double d2 = Point2D.Double.distance(b.x, b.y, c.x, c.y);
            double d3 = Point2D.Double.distance(c.x, c.y, a.x, a.y);
            if(d1 < _dMax && !isBorderA && !isBorderB){
                a.setMarked(true);
                b.setMarked(true);
                PointTWithInfo[] tmp = {a, b};
                connexions.add(tmp);
            }
            if(d2 < _dMax && !isBorderB && !isBorderC){
                b.setMarked(true);
                c.setMarked(true);
                PointTWithInfo[] tmp = {b, c};
                connexions.add(tmp);
            }
            if(d3 < _dMax && !isBorderC && !isBorderA){
                c.setMarked(true);
                a.setMarked(true);
                PointTWithInfo[] tmp = {c, a};
                connexions.add(tmp);
            }
            //t.setMarked(d1 < dMax && d2 < dMax && d3 < dMax);// && (!isBorderA && !isBorderB && !isBorderC));
        }
        /*for(PointT point : delau.m_allPoints){
            PointTWithInfo pinfo = (PointTWithInfo)point;
            System.out.println("point " + pinfo.getIndex() + ", marked ? " + pinfo.isMarked());
        }
        for(PointTWithInfo[] edge : connexions){
            System.out.println("edge beween " + edge[0].getIndex() + ", marked ? " + edge[0].isMarked() + " and " + edge[1].getIndex() + ", marked ? " + edge[1].isMarked());
            test.add(edge[0].getIndex());
            test.add(edge[1].getIndex());
        }
        
        System.out.println("**************************************");
        List<Integer> list = new ArrayList<Integer>(test); 
        Collections.sort(list); 
        for (Integer s : list) {
            System.out.println(s);
        }*/
        
        m_icd.setEdges(connexions);
        m_icd.repaint();
    }
    
    public void determineClusters(double _dMax){
        DelaunayTInROI delau = m_icd.getDelaunay();
        ArrayList < PointT > points = delau.m_allPoints;
        boolean originalStatePoints[] = new boolean[points.size()];
        ArrayList < ArrayList < PointTWithInfo > > clusters = new ArrayList < ArrayList < PointTWithInfo > >();
        ArrayList < FloatPolygon > convexHulls = new ArrayList < FloatPolygon >();
        ArrayList < Color > colors = new ArrayList < Color >();
 
        Random rand = new Random();
        
        for(int n = 0; n < originalStatePoints.length; n++){
            PointTWithInfo pinfo = (PointTWithInfo)points.get(n);
            originalStatePoints[n] = pinfo.isMarked();
            
            if((!pinfo.isMarked()) && pinfo.getInfo() != 2){
                //System.out.println("point " + pinfo.getIndex() + ", marked ? " + pinfo.isMarked() + ", type = " + pinfo.getInfo());
                ArrayList < PointTWithInfo > cluster = new ArrayList < PointTWithInfo >();
                cluster.add(pinfo);
                clusters.add(cluster);
                convexHulls.add(new FloatPolygon());
                
                float r = rand.nextFloat();
                float v = rand.nextFloat();
                float b = rand.nextFloat();
                Color randomColor = new Color(r, v, b);
                colors.add(randomColor);
            }
        }
        
       
        for(PointT point : points){
            PointTWithInfo pinfo = (PointTWithInfo)point;
            if(!pinfo.isMarked()) continue;

            ArrayList < PointTWithInfo > cluster = new ArrayList < PointTWithInfo >();
            cluster.add(pinfo);
            pinfo.setMarked(false);

            for(int n = 0; n < cluster.size(); n++){
                PointTWithInfo current = cluster.get(n);
                for(TriangleT t : current.getTriangles()){
                    for(int i = 0; i < 3; i++){
                        PointTWithInfo pinfo2 = (PointTWithInfo)t.getPoint(i);
                        double d = Point2D.Double.distance(current.x, current.y, pinfo2.x, pinfo2.y);
                        if(pinfo2.isMarked() && d < _dMax){
                            cluster.add(pinfo2);
                            pinfo2.setMarked(false);
                        }
                    }
                }
            }
            clusters.add(cluster);
            
            float r = rand.nextFloat();
            float v = rand.nextFloat();
            float b = rand.nextFloat();
            Color randomColor = new Color(r, v, b);
            colors.add(randomColor);
            
            if( cluster.size() < 3){
                convexHulls.add(new FloatPolygon());
            }
            else{
                float[] xs = new float[cluster.size()], ys = new float[cluster.size()];
                int cpt = 0;
                for(PointTWithInfo p : cluster){
                    xs[cpt] = (float)p.x;
                    ys[cpt++] = (float)p.y;
                }
                PolygonRoi proi = new PolygonRoi(xs, ys, Roi.FREEROI);
                convexHulls.add(proi.getFloatConvexHull());
            }
        }
        m_icd.setClusters(clusters);
        m_icd.setConvexHulls(convexHulls);
        m_icd.setColors(colors);
        
        for(int n = 0; n < originalStatePoints.length; n++){
            PointTWithInfo pinfo = (PointTWithInfo)points.get(n);
            pinfo.setMarked(originalStatePoints[n]);
        }
        
        m_icd.repaint();
    }
    
    public void exportStats(){
        ImagePlus imp = m_icd.getImage();
        Calibration cal = imp.getCalibration();
        FileInfo finfo = imp.getOriginalFileInfo();
        String directory = finfo.directory;
        if( directory == null ){
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
            int returnVal = chooser.showOpenDialog( this );
            if( returnVal == JFileChooser.APPROVE_OPTION ){
                File file = chooser.getSelectedFile();
                directory = file.getName();
            }
            else
                return;
        }
        String generalName = imp.getTitle();
        generalName = generalName.substring( 0, generalName.lastIndexOf( "." ) );

        String nameXls = generalName + ".xls";
        boolean overwrite = false;
        if( new File( directory + nameXls ).exists() ){
            YesNoCancelDialog dialog = new YesNoCancelDialog( null, "Overwrite result file ?", "Do you want to overwrite " + nameXls );
            if( dialog.yesPressed() )
                overwrite = true;
            else if( dialog.cancelPressed() )
                overwrite = false;
        }
        else
            overwrite = true;                
        if( !overwrite ) return;
        
        ArrayList < ArrayList < PointTWithInfo > > clusters = m_icd.getClusters();
        ArrayList < FloatPolygon > convexHulls = m_icd.getConvexHulls();
        
        if( clusters == null || clusters.isEmpty() ) return;
        DecimalFormat df = new DecimalFormat( "#.###" );
        TextWindow textw = null;
        String headings = "Index\t# objects\tArea (" + cal.getXUnit() + ")\tPerimeter (" + cal.getXUnit() + ")\t# object color 1\t# objects color 2";
        String title = imp.getTitle();
        int index = title.lastIndexOf( "." );
        title = title.substring( 0, index );
        title += ".xls";
        textw = new TextWindow(title, headings, "", 1200, 800);
        textw.setVisible( true );
        for( int n = 0; n < clusters.size(); n++ ){
            ArrayList < PointTWithInfo > cluster = clusters.get(n);
            
            int nbColor1 = 0, nbColor2 = 0;
            for(PointTWithInfo p : cluster){
                if(p.getInfo() == 0)
                    nbColor1++;
                else if(p.getInfo() == 1)
                    nbColor2++;
            }
            
            String area = (cluster.size() >= 3) ? df.format(getArea(convexHulls.get(n))) : "-";
            String perimeter = "-";
            if(cluster.size() == 2)
                perimeter = df.format(getPerimeter(cluster.get(0), cluster.get(1)));
            else if(cluster.size() >= 3)
                perimeter = df.format(getPerimeter(convexHulls.get(n)));
            String s = "" + (n+1) + "\t" + ( cluster.size() ) + "\t" + area + "\t" + perimeter + "\t" + df.format(nbColor1) + "\t" + df.format(nbColor2) + "\t";
            textw.append( s );
        }
        textw.getTextPanel().saveAs( directory + nameXls );
    }
    
    public float getArea(FloatPolygon fp){
        double area = 0.;
        for( int n = 1; n <= fp.npoints; n++ ){
            float x1 = fp.xpoints[n-1], y1 = fp.ypoints[n-1], x2 = fp.xpoints[n%fp.npoints], y2 = fp.ypoints[n%fp.npoints];
            area += x1 * y2 - x2 * y1;
        }
        return (float)Math.abs( area / 2. );
    }
    
    public float getPerimeter(FloatPolygon fp){
        float perimeter = 0.f;
        for( int n = 1; n <= fp.npoints; n++ ){
            float x1 = fp.xpoints[n-1], y1 = fp.ypoints[n-1], x2 = fp.xpoints[n%fp.npoints], y2 = fp.ypoints[n%fp.npoints];
            perimeter += Point2D.Float.distance(x1, y1, x2, y2);
        }
        return perimeter;
    }
    
    public float getPerimeter(PointT p0, PointT p1){
        return (float)p0.distance(p1);
    }
}
