package org.opensha.commons.data;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;
import java.util.Random;

import org.junit.Test;
import org.opensha.commons.util.DataUtils.MinMaxAveTracker;

public class Point2DToleranceSortedListTest {

	private static Random rand = new Random();
	
	private static double getRandVal() {
		double val = rand.nextDouble();
		
		val = val * 40d - 20d;
		
		return val;
	}
	
	private static void addRandom(Point2DToleranceSortedList list) {
		double x = getRandVal();
		double y = getRandVal();
		Point2D pt = new Point2D.Double(x, y);
		
		list.add(pt);
	}
	
	private static void addRandom(Point2DToleranceSortedList list, int num) {
		for (int i=0; i<num; i++) {
			addRandom(list);
		}
	}
	
	private static void removeRandom(Point2DToleranceSortedList list) {
		int index = rand.nextInt(list.size());
		list.remove(index);
	}
	
	private static void removeRandom(Point2DToleranceSortedList list, int num) {
		for (int i=0; i<num; i++) {
			removeRandom(list);
		}
	}
	
	@Test
	public void testMinMaxYSimple() {
		Point2DToleranceSortedList list = new Point2DToleranceSortedList(new Point2DToleranceComparator());
		
		addRandom(list, 100);
		
		validateMinMax(list);
	}
	
	private void validateMinMax(Point2DToleranceSortedList list) {
		MinMaxAveTracker xTrack = new MinMaxAveTracker();
		MinMaxAveTracker yTrack = new MinMaxAveTracker();
		
		for (Point2D pt : list) {
			xTrack.addValue(pt.getX());
			yTrack.addValue(pt.getY());
		}
		
		assertEquals("getMinX() is wrong!", xTrack.getMin(), list.getMinX(), 0.0d);
		assertEquals("getMaxX()", xTrack.getMax(), list.getMaxX(), 0.0d);
		assertEquals("getMinX() is wrong!", yTrack.getMin(), list.getMinY(), 0.0d);
		assertEquals("getMaxX()", yTrack.getMax(), list.getMaxY(), 0.0d);
	}
	
	@Test
	public void testMinMaxYComplicated() {
		Point2DToleranceSortedList list = new Point2DToleranceSortedList(new Point2DToleranceComparator());
		
		addRandom(list, 100);
		
		validateMinMax(list);
		
		for (int i=0; i<50; i++) {
			removeRandom(list);
			validateMinMax(list);
		}
		
		for (int i=0; i<10; i++) {
			addRandom(list);
			validateMinMax(list);
		}
	}
	
	private static void validateListOrder(Point2DToleranceSortedList list) {
		Point2D prev = null;
		for (Point2D pt : list) {
			if (prev != null) {
				assertTrue("list order error: " + prev + ", " + pt, prev.getX() < pt.getX());
			}
			prev = pt;
		}
	}
	
	@Test
	public void testListOrder() {
		Point2DToleranceSortedList list = new Point2DToleranceSortedList(new Point2DToleranceComparator());
		
		for (int i=0; i<100; i++) {
			addRandom(list);
			validateListOrder(list);
		}
		
		for (int i=0; i<50; i++) {
			removeRandom(list);
			validateListOrder(list);
		}
		
		for (int i=0; i<10; i++) {
			addRandom(list);
			validateListOrder(list);
		}
	}

	
}
