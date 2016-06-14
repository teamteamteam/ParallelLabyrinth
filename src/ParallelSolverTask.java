

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class ParallelSolverTask<T> extends RecursiveTask<T> {

	private static final long serialVersionUID = 1L;
	
	private ParallelSolver dataHolder;
	
	private Labyrinth.Grid grid;
	private Point startPoint;
	private ArrayDeque<Point> pathSoFar;
	
	private Collection<ForkJoinTask<ArrayDeque<Point>>> forkedTasks;

	public ParallelSolverTask(Point startPoint, Labyrinth.Grid grid, ArrayDeque<Point> pathSoFar, ParallelSolver dataHolder) {
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
				return result;
			}
		}
		// Return their result if they made it, otherwise null.
		return null;
	}
	
	public T compute() {
		Point current = this.startPoint;
		Direction[] dirs = Direction.values(); // static data
		while (!current.equals(this.grid.end)) {
			// First, mark current field as visited!
			if(this.dataHolder.tryVisit(current)) {
				this.pathSoFar.add(current);
			} else {
				// If that failed, abort immediately.
				return (T) this.collectResults();
			}
			// Use first random unvisited neighbor as next cell, push others on the backtrack stack: 
			Point next = null;
			Collection<ForkJoinTask<ArrayDeque<Point>>> newTasks = new ArrayList<ForkJoinTask<ArrayDeque<Point>>>();
			for (Direction directionToNeighbor: dirs) {
				Point neighbor = current.getNeighbor(directionToNeighbor);
				// Fork for each direction available
				if (this.dataHolder.hasPassage(current, neighbor) && !this.dataHolder.visitedBefore(neighbor)) {
					if(next == null) {
						// I go this way
						next = neighbor;
					} else {
						// Fork for that way
						ForkJoinTask<ArrayDeque<Point>> neighbourTask = new ParallelSolverTask<ArrayDeque<Point>>(neighbor, this.grid, this.pathSoFar.clone(), this.dataHolder);
						newTasks.add(neighbourTask);
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
				// No result -> return null or so.
				return (T) this.collectResults();
			}
		}
		this.pathSoFar.addLast(current);
		 // Point[0] is only for making the return value have type Point[] (and not Object[]):
		//return pathSoFar.toArray(new Point[0]); 
		return (T) this.pathSoFar;
	}

}
