package uebung_parallelisierung.parallel;

import java.util.ArrayDeque;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import uebung_parallelisierung.sequentiell.Labyrinth;
import uebung_parallelisierung.sequentiell.LabyrinthSolver;
import uebung_parallelisierung.sequentiell.Point;

public class LimitedParallelSolver implements LabyrinthSolver {

	private Labyrinth lab;
	
	private AtomicIntegerArray visited;

	protected Semaphore activeThreads;
	
	private ForkJoinPool fjk;
	private LimitedParallelSolverTask<ArrayDeque<Point>> initialTask;
	
	public LimitedParallelSolver() {
		this.lab = null;
	}

	public void initializeDatastructure(Labyrinth lab) {
		this.activeThreads  = new Semaphore(Runtime.getRuntime().availableProcessors()-1);
		this.lab = lab;
		this.visited = new AtomicIntegerArray(this.lab.grid.width*this.lab.grid.height);//[this.lab.grid.width][this.lab.grid.height];
		// ForkJoinTaskThreadPool bauen
		this.fjk = new ForkJoinPool();
		// Initial task bauen
		this.initialTask = new LimitedParallelSolverTask<ArrayDeque<Point>>(lab.grid.start, lab.grid, new ArrayDeque<Point>(), this);

	}
	
	@Override
	public Point[] solve(Labyrinth lab) {
		// Task initial invoken und auf Ergebnis warten
		ArrayDeque<Point> result = this.fjk.invoke(this.initialTask);
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
