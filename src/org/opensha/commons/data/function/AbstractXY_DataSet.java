package org.opensha.commons.data.function;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import org.dom4j.Element;
import org.opensha.commons.exceptions.Point2DException;

public abstract class AbstractXY_DataSet implements XY_DataSetAPI {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Information about this function, will be used in making the legend from
	 * a parameter list of variables
	 */
	protected String info = "";

	/**
	 * Name of the function, useful for differentiation different instances
	 * of a function, such as in an array of functions.
	 */
	protected String name = "";
	
	/** Returns the name of this function. */
	public String getName(){ return name; }
	/** Sets the name of this function. */
	public void setName(String name){ this.name = name; }


	/** Returns the info of this function. */
	public String getInfo(){ return info; }
	/** Sets the info string of this function. */
	public void setInfo(String info){ this.info = info; }
	
	//X and Y Axis name
	private String xAxisName,yAxisName;
	
	public void setXAxisName(String xName){
		xAxisName = xName;
	}

	public String getXAxisName(){
		return xAxisName;
	}

	public void setYAxisName(String yName){
		yAxisName = yName;
	}

	public String getYAxisName(){
		return yAxisName;
	}
	
	public double getClosestX(double y) {
		double x = Double.NaN;
		double dist = Double.POSITIVE_INFINITY;
		for (int i=0; i<getNum(); i++) {
			double newY = getY(i);
			double newDist = Math.abs(newY - y);
			if (newDist < dist) {
				dist = newDist;
				x = getX(i);
			}
		}
		return x;
	}

	public double getClosestY(double x) {
		double y = Double.NaN;
		double dist = Double.POSITIVE_INFINITY;
		for (int i=0; i<getNum(); i++) {
			double newX = getX(i);
			double newDist = Math.abs(newX - x);
			if (newDist < dist) {
				dist = newDist;
				y = getY(i);
			}
		}
		return y;
	}
	
	/**
	 * It finds out whether the X values are within tolerance of an integer value
	 * @param tol tolerance value to consider  rounding errors
	 *
	 * @return true if all X values are within the tolerance of an integer value
	 * else returns false
	 */
	public boolean areAllXValuesInteger(double tolerance) {
		int num = getNum();
		double x, diff;
		for (int i = 0; i < num; ++i) {
			x = getX(i);
			diff = Math.abs(x - Math.rint(x));
			if (diff > tolerance) return false;
		}
		return true;
	}
	
	/**
	 * Returns an iterator over all x-values in the list. Results returned
	 * in sorted order. Returns null if no points present.
	 * @return
	 */
	public ListIterator<Double> getXValuesIterator(){
		ArrayList<Double> list = new ArrayList<Double>();
		for( int i = 0; i < getNum(); i++){
			list.add( new Double(this.getX(i)) );
		}
		return list.listIterator();
	}

	/**
	 * Returns an iterator over all y-values in the list. Results returned
	 * in sorted order along the x-axis. Returns null if no points present.
	 * @return
	 */
	public ListIterator<Double> getYValuesIterator(){
		ArrayList<Double> list = new ArrayList<Double>();
		for( int i = 0; i < getNum(); i++){
			list.add( new Double(this.getY(i)));
		}
		return list.listIterator();
	}

}
