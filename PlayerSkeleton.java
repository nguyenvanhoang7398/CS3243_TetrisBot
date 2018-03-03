import java.util.*;
import java.lang.*;

// Helper class
class Helper {
	public static int[] clone1DArr(int[] arr) {
		int arrSize = arr.length;
		int[] newArr = new int[arrSize];

		for (int i=0; i<arrSize; i++) {
			newArr[i] = arr[i];
		}

		return newArr;
	}

	public static int[][] clone2DArr(int[][] arr) {
		int numRow = arr.length;
		int numCol = arr[0].length;
		int[][] newArr = new int[numRow][numCol];

		for (int r=0; r<numRow; r++) {
			newArr[r] = clone1DArr(arr[r]);
		}

		return newArr;
	}

	public static void printArr(int[] arr) {
		for (int i=0; i<arr.length; i++) {
			System.out.print(arr[i] + " ");
		}
		System.out.println();
	}

	public static void print2DArr(int[][] arr) {
		for (int i=0; i<arr.length; i++) {
			printArr(arr[i]);
		}
	}
}

public class PlayerSkeleton {

	public static final int FEATURE_NUMBER = 4;

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		double benchmark = -100000;
		double maxUtility = -Double.MAX_VALUE;
		int move = -1;
		for (int j = 0; j < legalMoves.length; j++) {
			double utility = getUtility(s,j);
			if (utility > benchmark && utility > maxUtility) {
				move = j;
				maxUtility = utility;
			}
		}
		return move;
	}

	public int getHoles(State s)
	{
		int result = 0;
		for (int i = 0; i < State.COLS; i++)
		{
			for (int j = 0; j < State.ROWS; j++) 
				if (s.getField()[j][i] == 0 && j < s.getTop()[i]) result++;
		}
		return result;
	}

	public double getUtility(State s, int move)
	{
		AuxState next = new AuxState(s);
		// System.out.println("S's field ");
		// Helper.print2DArr(s.getField());
		next.makeMove(move);
		double[] weightFeat = new double[FEATURE_NUMBER]; //weights of features
		weightFeat[0] = 10; //weight for number of rows cleared
		weightFeat[1] = -3; //weight for height of map
		weightFeat[3] = -2;  //weight for number of holes
		weightFeat[2] = -10; //weight for "even-ness" of top height
		double[] feats = new double[FEATURE_NUMBER]; //actual features
		feats[0] = next.getRowsCleared() - s.getRowsCleared(); //number of rows cleared
		feats[1] = getHoles(next); //number of holes
		int[] topS = s.getTop().clone();
		int[] topN = next.getTop();
		int hs = -1, hn = -1, ls = 30, ln = 30;
		for (int i = 0; i < State.COLS; i++) 
		{
			hs = Math.max(hs,topS[i]);
			hn = Math.max(hn,topN[i]);
			ls = Math.min(ls,topS[i]);
			ln = Math.min(ln,topN[i]);
		}
		feats[2] = hn-hs; //change in height
		feats[3] = hn-ln; //"even-ness" of the next map
		double result = 0;
		for (int i = 0; i < FEATURE_NUMBER; i++) 
			result += feats[i]*weightFeat[i];
		return result;
	}

	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
}

class AuxState extends State {
	//current turn
	private int turn = 0;
	private int cleared = 0;
	
	//each square in the grid - int means empty - other values mean the turn it was placed
	private int[][] field = new int[ROWS][COLS];
	//top row+1 of each column
	//0 means empty
	private int[] top = new int[COLS];
	
	
	//number of next piece
	protected int nextPiece;
	
	
	//all legal moves - first index is piece type - then a list of 2-length arrays
	protected static int[][][] legalMoves = new int[N_PIECES][][];
	
	//possible orientations for a given piece type
	protected static int[] pOrients = {1,2,4,4,4,2,2};
	
