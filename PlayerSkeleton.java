import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.*;
import java.util.Collections;
import java.util.List;
import java.util.List.*;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;

public class PlayerSkeleton {

    private static final int FEATURE_NUMBER = 4;
    private static final int MAX_AHEAD = 1;
    private static final int INF = 1000000000;

    private int[] movesArr;
//    private double[] weightFeat = new double[FEATURE_NUMBER];
    private static double[] weightFeat = {
        0.03729649350761288,
        -0.8997522188779017,
        -0.09645180388339958,
        -0.056707374997855386};
        
    public PlayerSkeleton() {
        movesArr = new int[State.N_PIECES];
        for (int i = 0; i < State.N_PIECES; i++) {
            movesArr[i] = i;
        }
    }

    //implement this function to have a working system
    public int pickMove(State currState, int[][] legalMoves) {
        double benchmark = -Double.MAX_VALUE;
        double maxUtility = -Double.MAX_VALUE;
        int nextMove = 0;
        int[] possibleMoves = new int[legalMoves.length];
        for (int i = 0; i < legalMoves.length; i++) 
            possibleMoves[i] = i;
        AuxState[][] imdAuxStates = new AuxState[legalMoves.length][movesArr.length];
        for (int i = 0; i < legalMoves.length; i++) {
        	for (int j = 0; j < movesArr.length; j++) 
        		imdAuxStates[i][j] = new AuxState(currState);
        }
        //ReentrantLock stateAccessLock = new ReentrantLock();
        ReentrantLock moveLock = new ReentrantLock();
        double[] utilities = Arrays.stream(possibleMoves)
        		.parallel()				
                .mapToDouble(idxLocal -> {
                    return Arrays.stream(movesArr)
                	.parallel()
                    .mapToDouble(piece -> {
                        moveLock.lock();
                        imdAuxStates[idxLocal][piece].makeMove(idxLocal);
                        if (!imdAuxStates[idxLocal][piece].hasLost()) {
                        	imdAuxStates[idxLocal][piece].setNextPiece(piece);
                        	imdAuxStates[idxLocal][piece].makeMove(
                        		pickSingleMove(imdAuxStates[idxLocal][piece]));
                        }
                        moveLock.unlock();
                        return getUtility(currState, imdAuxStates[idxLocal][piece]);
                    })
                    .sum();
                })
                .toArray();
        for (int i = 0; i < legalMoves.length; i++) {
            if (utilities[i] > benchmark && utilities[i] > maxUtility) {
                nextMove = i;
                maxUtility = utilities[i];
            }
        }
        return nextMove;
    }

    public int pickSingleMove(AuxState s) {
        int[][] legalMoves = s.legalMoves();
        double benchmarkSingle = -INF;
        double maxUtilitySingle = -INF;
        int moveSingle = 0;
        for (int k = 0; k < legalMoves.length; k++) {
            double utilitySingle = getUtility(s,k);
            if (utilitySingle > benchmarkSingle && utilitySingle > maxUtilitySingle) {
                moveSingle = k;
                maxUtilitySingle = utilitySingle;
            }
        }
        return moveSingle;
    }

    public int getHoles(AuxState s) {
        int result = 0;
        for (int i = 0; i < State.COLS; i++) {
            for (int j = 0; j < State.ROWS; j++) {
                if (s.getField()[j][i] == 0 && j < s.getTop()[i]) {
                    result++;
                }
            }
        }
        return result;
    }

    public double getUtility(AuxState s, int move) {
        AuxState next = new AuxState(s);
        next.makeMove(move);
        return getUtility(s, next);
    }

