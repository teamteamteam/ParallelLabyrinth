package uebung_parallelisierung.sequentiell;
/*
 * Generate a labyrinth using the depth-first algorithm
 * (www.astrolog.org/labyrnth/algrithm.htm), display it unless too large
 * (as ASCII graphics and using Swing graphics), find a solution (using depth 
 * first search) and display the solution, again unless too large, as a list of 
 * 2D coordinates and using Swing graphics.
 * 
 * Author: Holger.Peine@hs-hannover.de
 * 
 * Source of labyrinth representation and ASCII output generation:
 * http://rosettacode.org/wiki/Maze#Java
 * 
 */



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.plaf.synth.SynthSplitPaneUI;

import uebung_parallelisierung.parallel.LimitedParallelSolver;
import uebung_parallelisierung.parallel.ParallelSolver;
import uebung_parallelisierung.parallel.WorkStealingSolver;



final public class Labyrinth extends JPanel  {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static class Grid implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * Serialized state of a labyrinth with size, passages, start and end
		 * (without search state).
		 * This is only a separate class in order to easily (de)serialize
		 * the state of the labyrinth. In all other respects, it should be
		 * considered a part of class Labyrinth (which is also why its 
		 * attributes are not private).
		 */
		
		public final int width;  // total number of cells in x direction
		public final int height;  // total number of cells in y direction
		public final Point start;
		public final Point end;
		
		final byte[][] passages;
		/*		
		 *  Each array element represents a cell in the labyrinth with the passages possible from 
		 *  this cell. Its four least significant bits are interpreted as one flag for each direction
		 *  (see enum Direction for which bit means which direction) indicating whether 
		 *  there is a passage from this cell in that direction (note that passages
		 *  and walls are not cells, but represented indirectly by these flags).
		 *  Initially all cells are 0, i.e. have no passage from them (i.e. surrounded
		 *  by walls on all their four sides). Note that two-way passages appear as opposite
		 *  bits in both the source and destination cell; thus, this data structure supports
		 *  one-way passages, too, by setting a bit in the source cell only.
		 */	
		
		public Grid(int width, int height, Point start, Point end) {
			this.width = width;
			this.height = height;
			this.start = start;
			this.end = end;
			
			passages = new byte[width][height]; // initially all 0 (see comment at declaration of passages)
		}
	}

	private static final int  CELL_PX = 10;  // width and length of the labyrinth cells in pixels
	private static final int  HALF_WALL_PX = 2;  // thickness/2 of the labyrinth walls in pixels
	// labyrinths with more pixels than this (in one or both directions) will not be graphically displayed:
	private static final int MAX_PX_TO_DISPLAY = 1000;
	// When generating the labyrinth and considering whether to create a passage to some neighbor cell, create a 
	// passage to a cell already that is accessible on another path (i.e. create a cycle) with this probability:
	private static final double CYCLE_CREATION_PROBABILITY = 0.01;
	
	// The default size of the labyrinth (i.e. unless program is invoked with size arguments):
	private static final int DEFAULT_WIDTH_IN_CELLS = 5000;
	private static final int DEFAULT_HEIGHT_IN_CELLS = 5000;
	
	public final Grid grid;
	
	// For each cell in the labyrinth: Has solve() visited it yet?
	private final boolean[][] visited; 
	
	private Point[] solution = null; // set to solution path once that has been computed

	public Labyrinth(Grid grid) {
		this.grid = grid;
		visited = new boolean[grid.width][grid.height]; // initially all false
		generate();
	}
	
    public Labyrinth(int width, int height, Point start, Point end) {
    	this(new Grid(width, height, start, end));
	}

