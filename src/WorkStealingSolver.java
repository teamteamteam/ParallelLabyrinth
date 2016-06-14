

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.Exchanger;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class WorkStealingSolver implements LabyrinthSolver{

	public Labyrinth lab;
	public Exchanger<Point[]> solutionHandover;
	
	private ArrayList<WorkStealingSolverThread> workerThreads;
	
	private AtomicIntegerArray visited;
	
	public ArrayDeque<Point> labyrinthPathTree;
	
	public LinkedBlockingDeque<WorkStealingSolverThread.WorkPackage> workQueue;

	public void initializeDatastructure(Labyrinth labyrinth) {
		// Prepare neccessary datastructure
		this.lab = labyrinth;
		this.visited = new AtomicIntegerArray(this.lab.grid.width*this.lab.grid.height);
		// Create a workQueue
		this.workQueue = new LinkedBlockingDeque<WorkStealingSolverThread.WorkPackage>();
		// Create threads
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		this.workerThreads = new ArrayList<WorkStealingSolverThread>();
		for(int i = 0; i < availableProcessors; i++) {
			this.workerThreads.add(new WorkStealingSolverThread(this));
		}
		// Prepare solution-handover
		this.solutionHandover = new Exchanger<Point[]>();
		// Start the threads to be ready for work.
		for(WorkStealingSolverThread workerThread : this.workerThreads) {
			workerThread.start();
		}
	}

	@Override
	public Point[] solve(Labyrinth lab) {
		// Initialize labyrinthPathTree with starting point
		labyrinthPathTree = new ArrayDeque<Point>();
		// Dispatch initial work to first thread and run thems
		WorkStealingSolverThread firstWorker = this.workerThreads.get(0);
		WorkStealingSolverThread.WorkPackage initialWork = firstWorker.generateWorkPackage(lab.grid.start, labyrinthPathTree);
		this.enqueueWork(initialWork);
		Point[] solution = null;
		try {
			// Wait for a solution to come up. (being blocked)
			solution = this.solutionHandover.exchange(null);
		} catch (InterruptedException e) {
			System.err.println("I got interrupted waiting for the solution! Damn it!");
			e.printStackTrace();
		}
		this.shutdownNow();
		return solution;
	}
	
	private void shutdownNow() {
		for(WorkStealingSolverThread workerThread: this.workerThreads) {
			workerThread.interrupt();
		}
	}

	// Provide some work so everybody who wants one can have one.
	public void enqueueWork(WorkStealingSolverThread.WorkPackage work) {
		this.workQueue.add(work);
	}

	public boolean tryVisit(Point current) {
		int index = current.x * this.lab.grid.width + current.y;
		return this.visited.compareAndSet(index, 0, 1);
	}

	public boolean visitedBefore(Point neighbor) {
		int index = neighbor.x * this.lab.grid.width + neighbor.y;
		return (this.visited.get(index) == 1);
	}


	
}
