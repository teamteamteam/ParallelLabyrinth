package uebung_parallelisierung.parallel;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingDeque;

import uebung_parallelisierung.sequentiell.Direction;
import uebung_parallelisierung.sequentiell.Point;

public class WorkStealingSolverThread extends Thread {

	private static int threadCounter = 0;
	
	private WorkStealingSolver dataHolder;
	public LinkedBlockingDeque<WorkPackage> workQueue;
	
	// Constructor
	public WorkStealingSolverThread(WorkStealingSolver dataHolder) {
		this.setName("WorkStealingThread #" + WorkStealingSolverThread.threadCounter);
		WorkStealingSolverThread.threadCounter++;
		this.dataHolder = dataHolder;
		this.workQueue = new LinkedBlockingDeque<WorkPackage>();
	}
	
	// Container to pass over undone work
	public class WorkPackage {
		public final ArrayDeque<Point> pathSoFar;
		public final Point next;
		public WorkPackage(Point next, ArrayDeque<Point> pathSoFar) {
			this.next = next;
			this.pathSoFar = pathSoFar;
		}
	}
	
	public WorkStealingSolverThread.WorkPackage generateWorkPackage(Point next, ArrayDeque<Point> pathSoFar) {
		return new WorkPackage(next, pathSoFar.clone());
	}
	
	public void enqueueWork(WorkPackage initialWork) {
		this.workQueue.addFirst(initialWork);
	}
	
	// Main run method of thread. Processes WorkPackages, shuts down when interrupted
	public void run() {
		while(Thread.interrupted() == false) {
			// Fetch next WorkPackage, wait if neccessary. (busy waiting right now :-/)
			WorkPackage currentWorkPackage = null;
			while(currentWorkPackage == null) {
				try {
					currentWorkPackage = this.workQueue.takeLast();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			this.process(currentWorkPackage);
		}
	}

	private void process(WorkPackage currentWorkPackage) {
		Point current = currentWorkPackage.next;
		ArrayDeque<Point> pathSoFar = currentWorkPackage.pathSoFar;  // Path from start to just before current
		while (!current.equals(this.dataHolder.lab.grid.end)) {
			Point next = null;
			if(this.dataHolder.tryVisit(current)) {
				pathSoFar.add(current);
			} else {
				// Field was already visited. Aborting this workPackage.
				return;
			}

			// Use first random unvisited neighbor as next cell, push others on the backtrack stack: 
			Direction[] dirs = Direction.values();
			for (Direction directionToNeighbor: dirs) {
				Point neighbor = current.getNeighbor(directionToNeighbor);
				if (this.dataHolder.lab.hasPassage(current, neighbor) && !this.dataHolder.visitedBefore(neighbor)) {
					if (next == null) {
						// I proceed to go this way
						next = neighbor;
					} else {
						// Everything else will be a new WorkPackage
						this.dataHolder.enqueueWork(this.generateWorkPackage(neighbor, pathSoFar));
					}
				}
			}
			// Advance to next cell, if any:
			if (next == null) {
				return;
			} else {
				current = next;
			}
		}
		// Polish up the solution by adding the last field
		pathSoFar.addLast(current);
		 // Exchange the valid solution.
		try {
			this.dataHolder.solutionHandover.exchange(pathSoFar.toArray(new Point[0]));
		} catch (InterruptedException e) {
			System.err.println(this.logMsg("I was interrupted passing over the solution. DAMN IT!"));
			e.printStackTrace();
		}
		// We're done!
		return;
	}
	
	private String logMsg(String message) {
		StringBuilder sb = new StringBuilder("[");
		sb.append(this.getName());
		sb.append("] ");
		sb.append(message);
		return sb.toString();
	}
	
}
