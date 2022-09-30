
import DelaunayClustering.ImageCanvasDelaunayClustering;
import DelaunayClustering.InteractionDialog;
import Tools.Delaunay.DelaunayTInROI;
import Tools.Delaunay.TriangleT;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.text.TextWindow;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Vector;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Florian Levet
 */
public class DelaunayClustering_  implements PlugIn{
    public static int COLOR_1 = 0, COLOR_2 = 1, BOUNDARY = 2;
    
    protected ImagePlus m_impColor1 = null, m_impColor2 = null;
    private String[] choiceColor1, choiceColor2;
    protected Calibration m_cal = null;
    protected InteractionDialog m_id = null;

    public void run( String arg ) {
        Prefs.blackBackground = false;
        Prefs.requireControlKey = true;
        
        if (!showDialog())
            return;
        
        m_cal = m_impColor1.getCalibration();
        
        Frame frame = WindowManager.getFrame("ROI Manager");
        if(frame == null){
            System.out.println("This plugin need 2 ROIs in the ROI Manager to work.");
            return;
        }
        RoiManager roiManager = (RoiManager) frame;
        Roi[] rois = roiManager.getRoisAsArray();
        if(rois == null || rois.length < 2){
            System.out.println("This plugin need 2 ROIs in the ROI Manager to work.");
            return;
        }
        
        ArrayList <Point2D.Double > points = new ArrayList <Point2D.Double>();
        ArrayList <Integer > infos = new ArrayList <Integer>();
        
        for(int n = 1; n < 3; n++){
            FloatPolygon fp = rois[n].getFloatPolygon();
            float[] xs = fp.xpoints, ys = fp.ypoints;
            for(int i = 0; i < fp.npoints; i++){
                points.add(new Point2D.Double(xs[i], ys[i]));
                infos.add(BOUNDARY);
            }
        }
        
        Roi smallROI = null, bigROI = null;
        if(getArea(rois[1]) > getArea(rois[2])){
            bigROI = rois[1];
            smallROI = rois[2];
        }
        else{
            bigROI = rois[2];
            smallROI = rois[1];

        }
        
        roiManager.runCommand("Deselect");
        roiManager.runCommand("Delete");
        indentifyROIs(m_impColor1, points, infos, roiManager, smallROI, bigROI, COLOR_1); 
        roiManager.runCommand("Deselect");
        roiManager.runCommand("Delete");
        if(m_impColor2 != null){
            indentifyROIs(m_impColor2, points, infos, roiManager, smallROI, bigROI, COLOR_2); 
            roiManager.runCommand("Deselect");
            roiManager.runCommand("Delete");
        }
        
        Roi roisDelay[] = {bigROI, smallROI};
        DelaunayTInROI delau = new DelaunayTInROI();
        delau.executeWithInfo(points, infos, m_impColor1.getWidth(), m_impColor1.getHeight(), roisDelay);
        for(TriangleT triangle : delau.m_triangles2)
            triangle.setMarked(true);
        
        FileInfo finfo = m_impColor1.getOriginalFileInfo();
        ImagePlus impDebug = new ImagePlus( m_impColor1.getTitle(), m_impColor1.getProcessor().duplicate() );
        impDebug.setFileInfo(finfo);
        impDebug.setCalibration(m_impColor1.getCalibration());
        impDebug.show();
        ImageCanvasDelaunayClustering ic = new ImageCanvasDelaunayClustering( impDebug, delau );
        ImageWindow iw = new ImageWindow( impDebug, ic );
        iw.setVisible( true );
        
        FloatPolygon[] roisTmp = new FloatPolygon[]{smallROI.getFloatPolygon(), bigROI.getFloatPolygon()};
        ic.setOriginalROIs(roisTmp);
        
        m_id = new InteractionDialog(ic, m_impColor1.getCalibration(), BOUNDARY);
        m_id.setVisible(true);
        m_id.determinePointsSelected(Double.MAX_VALUE);
        m_id.determineClusters(Double.MAX_VALUE);
    }
    
    public void indentifyROIs(ImagePlus _imp, ArrayList <Point2D.Double > _points, ArrayList <Integer> _infos, RoiManager _roiManager, Roi _smallROI, Roi _bigROI, int _index){
        int options = ParticleAnalyzer.ADD_TO_MANAGER + ParticleAnalyzer.CLEAR_WORKSHEET;// -> 64+2048 -> CLEAR_WORKSHEET, ADD_TO_MANAGER | 2053 with the 4 of the OUTLINES
        int measurements = Measurements.AREA + Measurements.CIRCULARITY + Measurements.CENTROID;
        ResultsTable rt = Analyzer.getResultsTable();
        ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, 0., Double.POSITIVE_INFINITY, 0., Double.POSITIVE_INFINITY);
        ImagePlus useless2 = new ImagePlus("Useless", _imp.getProcessor().duplicate());
        pa.analyze(useless2);
        
