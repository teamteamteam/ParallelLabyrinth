
package uebung_parallelisierung.sequentiell;

public enum Direction {
	N(1, 0, -1), S(2, 0, 1), E(4, 1, 0), W(8, -1, 0);
	final int bit;
	final int dx;
	final int dy;
	public Direction opposite;

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