/**
 * Generate a labyrinth (with or without cycles, depending on CYCLE_CREATION_PROBABILITY)
 * using the depth-first algorithm (www.astrolog.org/labyrnth/algrithm.htm (sic!)) 
*/
	

	private void generate() {
		ArrayDeque<Point> pointsToDo = new ArrayDeque<Point>();
		Point current;
		pointsToDo.push(grid.start);
		while (!pointsToDo.isEmpty()) {
			current = pointsToDo.pop();
			int cx = current.getX();
			int cy = current.getY();
			Direction[] dirs = Direction.values();
			Collections.shuffle(Arrays.asList(dirs));
			// For all unvisited neighboring cells in random order: 
			// Make a passage from the current cell to that neighbor
			for (Direction dir : dirs) {
				// Pick random neighbor of current cell as new cell (nx, ny)
				Point neighbor = current.getNeighbor(dir);
				int nx = neighbor.getX();
				int ny = neighbor.getY();
	
				if (contains(neighbor) // If neighbor is still in the labyrinth ...
						&& 	(	 grid.passages[nx][ny] == 0 // ... and has no passage yet, i.e. has not been visited yet during generation
							  || Math.random() < CYCLE_CREATION_PROBABILITY )) {  // ... or creating a cycle is OK 

					// Make a two-way passage, i.e. from current to neighbor and from neighbor to current:
					grid.passages[cx][cy] |= dir.bit;
					grid.passages[nx][ny] |= dir.opposite.bit;

					// Remember to continue from this neighbor later on
					pointsToDo.push(neighbor);
				}
			}
		}
	}
	
	private boolean contains(Point p) {
		return 0 <= p.getX() && p.getX() < grid.width && 
			   0 <= p.getY() && p.getY() < grid.height;
	}

	public boolean hasPassage(Point from, Point to) {
		if (!contains(from) ||  !contains(to)) {
			return false;
		}
		if (from.getNeighbor(Direction.N).equals(to))
			return (grid.passages[from.getX()][from.getY()] & Direction.N.bit) != 0;
		if (from.getNeighbor(Direction.S).equals(to))
			return (grid.passages[from.getX()][from.getY()] & Direction.S.bit) != 0;
		if (from.getNeighbor(Direction.E).equals(to))
			return (grid.passages[from.getX()][from.getY()] & Direction.E.bit) != 0;
		if (from.getNeighbor(Direction.W).equals(to))
			return (grid.passages[from.getX()][from.getY()] & Direction.W.bit) != 0;
		return false;  // To suppress warning about undefined return value
	}

	public boolean visitedBefore(Point p) {
		boolean result = visited[p.getX()][p.getY()];
		// DEBUG
//		if (result)
//			System.out.println("Node " + p + " already visited.");
		return result;
	}

	public void visit(Point p) {
		// DEBUG System.out.println("Visiting " + p);
		visited[p.getX()][p.getY()] = true;
	}

	private boolean checkSolution() {
		Point from = solution[0];
		if (!from.equals(grid.start)) {
			System.out.println("checkSolution fails because the first cell is" + from + ", but not  " + grid.start);
			return false;
		}

		for (int i = 1; i < solution.length; ++i) {
			Point to = solution[i];
			if (!hasPassage(from, to)) {
				System.out.println("checkSolution fails because there is no passage from " + from + " to " + to);
				return false;
			}
			from = to;
		}
		if (!from.equals(grid.end)) {
			System.out.println("checkSolution fails because the last cell is" + from + ", but not  " + grid.end);
			return false;
		}
		return true;
	}

	/**
	 * @return Returns a path through the labyrinth from start to end as an array, or null if no solution exists
	 */
	public Point[] solve(LabyrinthSolver labsolver) {
		return labsolver.solve(this);
	}
	
	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		display(graphics);
	}
	
	public void print() {
		for (int i = 0; i < grid.height; i++) {
			// draw the north edges
			for (int j = 0; j < grid.width; j++) {
				System.out.print((grid.passages[j][i] & Direction.N.bit) == 0 ? "+---" : "+   ");
			}
			System.out.println("+");
			// draw the west edges
			for (int j = 0; j < grid.width; j++) {
				System.out.print((grid.passages[j][i] & Direction.W.bit) == 0 ? "|   " : "    ");
			}
			// draw the far east edge
			System.out.println("|");
		}
		// draw the bottom line
		for (int j = 0; j < grid.width; j++) {
			System.out.print("+---");
		}
		System.out.println("+");
	}
	
	private boolean smallEnoughToDisplay() {
		return grid.width*CELL_PX <= MAX_PX_TO_DISPLAY && grid.height*CELL_PX <= MAX_PX_TO_DISPLAY;
	}

	public void display(Graphics graphics) {
		// draw white background
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, grid.width*CELL_PX, grid.height*CELL_PX);
		
		// draw solution path, if available
		if (solution  != null) {
			graphics.setColor(Color.YELLOW);
			for (Point p: solution)
/*				// fill only white area between the walls instead of whole cell:
				graphics.fillRect(p.getX()*CELL_PX+HALF_WALL_PX, p.getY()*CELL_PX+HALF_WALL_PX, 
											CELL_PX-2*HALF_WALL_PX, CELL_PX-2*HALF_WALL_PX); 
*/			
				graphics.fillRect(p.getX()*CELL_PX, p.getY()*CELL_PX, 	CELL_PX, CELL_PX); 
		}
		
		// draw start and end cell in special colors (covering start and end cell of the solution path)
		graphics.setColor(Color.RED);
		graphics.fillRect(grid.start.getX()*CELL_PX, grid.start.getY()*CELL_PX, CELL_PX, CELL_PX);
		graphics.setColor(Color.GREEN);
		graphics.fillRect(grid.end.getX()*CELL_PX, grid.end.getY()*CELL_PX, CELL_PX, CELL_PX);
		
		// draw black walls (covering part of the solution path)
		graphics.setColor(Color.BLACK);
		for(int x = 0; x < grid.width; ++x) {
			for(int y = 0; y < grid.height; ++y) {
				// draw north edge of each cell (together with south edge of cell above)
				if ((grid.passages[x][y] & Direction.N.bit) == 0)
					// y-HALF_WALL_PX will be half out of labyrinth for x==0 row, 
					// but that does not hurt the picture thanks to automatic cropping
					graphics.fillRect(x*CELL_PX, y*CELL_PX-HALF_WALL_PX, CELL_PX, 2*HALF_WALL_PX);
				// draw west edge of each cell (together with east edge of cell to the left)
				if ((grid.passages[x][y] & Direction.W.bit) == 0)
					// x-HALF_WALL_PX will be half out of labyrinth for y==0 column, 
					// but that does not hurt the picture thanks to automatic cropping
					graphics.fillRect(x*CELL_PX-HALF_WALL_PX, y*CELL_PX, 2*HALF_WALL_PX, CELL_PX);
			}
		}
		// draw east edge of labyrinth
		graphics.fillRect(grid.width*CELL_PX, 0, HALF_WALL_PX, grid.height*CELL_PX);
		// draw south edge of labyrinth
		graphics.fillRect(0, grid.height*CELL_PX-HALF_WALL_PX, grid.width*CELL_PX, HALF_WALL_PX);		
	}

	public void printSolution() {
		System.out.print("Solution: ");
		for (Point p: solution)
			System.out.print(p);
		System.out.println();
	}
	
	public void displaySolution(JFrame frame) {
		repaint();
}
	
