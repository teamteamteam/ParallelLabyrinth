/*
 * An enum for 2D directions (north, west, south, east) represented as a bit vector, 
 * and an immutable class that packages a Point with such a direction.
 * 
 *  Author: Holger.Peine@hs-hannover.de
 *  Source of enum Direction: http://rosettacode.org/wiki/Maze#Java
 */

package uebung_parallelisierung.sequentiell;

public final class PointAndDirection {
	
	final private Point point;
	
	public Point getPoint() {
		return point;
	}
	
	final private Direction directionToBranchingPoint;
	
	public Direction getDirectionToBranchingPoint() {
		return directionToBranchingPoint;
	}
	
	public PointAndDirection(Point p, Direction direction) {
		this.point = p;
		directionToBranchingPoint = direction;
	}
}