    private double getUtility(State s, AuxState next) {
        if (next.hasLost()) {
            return -INF;
        }

        double[] feats = new double[FEATURE_NUMBER]; //actual features
        feats[0] = next.getRowsCleared() - s.getRowsCleared(); //number of rows cleared
        feats[1] = getHoles(next); //number of holes
        int[] topS = s.getTop();
        int[] topN = next.getTop();
        int hs = -1, hn = -1, ls = 30, ln = 30;
        int evenness = 0;
        for (int i = 0; i < State.COLS; i++) {
            hs = Math.max(hs, topS[i]);
            hn = Math.max(hn, topN[i]);
            ls = Math.min(ls, topS[i]);
            ln = Math.min(ln, topN[i]);
            if (i < State.COLS - 1) {
                evenness += (topN[i + 1] - topN[i]) * (topN[i + 1] - topN[i]);
            }
        }
        feats[2] = hn - hs; //change in height
        feats[3] = evenness;
        double result = 0;
        for (int i = 0; i < FEATURE_NUMBER; i++) {
            result += feats[i] * weightFeat[i];
        }
        return result;
    }

    private double getUtility(AuxState s, AuxState next) {
        if (next.hasLost()) {
            return -INF;
        }

        double[] feats = new double[FEATURE_NUMBER]; //actual features
        feats[0] = next.getRowsCleared() - s.getRowsCleared(); //number of rows cleared
        feats[1] = getHoles(next); //number of holes
        int[] topS = s.getTop();
        int[] topN = next.getTop();
        int hs = -1, hn = -1, ls = 30, ln = 30;
        int evenness = 0;
        for (int i = 0; i < State.COLS; i++) {
            hs = Math.max(hs, topS[i]);
            hn = Math.max(hn, topN[i]);
            ls = Math.min(ls, topS[i]);
            ln = Math.min(ln, topN[i]);
            if (i < State.COLS - 1) {
                evenness += (topN[i + 1] - topN[i]) * (topN[i + 1] - topN[i]);
            }
        }
        feats[2] = hn - hs; //change in height
        feats[3] = evenness;
        double result = 0;
        for (int i = 0; i < FEATURE_NUMBER; i++) {
            result += feats[i] * weightFeat[i];
        }
        return result;
    }

    public static void main(String[] args) {
        State s = new State();
        new TFrame(s);
        PlayerSkeleton p = new PlayerSkeleton();
        while (!s.hasLost()) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
            System.out.println("Rows cleared until now: " + s.getRowsCleared());
            //System.out.println("Number of pickSingMove calls: " + cntPickSingleMove);
            s.draw();
            s.drawNext(0, 0);
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("You have completed " + s.getRowsCleared() + " rows.");
    }
}

class AuxState {
    public static final int COLS = 10;
    public static final int ROWS = 21;
    public static final int N_PIECES = 7;

    public boolean lost = false;

    //current turn
    private int turn = 0;
    private int cleared = 0;

    //each square in the grid - int means empty - other values mean the turn it was placed
    private int[][] field = new int[ROWS][COLS];
    //top row+1 of each column
    //0 means empty
    private int[] top = new int[COLS];


    //number of next piece
    private int nextPiece;


    //all legal moves - first index is piece type - then a list of 2-length arrays
    private static int[][][] legalMoves = new int[N_PIECES][][];

    //indices for legalMoves
    public static final int ORIENT = 0;
    public static final int SLOT = 1;

    //possible orientations for a given piece type
    protected static final int[] pOrients = {1, 2, 4, 4, 4, 2, 2};

    //the next several arrays define the piece vocabulary in detail
    //width of the pieces [piece ID][orientation]
    protected static final int[][] pWidth = {
            {2},
            {1, 4},
            {2, 3, 2, 3},
            {2, 3, 2, 3},
            {2, 3, 2, 3},
            {3, 2},
            {3, 2}
    };
    //height of the pieces [piece ID][orientation]
    protected static final int[][] pHeight = {
            {2},
            {4, 1},
            {3, 2, 3, 2},
            {3, 2, 3, 2},
            {3, 2, 3, 2},
            {2, 3},
            {2, 3}
    };
    protected static final int[][][] pBottom = {
            {{0, 0}},
            {{0}, {0, 0, 0, 0}},
            {{0, 0}, {0, 1, 1}, {2, 0}, {0, 0, 0}},
            {{0, 0}, {0, 0, 0}, {0, 2}, {1, 1, 0}},
            {{0, 1}, {1, 0, 1}, {1, 0}, {0, 0, 0}},
            {{0, 0, 1}, {1, 0}},
            {{1, 0, 0}, {0, 1}}
    };
    protected static final int[][][] pTop = {
            {{2, 2}},
            {{4}, {1, 1, 1, 1}},
            {{3, 1}, {2, 2, 2}, {3, 3}, {1, 1, 2}},
            {{1, 3}, {2, 1, 1}, {3, 3}, {2, 2, 2}},
            {{3, 2}, {2, 2, 2}, {2, 3}, {1, 2, 1}},
            {{1, 2, 2}, {3, 2}},
            {{2, 2, 1}, {2, 3}}
    };