private static Labyrinth makeAndSaveLabyrinth(String[] args) {
	
	// Construct labyrinth: Either read it from a file, or create a new one
	if (args.length >= 1 && args[0].endsWith(".ser")) {  
		
		// 1st argument is name of file with serialized labyrinth: Ignore other arguments
		// and create labyrinth from that file:
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(args[0]));
			Grid grid = (Grid)ois.readObject();
			ois.close();
			Labyrinth labyrinth = new Labyrinth(grid);
			return labyrinth;
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	} else {
		// Create new, random labyrinth:
		
		int width = args.length >= 1 ? (Integer.parseInt(args[0])) : DEFAULT_WIDTH_IN_CELLS;
		int height = args.length >= 2 ? (Integer.parseInt(args[1])) : DEFAULT_HEIGHT_IN_CELLS;
		
		Point start = new Point(width/2, height/2);

		// Randomly pick one of the four corners as the end point:
		int zeroToThree = (int)(4.0*Math.random());
		Point end = new Point(zeroToThree / 2 == 0 ? 0 : width-1, 
							  zeroToThree % 2 == 0 ? 0 : height-1);

		Labyrinth labyrinth = new Labyrinth(width, height, start, end);

		// Save to file (may be reused in future program executions):
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("grid.ser"));
			oos.writeObject(labyrinth.grid);
			oos.close();
		} catch (Exception e) {
			System.out.println(e);
		}
			
		return labyrinth;
	}
}
	
