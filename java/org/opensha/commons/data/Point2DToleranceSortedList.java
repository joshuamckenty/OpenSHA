package org.opensha.commons.data;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.TreeSet;

public class Point2DToleranceSortedList extends TreeSet<Point2D> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private double minY = Double.NaN;
	private double maxY = Double.NaN;
	
	public Point2DToleranceSortedList(Point2DComparatorAPI comparator) {
		super(comparator);
	}
	
	public double getTolerance() {
		return ((Point2DComparatorAPI)comparator()).getTolerance();
	}
	
	public void setTolerance(double newTolerance) {
		((Point2DComparatorAPI)comparator()).setTolerance(newTolerance);
	}
	
	private void checkMinMaxY(Point2D p) {
		double y = p.getY();
		if (Double.isNaN(minY) || y < minY)
			minY = y;
		if (Double.isNaN(maxY) || y > maxY)
			maxY = y;
	}
	
	protected void recalcMinMaxYs() {
		minY = Double.NaN;
		maxY = Double.NaN;
		for (Point2D p : this) {
			checkMinMaxY(p);
		}
	}

	@Override
	public boolean add(Point2D e) {
		checkMinMaxY(e);
		return super.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends Point2D> c) {
		boolean ret = super.addAll(c);
		if (ret)
			recalcMinMaxYs();
		return ret;
	}

	@Override
	public boolean remove(Object obj) {
		boolean ret = super.remove(obj);
		if (ret)
			recalcMinMaxYs();
		return ret;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean ret = super.removeAll(c);
		if (ret)
			recalcMinMaxYs();
		return ret;
	}
	
	public double getMinY() {
		return minY;
	}
	
	public double getMaxY() {
		return maxY;
	}
	
	public double getMinX() {
		return this.first().getX();
	}
	
	public double getMaxX() {
		return this.last().getX();
	}
	
	public Point2D get(int index) {
		if (index > size())
			throw new IndexOutOfBoundsException();
		int cnt = 0;
		for (Point2D p : this) {
			if (cnt == index)
				return p;
			cnt++;
		}
		return null; // unreachable
	}
	
	public boolean remove(int index) {
		if (index > size())
			throw new IndexOutOfBoundsException();
		int cnt = 0;
		for (Point2D p : this) {
			if (cnt == index)
				return remove(p);
			cnt++;
		}
		return false; // unreachable
	}
	
	public Point2D get(double x) {
		Point2D findPoint = new Point2D.Double(x,0.0);
		for (Point2D p : this) {
			if(comparator().compare(p, findPoint) == 0)
				return p;
		}
		return null;
	}
	
	public int getIndex(Point2D findPoint) {
		int cnt = 0;
		for (Point2D p : this) {
			if(comparator().compare(p, findPoint) == 0)
				return cnt;
			cnt++;
		}
		return -1;
	}

}