    {
    	//initialize legalMoves
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

    public AuxState(State s) {
        turn = s.getTurnNumber();
        cleared = s.getRowsCleared();
        field = Helper.clone2DArr(s.getField());
        top = Helper.clone1DArr(s.getTop());
        nextPiece = s.getNextPiece();
        lost = s.hasLost();
    }

    public AuxState(AuxState s) {
        turn = s.getTurnNumber();
        cleared = s.getRowsCleared();
        field = Helper.clone2DArr(s.getField());
        top = Helper.clone1DArr(s.getTop());
        nextPiece = s.getNextPiece();
        lost = s.hasLost();
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

    public void setNextPiece(int _nextPiece) {
        this.nextPiece = _nextPiece;
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

    //gives legal moves for
    public int[][] legalMoves() {
        return legalMoves[nextPiece];
    }

    //make a move based on the move index - its order in the legalMoves list
    public void makeMove(int move) {
        if (move < 0 || move > legalMoves[nextPiece].length) {
            System.out.println("Somehow encounter an invalid move!");
            return ;
        }
        else makeMove(legalMoves[nextPiece][move]);
    }

    //make a move based on an array of orient and slot
    public void makeMove(int[] move) {
        makeMove(move[ORIENT], move[SLOT]);
    }

    //returns false if you lose - true otherwise
    public boolean makeMove(int orient, int slot) {
        turn++;
        //height if the first column makes contact
        int height = top[slot] - pBottom[nextPiece][orient][0];
        //for each column beyond the first in the piece
        for (int c = 1; c < pWidth[nextPiece][orient]; c++) {
            try {
                height = Math.max(height, top[slot + c] - pBottom[nextPiece][orient][c]);
            } catch (ArrayIndexOutOfBoundsException ioe) {
                System.out.println("Cell slot+c has index " + (slot+c));
            }
        }
        //check if game ended
        if (height + pHeight[nextPiece][orient] >= ROWS) {
            lost = true;
            return false;
        }

        //for each column in the piece - fill in the appropriate blocks
        for (int i = 0; i < pWidth[nextPiece][orient]; i++) {

            //from bottom to top of brick
            for (int h = height + pBottom[nextPiece][orient][i]; h < height + pTop[nextPiece][orient][i]; h++) {
                field[h][i + slot] = turn;
            }
        }

        for (int c = 0; c < pWidth[nextPiece][orient]; c++) {
            top[slot + c] = height + pTop[nextPiece][orient][c];
        }

        int rowsCleared = 0;

        //check for full rows - starting at the top
        for (int r = height + pHeight[nextPiece][orient] - 1; r >= height; r--) {
            //check all columns in the row
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (field[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            //if the row was full - remove it and slide above stuff down
            if (full) {
                rowsCleared++;
                cleared++;
                //for each column
                for (int c = 0; c < COLS; c++) {

                    //slide down all bricks
                    for (int i = r; i < top[c]; i++) {
                        field[i][c] = field[i + 1][c];
                    }
                    //lower the top
                    top[c]--;
                    while (top[c] >= 1 && field[top[c] - 1][c] == 0) top[c]--;
                }
            }
        }
        return true;
    }
}