/**
 * 
 * @param args If the first argument is a file name ending in .ser, the serialized labyrinth in that file
 * is used; else the first two arguments are optional numbers giving the width and height of a new
 * labyrinth to be constructed.
 */
	public static void main(String[] inputArgs) {
		JFrame frame = null;
		
		String solverName = inputArgs[0];
		// remove first param and pass rest on to programm
		String[] args = new String[inputArgs.length-1];
		for(int i = 1; i < inputArgs.length; i++) {
			args[i-1] = inputArgs[i];
		}

		Labyrinth labyrinth = makeAndSaveLabyrinth(args);
		System.out.println("Labyrinth dimensions: " + labyrinth.grid.width + "x" + labyrinth.grid.height);

		// Build the right solver.
		LabyrinthSolver solver = null;
		if(solverName.equals("seq")) {
			solver = new NonParallelSolver();
		} else if(solverName.equals("par")) {
			ParallelSolver p = new ParallelSolver();
			p.initializeDatastructure(labyrinth);
			solver = p;
		} else if(solverName.equals("parlim")) {
			LimitedParallelSolver p = new LimitedParallelSolver();
			p.initializeDatastructure(labyrinth);
			solver = p;
		} else if(solverName.equals("thread")) {
				WorkStealingSolver p = new WorkStealingSolver();
				p.initializeDatastructure(labyrinth);
				solver = p;
			}		
		if (labyrinth.smallEnoughToDisplay()) {
			frame = new JFrame("Sequential labyrinth solver");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			// TODO: Window is initially displayed somewhat smaller than
			// the indicated frame size, therefore use width+5 and height+5:			
			frame.setSize((labyrinth.grid.width+5)*CELL_PX, (labyrinth.grid.height+5)*CELL_PX);
			
			// Put a scroll pane around the labyrinth frame if the latter is too large
			// (by Joern Lenselink)
			Dimension displayDimens = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
			Dimension labyrinthDimens = frame.getSize();
			if(labyrinthDimens.height > displayDimens.height) {
				JScrollPane scroll = new JScrollPane();
				labyrinth.setBackground(Color.LIGHT_GRAY);
				frame.getContentPane().add(scroll);
				JPanel borderlayoutpanel = new JPanel();
				borderlayoutpanel.setBackground(Color.darkGray);
				scroll.setViewportView(borderlayoutpanel);
				borderlayoutpanel.setLayout(new BorderLayout(0, 0));
				
				JPanel columnpanel = new JPanel();
				borderlayoutpanel.add(columnpanel, BorderLayout.NORTH);
				columnpanel.setLayout(new GridLayout(0, 1, 0, 1));
				columnpanel.setOpaque(false);
				columnpanel.setBackground(Color.darkGray);
				
				columnpanel.setSize(labyrinthDimens.getSize());
				columnpanel.setPreferredSize(labyrinthDimens.getSize());
				columnpanel.add(labyrinth);
			} else {
				// No scroll pane needed:
				frame.getContentPane().add(labyrinth);
			}
			
			frame.setVisible(true); // will draw the labyrinth (without solution)
			labyrinth.print();
		}
		System.out.println("Press enter to start");
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long startTime = System.currentTimeMillis();		
		labyrinth.solution = labyrinth.solve(solver);
		long endTime = System.currentTimeMillis();
		System.out.println("Computed sequential solution of length " + labyrinth.solution.length + " to labyrinth of size " + 
				labyrinth.grid.width + "x" + labyrinth.grid.height + " in " + (endTime - startTime) + "ms.");
		
		if (labyrinth.smallEnoughToDisplay()) {
			labyrinth.displaySolution(frame);
		    labyrinth.printSolution();
		}

		if (labyrinth.checkSolution())
			System.out.println("Solution correct :-)"); 
		else
			System.out.println("Solution incorrect :-(");
		System.out.println(solverName);
	}
}