        rt.show("Results");
	Frame frameResult = WindowManager.getFrame("Results");
	TextWindow win = (TextWindow)frameResult;
	win.setVisible(false);
        float[] centroidsX = rt.getColumn(rt.getColumnIndex("X"));
	float[] centroidsY = rt.getColumn(rt.getColumnIndex("Y"));
        
        Roi[] roiCells = _roiManager.getRoisAsArray();

        for(int n = 0; n < roiCells.length; n++)
            if(_bigROI.contains((int)centroidsX[n], (int)centroidsY[n]) && !_smallROI.contains((int)centroidsX[n], (int)centroidsY[n])){
                _points.add(new Point2D.Double(centroidsX[n], centroidsY[n]));
                _infos.add(_index);
            }
    }
    
    protected int getArea(Roi roi){
        ImageProcessor mask = roi.getMask();
        Rectangle r = roi.getBounds();
        if( mask == null ){
            return r.width * r.height;
        }
        int cpt = 0;
        for(int i = 0; i < mask.getWidth(); i++)
            for(int j = 0; j < mask.getHeight(); j++)
                if(mask.get(i, j) == 255)
                    cpt++;
        return cpt;
    }
    
    public boolean showDialog() {
        GenericDialog gd = new GenericDialog("Plugin");

        Vector indexForOrig = new Vector(1, 1);
        Vector indexForBin = new Vector(1, 1);
        setImageList(indexForOrig, indexForBin);

        if(choiceColor1.length < 2)
        {
            IJ.showMessage(new String("At least two images have to be opened."));
            return false;
        }
        gd.addChoice("First stack", choiceColor1, choiceColor1[0]);
        gd.addChoice("Second stack", choiceColor2, choiceColor2[1]);
        gd.addCheckbox("Use second stack", true);

        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        
        Vector cbs = gd.getCheckboxes();
        boolean useSecondColor = ((Checkbox)(cbs.get(0))).getState();

        Vector tmp = gd.getChoices();
        Choice tmp1 = (Choice)(tmp.get(0));
        Choice tmp2 = (Choice)(tmp.get(1));

        int index1 = tmp1.getSelectedIndex();
        int index2 = tmp2.getSelectedIndex();

        int ind1 = ((Integer)indexForOrig.get(index1)).intValue();
        int ind2 = ((Integer)indexForBin.get(index2)).intValue();

        m_impColor1 = WindowManager.getImage(ind1);
        m_impColor2 = useSecondColor ? WindowManager.getImage(ind2) : null;
        
        m_impColor1.getProcessor().resetRoi();
        if(m_impColor2 != null)
            m_impColor2.getProcessor().resetRoi();
        
        return true;
    }
    
    private void setImageList(Vector v1, Vector v2)
    {
        int ai[] = WindowManager.getIDList();
        if(ai == null)
            {
            IJ.error("No images are open.");
            System.exit(0);
            }
        v1.removeAllElements();
        v2.removeAllElements();
        Vector ArrayList1 = new Vector(1, 1);
        Vector ArrayList2 = new Vector(1, 1);
        for(int i = 0; i < ai.length; i++)
            {
                ImagePlus imageplus = WindowManager.getImage(ai[i]);
                String title = imageplus.getTitle();
                ArrayList1.addElement(imageplus.getTitle());
                v1.addElement(new Integer(ai[i]));
            }
        for(int i = 0; i < ai.length; i++)
            {
                ImagePlus imageplus = WindowManager.getImage(ai[i]);
                String title = imageplus.getTitle();
                ArrayList2.addElement(imageplus.getTitle());
                v2.addElement(new Integer(ai[i]));
            }

        /*if(ArrayList1.size() <= 0)
            {
            IJ.error("No 8-bits or 32-bits images are open.");
            System.exit(0);
            }*/
        choiceColor1 = new String[ArrayList1.size()];
        choiceColor2 = new String[ArrayList2.size()];

        for(int j = 0; j < ArrayList1.size(); j++)
            choiceColor1[j] = (String)ArrayList1.elementAt(j);

        for(int j = 0; j < ArrayList2.size(); j++)
            choiceColor2[j] = (String)ArrayList2.elementAt(j);
    }
}
