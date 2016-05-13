/*
 * An immutable class for a 2D point that can safely be shared among threads.
 * 
 * Author: Holger.Peine@hs-hannover.de
 * 
 */

package uebung_parallelisierung.sequentiell;

import java.io.Serializable;

public final class Point implements Serializable {
	private static final long serialVersionUID = 1L;
	public final int x, y;
	Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() { return x; }
	public int getY() { return y; }

	public final Point getNeighbor(Direction dir) {
		return new Point(x+dir.dx, y+dir.dy);
	}
	
	@Override
	public String toString() {
		return "("+x+", "+y+")";
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other.getClass() != this.getClass())
			return false;
		Point p = (Point)other;
		return x == p.x && y == p.y;
	}
	
	@Override
	public int hashCode() {
		return 3001*x+y;  // 3001 is prime
	}
}
