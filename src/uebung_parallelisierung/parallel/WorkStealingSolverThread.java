package uebung_parallelisierung.parallel;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingDeque;

import uebung_parallelisierung.sequentiell.Direction;
import uebung_parallelisierung.sequentiell.Point;
import uebung_parallelisierung.sequentiell.PointAndDirection;

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
		public final LabyrinthPathTreeNode pathSoFar;
		public final Point next;
		public WorkPackage(Point next, LabyrinthPathTreeNode pathSoFar) {
			this.next = next;
			this.pathSoFar = pathSoFar;
		}
	}
	
	public WorkStealingSolverThread.WorkPackage generateWorkPackage(Point next, LabyrinthPathTreeNode pathSoFar) {
		return new WorkPackage(next, pathSoFar);
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
					//System.out.println(this.logMsg("Elements in local workQueue: " + this.workQueue.size()));
					// Try local queue first
					currentWorkPackage = this.workQueue.removeLast();
				} catch (NoSuchElementException e) {
					try {
						// If that did not work wait on global workQueue for work to show up.
						currentWorkPackage = this.dataHolder.workQueue.takeLast();
					} catch(InterruptedException ie) {
						// Probably i have to stop.
						return;
					}
				}
			}
			this.process(currentWorkPackage);
		}
	}

	private void process(WorkPackage currentWorkPackage) {
		Point current = currentWorkPackage.next;
		LabyrinthPathTreeNode pathSoFar = currentWorkPackage.pathSoFar;  // Path from start to just before current
		ArrayDeque<PointAndDirection> backtrackStack = new ArrayDeque<PointAndDirection>(); // Backtracking is still a thing
		while (!current.equals(this.dataHolder.lab.grid.end)) {
			Point next = null;
			if(this.dataHolder.tryVisit(current)) {
				pathSoFar = pathSoFar.addChild(new LabyrinthPathTreeNode(pathSoFar, current));
			} else {
				// Do backtracking
				if (backtrackStack.isEmpty()) {
					return; // No more backtracking avaible: No solution exists on this work package.
				}
				// Backtrack: Continue with cell saved at latest branching point:
				PointAndDirection pd = backtrackStack.pop();
				current = pd.getPoint();
				Point branchingPoint = current.getNeighbor(pd.getDirectionToBranchingPoint());
				// Remove the dead end from the top of pathSoFar, i.e. all cells after branchingPoint:
				while (!pathSoFar.getPoint().equals(branchingPoint)) {
					// DEBUG System.out.println("    Going back before " + pathSoFar.peekLast());
					pathSoFar = pathSoFar.getParent();
				}
				continue; // This is important! We have to visit the new current field again!
			}
			// Use first random unvisited neighbor as next cell, push others on the backtrack stack: 
			Direction[] dirs = Direction.values();
			for (Direction directionToNeighbor: dirs) {
				Point neighbor = current.getNeighbor(directionToNeighbor);
				if (this.dataHolder.lab.hasPassage(current, neighbor) && !this.dataHolder.visitedBefore(neighbor)) {
					boolean queueWorkGlobally = this.dataHolder.workQueue.size() < 3; // This can be fine-tuned to determine when to dispatch work elsewhere.
					if (next == null) {
						// I proceed to go this way
						next = neighbor;
					} else {
						// Either backtrack or create a new WorkPackage
						if(queueWorkGlobally) {
							this.dataHolder.enqueueWork(this.generateWorkPackage(neighbor, pathSoFar));							
						} else {
							backtrackStack.push(new PointAndDirection(neighbor, directionToNeighbor.opposite));
						}
					}
				}
			}
			// Advance to next cell, if any:
			if (next == null) {
				// Do backtracking
				if (backtrackStack.isEmpty()) {
					return; // No more backtracking avaible: No solution exists on this work package.
				}
				// Backtrack: Continue with cell saved at latest branching point:
				PointAndDirection pd = backtrackStack.pop();
				current = pd.getPoint();
				Point branchingPoint = current.getNeighbor(pd.getDirectionToBranchingPoint());
				// Remove the dead end from the top of pathSoFar, i.e. all cells after branchingPoint:
				while (!pathSoFar.getPoint().equals(branchingPoint)) {
					// DEBUG System.out.println("    Going back before " + pathSoFar.peekLast());
					pathSoFar = pathSoFar.getParent();
				}
				continue; // This is important! We have to visit the new current field again!
			} else {
				current = next;
			}
		}
		// Polish up the solution by adding the last field
		pathSoFar = pathSoFar.addChild(new LabyrinthPathTreeNode(pathSoFar, current));
		 // Exchange the valid solution.
		try {
			this.dataHolder.solutionHandover.exchange(pathSoFar.getFullPath());
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
