package uebung_parallelisierung.parallel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import uebung_parallelisierung.sequentiell.Direction;
import uebung_parallelisierung.sequentiell.Labyrinth.Grid;
import uebung_parallelisierung.sequentiell.Point;
import uebung_parallelisierung.sequentiell.PointAndDirection;

public class LimitedParallelSolverTask<T> extends RecursiveTask<T> {

	private static final long serialVersionUID = 1L;
	
	private LimitedParallelSolver dataHolder;
	
	private Grid grid;
	private Point startPoint;
	private ArrayDeque<Point> pathSoFar;
	
	private Collection<ForkJoinTask<ArrayDeque<Point>>> forkedTasks;

	private ArrayDeque<PointAndDirection> backtrackStack;

	public LimitedParallelSolverTask(Point startPoint, Grid grid, ArrayDeque<Point> pathSoFar, LimitedParallelSolver dataHolder) {
		this.backtrackStack = new ArrayDeque<PointAndDirection>();
		this.forkedTasks = new ArrayList<ForkJoinTask<ArrayDeque<Point>>>(); 
		this.startPoint = startPoint;
		this.grid = grid;
		this.pathSoFar = pathSoFar;
		this.dataHolder = dataHolder;
	}
	
	private ArrayDeque<Point> collectResults() {
		// I did not make it, check the others.
		for(ForkJoinTask<ArrayDeque<Point>> fjt : this.forkedTasks) {
			ArrayDeque<Point> result = fjt.join();
			if(result != null) {
				this.dataHolder.activeThreads.release();
				return result;
			}
		}
		// Return their result if they made it, otherwise null.
		this.dataHolder.activeThreads.release();
		return null;
	}
	
	public T compute() {
		Collection<ForkJoinTask<ArrayDeque<Point>>> newTasks = new ArrayList<ForkJoinTask<ArrayDeque<Point>>>();
		Point current = this.startPoint;
		Direction[] dirs = Direction.values(); // static data
		while (!current.equals(this.grid.end)) {
			// First, mark current field as visited!
			if(this.dataHolder.tryVisit(current)) {
				this.pathSoFar.add(current);
			} else {
				// If that failed try backtracking ...
				if(this.backtrackStack.isEmpty()) {
					// No result -> return null or so.
					return (T) this.collectResults();					
				}
				// Backtrack: Continue with cell saved at latest branching point:
				PointAndDirection pd = backtrackStack.pop();
				current = pd.getPoint();
				Point branchingPoint = current.getNeighbor(pd.getDirectionToBranchingPoint());
				// Remove the dead end from the top of pathSoFar, i.e. all cells after branchingPoint:
				while (!pathSoFar.peekLast().equals(branchingPoint)) {
					pathSoFar.removeLast();
				}
				continue;
			}
			// Use first random unvisited neighbor as next cell, push others on the backtrack stack: 
			Point next = null;
			newTasks.clear();
			for (Direction directionToNeighbor: dirs) {
				Point neighbor = current.getNeighbor(directionToNeighbor);
				// Fork for each direction available
				if (this.dataHolder.hasPassage(current, neighbor) && !this.dataHolder.visitedBefore(neighbor)) {
					if(next == null) {
						// I go this way
						next = neighbor;
					} else {
						// Fork for that way if possible, otherwise note for backtracking
						if(this.dataHolder.activeThreads.tryAcquire()) {
							ForkJoinTask<ArrayDeque<Point>> neighbourTask = new LimitedParallelSolverTask<ArrayDeque<Point>>(neighbor, this.grid, this.pathSoFar.clone(), this.dataHolder);
							newTasks.add(neighbourTask);
						} else {
							// Note for backtracking
							this.backtrackStack.push(new PointAndDirection(neighbor, directionToNeighbor.opposite));
						}
					}
				}
			}
			if(newTasks.size() > 0) {
				// Fork all the tasks!
				ForkJoinTask.invokeAll(newTasks);
				this.forkedTasks.addAll(newTasks);
			}
			// Advance to next cell, if any:
			if (next != null) {
				// DEBUG System.out.println("Advancing from " + current + " to " + next);
				current = next;
			} else {
				// No where to go, we did not make it! :-(
				// Try backtracking ...
				if(this.backtrackStack.isEmpty()) {
					// No result -> return null or so.
					return (T) this.collectResults();					
				}
				// Backtrack: Continue with cell saved at latest branching point:
				PointAndDirection pd = backtrackStack.pop();
				current = pd.getPoint();
				Point branchingPoint = current.getNeighbor(pd.getDirectionToBranchingPoint());
				// Remove the dead end from the top of pathSoFar, i.e. all cells after branchingPoint:
				while (!pathSoFar.peekLast().equals(branchingPoint)) {
					pathSoFar.removeLast();
				}
			}
		}
		this.pathSoFar.addLast(current);
		 // Point[0] is only for making the return value have type Point[] (and not Object[]):
		//return pathSoFar.toArray(new Point[0]); 
		return (T) this.pathSoFar;
	}

}
