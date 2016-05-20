package uebung_parallelisierung.parallel;

import java.util.ArrayDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicIntegerArray;

import uebung_parallelisierung.sequentiell.Labyrinth;
import uebung_parallelisierung.sequentiell.LabyrinthSolver;
import uebung_parallelisierung.sequentiell.Point;

public class LimitedParallelSolver implements LabyrinthSolver {

	private Labyrinth lab;
	
	private AtomicIntegerArray visited;

	protected Semaphore activeThreads;
	protected int maxThreads;
	
	protected ForkJoinPool fjk;
	private ForkJoinTask<ArrayDeque<Point>> initialTask;
	
	public LimitedParallelSolver() {
		this.lab = null;
	}

	public void initializeDatastructure(Labyrinth lab) {
		this.maxThreads = Runtime.getRuntime().availableProcessors() - 1;
		this.activeThreads  = new Semaphore(this.maxThreads);
		this.lab = lab;
		this.visited = new AtomicIntegerArray(this.lab.grid.width*this.lab.grid.height);
		this.fjk = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
		this.initialTask = new LimitedParallelSolverTask<ArrayDeque<Point>>(lab.grid.start, lab.grid, new ArrayDeque<Point>(), this);
	}
	
	@Override
	public Point[] solve(Labyrinth lab) {
		// Task initial starten und auf Ergebnis warten
		this.fjk.execute(this.initialTask);
		while(this.initialTask.isDone() == false) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException ie) {
				System.err.println(ie);
			}
			System.out.println("besetzt Semaphore: " + (this.maxThreads - this.activeThreads.availablePermits()) + " | " + this.fjk);
		}
		ArrayDeque<Point> result = null;
		try {
			result = initialTask.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}	
		return result.toArray(new Point[0]);
	}

	public boolean hasPassage(Point current, Point neighbor) {
		return this.lab.hasPassage(current, neighbor);
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
