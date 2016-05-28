package uebung_parallelisierung.parallel;

import java.util.ArrayList;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import uebung_parallelisierung.parallel.WorkStealingSolverThread.WorkPackage;
import uebung_parallelisierung.sequentiell.Labyrinth;
import uebung_parallelisierung.sequentiell.LabyrinthSolver;
import uebung_parallelisierung.sequentiell.Point;

public class WorkStealingSolver implements LabyrinthSolver{

	public Labyrinth lab;
	public Exchanger<Point[]> solutionHandover;
	
	private ArrayList<WorkStealingSolverThread> workerThreads;
	
	private AtomicIntegerArray visited;
	
	private int nextTargetWorker;
	
	public LabyrinthPathTreeNode labyrinthPathTree;

	public void initializeDatastructure(Labyrinth labyrinth) {
		// Prepare neccessary datastructure
		this.lab = labyrinth;
		this.visited = new AtomicIntegerArray(this.lab.grid.width*this.lab.grid.height);
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
		this.nextTargetWorker = 0;
	}

	@Override
	public Point[] solve(Labyrinth lab) {
		// Initialize labyrinthPathTree with starting point
		labyrinthPathTree = new LabyrinthPathTreeNode(null, null);
		// Dispatch initial work to first thread and run thems
		WorkStealingSolverThread firstWorker = this.workerThreads.get(0);
		WorkPackage initialWork = firstWorker.generateWorkPackage(lab.grid.start, labyrinthPathTree);
		firstWorker.enqueueWork(initialWork);
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

	// Trying to equally distribute work among the workers
	public void enqueueWork(WorkPackage work) {
		/*
		WorkStealingSolverThread leastBusyThread = null;
		int todoSize = -1;
		for(WorkStealingSolverThread workerThread : this.workerThreads) {
			int currentTodoSize = workerThread.workQueue.size();
			if(todoSize == -1) {
				todoSize = currentTodoSize;
				leastBusyThread = workerThread;
			} else {
				if(currentTodoSize < todoSize) {
					todoSize = currentTodoSize;
					leastBusyThread = workerThread;
				}
			}
		}
		// Give it to the least busy thread, he should be able to handle it.
		leastBusyThread.enqueueWork(work);
		*/
		this.workerThreads.get(this.nextTargetWorker).enqueueWork(work);
		this.nextTargetWorker = (this.nextTargetWorker + 1 ) % this.workerThreads.size();
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
