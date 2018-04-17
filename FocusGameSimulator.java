import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.IntStream;

public class FocusGameSimulator {
	private double[] weights;
	private int points;
	private static final int INF = 1000000000;
	private static final int MAX_AHEAD = 1;
	private ArrayList<ArrayList<Integer>> nextPiecesArr;
	private int idx;

	public static int MAX_GAMES = 5;
	public static int FEATURE_NUMBER = 4;

    //constructor should set weights
    //constructor should set points = 0
	public FocusGameSimulator(double[] _weights) {
		generateNextPossiblePiecesForLookAhead();
		weights = _weights.clone();
		points = 0;
	}

	public int getPoints() {
		return points;
	}

	private void generateNextPossiblePiecesForLookAhead() {
		nextPiecesArr = new ArrayList<>();
		ArrayList<Integer> nextPieces = new ArrayList<>();
		generateNextPossiblePieces(nextPieces);
	}

	// recursive function to generate all possible sets of pieces for the next (MAX_AHEAD) moves
	private void generateNextPossiblePieces(ArrayList<Integer> nextPieces) {
		if (nextPieces.size() == MAX_AHEAD) {
			nextPiecesArr.add((ArrayList<Integer>) nextPieces.clone());
			return;
		}

		for (int nextPiece = 0; nextPiece < State.N_PIECES; nextPiece++) {
			nextPieces.add(nextPiece);
			generateNextPossiblePieces(nextPieces);
			nextPieces.remove(nextPieces.size() - 1);
		}
	}

	//implement this function to have a working system
	public int pickMove(State currState, int[][] legalMoves) {
		//cntPickSingleMove = 0;
		double benchmark = -Double.MAX_VALUE;
		double maxUtility = -Double.MAX_VALUE;
		int nextMove = 0;
		double[] utilities = new double[legalMoves.length];
		for (idx = 0; idx < legalMoves.length; idx++) {
		    utilities[idx] = nextPiecesArr.stream()
		        .mapToDouble(moves -> {
		            ContractedState nextState = new ContractedState(currState);
		            nextState.makeMove(idx);
		            for (int i = 0; i < moves.size(); i++) {
		                nextState.setNextPiece(moves.get(i).intValue());
		                //cntPickSingleMove++;
		                nextState.makeMove(pickSingleMove(nextState,nextState.legalMoves()));
		                if (nextState.hasLost()) {
		                    break;
		                }
		            }
		            return getUtility(currState, nextState);
		        })
		        .sum();
		    if (utilities[idx] > benchmark && utilities[idx] > maxUtility) {
		        nextMove = idx;
		        maxUtility = utilities[idx];
		    }
		}
		return nextMove;
	}

	public int pickSingleMove(ContractedState s, int[][] legalMoves) {
		double benchmark = -INF;
		double maxUtility = -INF;
		int move = 0;
		for (int j = 0; j < legalMoves.length; j++) {
			double utility = getUtility(s, j);
			if (utility > benchmark && utility > maxUtility) {
				move = j;
				maxUtility = utility;
			}
		}
		return move;
	}

	public int getHoles(ContractedState s) {
        //to be editted
		int result = 0;
		for (int i = 0; i < ContractedState.COLS; i++) {
			for (int j = 0; j < ContractedState.ROWS; j++)
				if (s.getField()[j][i] == 0 && j < s.getTop()[i]) result++;
		}
		return result;
	}

	public double getUtility(ContractedState s, int move) {
	         ContractedState next = new ContractedState(s);
	         next.makeMove(move);
	         return getUtility(s, next);
	     }
	 
	private double getUtility(State s, ContractedState next) {
		if (next.hasLost()) {
		 return -INF;
		}

		double[] feats = new double[FEATURE_NUMBER]; //actual features
		feats[0] = next.getRowsCleared() - s.getRowsCleared(); //number of rows cleared
		feats[1] = getHoles(next); //number of holes
		int[] topS = s.getTop().clone();
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
			result += feats[i] * weights[i];
		}
		return result;
	}

    //need function pick move
	public void simulate() {
		ArrayList<Integer> gameIndices = new ArrayList<Integer>();
		for (int i = 0; i < MAX_GAMES; i++) 
			gameIndices.add(i);
		points = gameIndices
			.parallelStream()
			.mapToInt(i -> {
				ContractedState s = new ContractedState(new State());
				while (!s.hasLost()) {
					s.makeMove(pickMove(s, s.legalMoves()));
					try {
						Thread.sleep(0);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				return s.getRowsCleared();
			})
			.sum();
		points = points / MAX_GAMES;
        System.out.println("Achieve " + points + " points!");
	}
}