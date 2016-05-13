package uebung_parallelisierung.parallel;

import java.util.ArrayDeque;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

import uebung_parallelisierung.sequentiell.Labyrinth;
import uebung_parallelisierung.sequentiell.LabyrinthSolver;
import uebung_parallelisierung.sequentiell.Point;

public class ParallelSolver implements LabyrinthSolver {

	private Labyrinth lab;
	
	private AtomicBoolean[][] visited;
	
	public ParallelSolver() {
		this.lab = null;
	}

	public void initializeDatastructure(Labyrinth lab) {
		this.lab = lab;
		this.visited = new AtomicBoolean[this.lab.grid.width][this.lab.grid.height];
		for(int i = 0; i < this.lab.grid.width; i++) {
			for(int j = 0; j < this.lab.grid.height; j++) {
				this.visited[i][j] = new AtomicBoolean(false);
			}
		}
	}
	
	@Override
	public Point[] solve(Labyrinth lab) {
		// ForkJoinTaskThreadPool bauen
		ForkJoinPool fjk = new ForkJoinPool();

		// Task initial invoken und auf Ergebnis warten
		ParallelSolverTask<ArrayDeque<Point>> initialTask = new ParallelSolverTask<ArrayDeque<Point>>(lab.grid.start, lab.grid, new ArrayDeque<Point>(), this);
		ArrayDeque<Point> result = fjk.invoke(initialTask);

		return result.toArray(new Point[0]);
	}

	public boolean hasPassage(Point current, Point neighbor) {
		return this.lab.hasPassage(current, neighbor);
	}

	public boolean tryVisit(Point current) {
		return this.visited[current.x][current.y].compareAndSet(false, true);
	}

	public boolean visitedBefore(Point neighbor) {
		return this.visited[neighbor.x][neighbor.y].get();
	}

}
