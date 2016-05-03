/*
 * An enum for 2D directions (north, west, south, east) represented as a bit vector, 
 * and an immutable class that packages a Point with such a direction.
 * 
 *  Author: Holger.Peine@hs-hannover.de
 *  Source of enum Direction: http://rosettacode.org/wiki/Maze#Java
 */

package uebung_parallelisierung.sequentiell;

enum Direction {
	N(1, 0, -1), S(2, 0, 1), E(4, 1, 0), W(8, -1, 0);
	final int bit;
	final int dx;
	final int dy;
	Direction opposite;

	// use the static initializer to resolve forward references
	static {
		N.opposite = S;
		S.opposite = N;
		E.opposite = W;
		W.opposite = E;
	}

	private Direction(int bit, int dx, int dy) {
		this.bit = bit;
		this.dx = dx;
		this.dy = dy;
	}
	
  @Override
	public String toString() {
		switch(this) {
			case N: return "N";
			case S: return "S";
			case W: return "W";
			case E: return "E";
			default: return "?";
		}
	}
}


final class PointAndDirection {
	final private Point point;
	public Point getPoint() {
		return point;
	}
	final private Direction directionToBranchingPoint;
	public Direction getDirectionToBranchingPoint() {
		return directionToBranchingPoint;
	}
	PointAndDirection(Point p, Direction direction) {
		this.point = p;
		directionToBranchingPoint = direction;
	}
}