	//the next several arrays define the piece vocabulary in detail
	//width of the pieces [piece ID][orientation]
	protected static int[][] pWidth = {
			{2},
			{1,4},
			{2,3,2,3},
			{2,3,2,3},
			{2,3,2,3},
			{3,2},
			{3,2}
	};
	//height of the pieces [piece ID][orientation]
	private static int[][] pHeight = {
			{2},
			{4,1},
			{3,2,3,2},
			{3,2,3,2},
			{3,2,3,2},
			{2,3},
			{2,3}
	};
	private static int[][][] pBottom = {
		{{0,0}},
		{{0},{0,0,0,0}},
		{{0,0},{0,1,1},{2,0},{0,0,0}},
		{{0,0},{0,0,0},{0,2},{1,1,0}},
		{{0,1},{1,0,1},{1,0},{0,0,0}},
		{{0,0,1},{1,0}},
		{{1,0,0},{0,1}}
	};
	private static int[][][] pTop = {
		{{2,2}},
		{{4},{1,1,1,1}},
		{{3,1},{2,2,2},{3,3},{1,1,2}},
		{{1,3},{2,1,1},{3,3},{2,2,2}},
		{{3,2},{2,2,2},{2,3},{1,2,1}},
		{{1,2,2},{3,2}},
		{{2,2,1},{2,3}}
	};
	
	//initialize legalMoves
	{
		//for each piece type
		for(int i = 0; i < N_PIECES; i++) {
			//figure number of legal moves
			int n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//number of locations in this orientation
				n += COLS+1-pWidth[i][j];
			}
			//allocate space
			legalMoves[i] = new int[n][2];
			//for each orientation
			n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//for each slot
				for(int k = 0; k < COLS+1-pWidth[i][j];k++) {
					legalMoves[i][n][ORIENT] = j;
					legalMoves[i][n][SLOT] = k;
					n++;
				}
			}
		}
	}
	
	public int[][] getField() {
		return field;
	}

	public int[] getTop() {
		return top;
	}

    public static int[] getpOrients() {
        return pOrients;
    }
    
    public static int[][] getpWidth() {
        return pWidth;
    }

    public static int[][] getpHeight() {
        return pHeight;
    }

    public static int[][][] getpBottom() {
        return pBottom;
    }

    public static int[][][] getpTop() {
        return pTop;
    }

	public int getNextPiece() {
		return nextPiece;
	}
	
	public boolean hasLost() {
		return lost;
	}
	
	public int getRowsCleared() {
		return cleared;
	}
	
	public int getTurnNumber() {
		return turn;
	}

	public AuxState(State s) {
		turn = s.getTurnNumber();
		cleared = s.getRowsCleared();
		field = Helper.clone2DArr(s.getField());
		top = Helper.clone1DArr(s.getTop());
		nextPiece = s.getNextPiece();
		pWidth = s.getpWidth().clone();
		pOrients = s.getpOrients().clone();
		pHeight = s.getpHeight().clone();
		pBottom = s.getpBottom().clone();
		pTop = s.getpTop().clone();
	}
	
	//gives legal moves for 
	public int[][] legalMoves() {
		return legalMoves[nextPiece];
	}
	
	//make a move based on the move index - its order in the legalMoves list
	public void makeMove(int move) {
		makeMove(legalMoves[nextPiece][move]);
	}
	
	//make a move based on an array of orient and slot
	public void makeMove(int[] move) {
		makeMove(move[ORIENT],move[SLOT]);
	}
	
	//returns false if you lose - true otherwise
	public boolean makeMove(int orient, int slot) {
		turn++;
		//height if the first column makes contact
		int height = top[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}
		//check if game ended
		if(height+pHeight[nextPiece][orient] >= ROWS) {
			lost = true;
			return false;
		}
		
		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			
			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = turn;
			}
		}
		
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+pTop[nextPiece][orient][c];
		}
		
		int rowsCleared = 0;
		
		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < COLS; c++) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				rowsCleared++;
				cleared++;
				//for each column
				for(int c = 0; c < COLS; c++) {

					//slide down all bricks
					for(int i = r; i < top[c]; i++) {
						field[i][c] = field[i+1][c];
					}
					//lower the top
					top[c]--;
					while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
				}
			}
		}
		return true;
	}
}