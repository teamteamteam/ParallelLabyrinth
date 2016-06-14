

import java.util.ArrayDeque;

public final class LabyrinthPathTreeNode {

	private final LabyrinthPathTreeNode parent;
	private final Point point;
	
	private LabyrinthPathTreeNode[] children;
	
	public LabyrinthPathTreeNode(LabyrinthPathTreeNode parent, Point point) {
		this.parent = parent;
		this.point = point;
		this.children = new LabyrinthPathTreeNode[4];
	}
	
	// Returns the child node for further assignment
	public LabyrinthPathTreeNode addChild(LabyrinthPathTreeNode child) {
		if(this.parent == null) {
			this.children[0] = child;
		} else {
			this.children[this.getDirectionIndex(this, child)] = child;
		}
		return child;
	}
	
	private int getDirectionIndex(LabyrinthPathTreeNode parent, LabyrinthPathTreeNode child) {
		int dx = parent.point.x - child.point.x;
		int dy = parent.point.y - child.point.y;
		int directionIndex = -42;
		if(dx == 1 && dy == 0) {
			directionIndex = 1; // right
		} else if(dx == -1 && dy == 0) {
			directionIndex = 3; // left
		} else if(dx == 0 && dy == 1) {
			directionIndex = 0; // up
		} else if(dx == 0 && dy == -1) {
			directionIndex = 2; // down
		}
		if(directionIndex == -42) {
			System.err.println("Uh oh, this is impossible!");
		}
		return directionIndex;	
	}
	
	public Point[] getFullPath() {
		ArrayDeque<Point> fullPath = new ArrayDeque<Point>();
		LabyrinthPathTreeNode currentNode = this;
		do {
			fullPath.addFirst(currentNode.point);
			currentNode = currentNode.parent;
		} while (currentNode.point != null);
		return fullPath.toArray(new Point[0]);
	}
	
	public Point getPoint() {
		return this.point;
	}
	
	public LabyrinthPathTreeNode getParent() {
		return this.parent;
	}
	
}
