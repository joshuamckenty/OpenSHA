package org.opensha.commons.data.function;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Iterator;
import java.util.ListIterator;

import org.opensha.commons.data.NamedObjectAPI;
import org.opensha.commons.exceptions.Point2DException;
import org.opensha.commons.metadata.XMLSaveable;

public interface XY_DataSetAPI extends NamedObjectAPI, XMLSaveable, Serializable {

	/* ******************************/
	/* Basic Fields Getters/Setters */
	/* ******************************/

	/** Sets the name of this function. */
	public void setName( String name );

	/** Sets the info string of this function. */
	public void setInfo( String info );
	/** Returns the info of this function.  */
	public String getInfo();


	/* ******************************/
	/* Metrics about list as whole  */
	/* ******************************/

	/** returns the number of points in this function list */
	public int getNum();

	/** return the minimum x value along the x-axis */
	public double getMinX();

	/** return the maximum x value along the x-axis */
	public double getMaxX();

	/** return the minimum y value along the y-axis */
	public double getMinY();

	/** return the maximum y value along the y-axis */
	public double getMaxY();



	/* ******************/
	/* Point Accessors  */
	/* ******************/

	/** Returns the nth (x,y) point in the Function by index */
	public Point2D get(int index);

	/** Returns the x-value given an index */
	public double getX(int index);

	/** Returns the y-value given an index */
	public double getY(int index);


	/* ***************/
	/* Point Setters */
	/* ***************/

	/** Either adds a new DataPoint, or replaces an existing one, within tolerance */
	public void set(Point2D point) throws Point2DException;

	/**
	 * Creates a new DataPoint, then either adds it if it doesn't exist,
	 * or replaces an existing one, within tolerance
	 */
	public void set(double x, double y) throws Point2DException;

	/** Replaces a DataPoint y-value at the specifed index. */
	public void set(int index, double Y);



	/* **********/
	/* Queries  */
	/* **********/

	/**
	 * Determine wheither a point exists in the list,
	 * as determined by it's x-value within tolerance.
	 */
	public boolean hasPoint(Point2D point);


	/**
	 * Determine wheither a point exists in the list,
	 * as determined by it's x-value within tolerance.
	 */
	public boolean hasPoint(double x, double y);



	/* ************/
	/* Iterators  */
	/* ************/

	/**
	 * Returns an iterator over all datapoints in the list. Results returned
	 * in sorted order.
	 * @return
	 */
	public Iterator<Point2D> getPointsIterator();


	/**
	 * Returns an iterator over all x-values in the list. Results returned
	 * in sorted order.
	 * @return
	 */
	public ListIterator<Double> getXValuesIterator();


	/**
	 * Returns an iterator over all y-values in the list. Results returned
	 * in sorted order along the x-axis.
	 * @return
	 */
	public ListIterator<Double> getYValuesIterator();



	/* **************************/
	/* Standard Java Functions  */
	/* **************************/

	/**
	 * Standard java function, usually used for debugging, prints out
	 * the state of the list, such as number of points, the value of each point, etc.
	 */
	public String toString();

	/**
	 * Determines if two lists are equal. Typical implementation would verify
	 * same number of points, and the all points are equal, using the DataPoint2D
	 * equals() function.
	 */
	public boolean equals( XY_DataSetAPI function );

	/**
	 * prints out the state of the list, such as number of points,
	 * the value of each point, etc.
	 * @returns value of each point in the function in String format
	 */
	public String getMetadataString();
	
	/**
	 * This function returns a new copy of this list, including copies
	 * of all the points. A shallow clone would only create a new DiscretizedFunc
	 * instance, but would maintain a reference to the original points. <p>
	 *
	 * Since this is a clone, you can modify it without changing the original.
	 */
	public XY_DataSetAPI deepClone();

	/**
	 * It finds out whether the X values are within tolerance of an integer value
	 * @param tolerance tolerance value to consider  rounding errors
	 *
	 * @return true if all X values are within the tolerance of an integer value
	 * else returns false
	 */
	public boolean areAllXValuesInteger(double tolerance);
	
	/**
	 * Sets the name of the X Axis
	 * @param xName String
	 */
	public void setXAxisName(String xName);
	
	/**
	 * Gets the name of the X Axis
	 */
	public String getXAxisName();
	
	/**
	 * Sets the name of the X Axis
	 * @param xName String
	 */
	public void setYAxisName(String xName);
	
	/**
	 * Gets the name of the Y Axis
	 */
	public String getYAxisName();
	

}
