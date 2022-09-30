/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tools.Delaunay;

import java.awt.geom.Point2D;

/**
 *
 * @author Florian
 */
public class PointTWithInfo extends PointT {
    public int m_info = 0, m_index = 0;
    public boolean m_marked = true;
    
    public PointTWithInfo( double _x, double _y, int _info ){
        super( _x, _y );
        m_info = _info;
    }
    public PointTWithInfo( double _x, double _y ){
        super( _x, _y );
    }
    public PointTWithInfo( Point2D.Double _p ){
        super( _p.x, _p.y );
    }
    public PointTWithInfo( PointT _p ){
        super(_p);
    }
    
    public int getInfo(){
        return m_info;
    }
    
    public void setInfo(int _info){
        m_info = _info;
    }
    
    public int getIndex(){
        return m_index;
    }
    
    public void setIndex(int _index){
        m_index = _index;
    }
    
    public boolean isMarked(){
        return m_marked;
    }
    
    public void setMarked(boolean _marked){
        m_marked = _marked;
    }
}
