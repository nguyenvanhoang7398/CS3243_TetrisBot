import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.*;
import java.util.*;
import java.lang.*;

public class GameSimulator {
	private double[] weights;
	private int points;
	private static final int INF = 1000000000;

	public static int MAX_GAMES = 2;
	public static int FEATURE_NUMBER = 12;

	//constructor should set weights
	//constructor should set points = 0
	public GameSimulator(double[] _weights) {
		weights = _weights.clone();
		points = 0;
	}

	public int getPoints() {
		return points;
	}

	public int pickMove(ContractedState s, int[][] legalMoves) {
		double benchmark = -Double.MAX_VALUE;
		double maxUtility = -Double.MAX_VALUE;
		int move = 0;
		for (int j = 0; j < legalMoves.length; j++) {
			double utility = getUtility(s,j);
			if (utility > benchmark && utility > maxUtility) {
				move = j;
				maxUtility = utility;
			}
		}
		return move;
	}

	public int getHoles(ContractedState s)
	{
		int result = 0;
		for (int i = 0; i < ContractedState.COLS; i++)
		{
			for (int j = 0; j < ContractedState.ROWS; j++) 
				if (s.getField()[j][i] == 0 && j < s.getTop()[i]) result++;
		}
		return result;
	}

	public double getUtility(ContractedState s, int move)
	{
		//to be editted
		ContractedState next = new ContractedState(s);
		next.makeMove(move);
		return getUtility(s,next);
	}

	private double getUtility(ContractedState s, ContractedState next) {
		if (next.hasLost()) {
			return -INF;
		}

		double[] feats = new double[FEATURE_NUMBER]; //actual features
		feats[0] = next.getRowsCleared() - s.getRowsCleared(); //number of rows cleared
		feats[1] = getHoles(next); //number of holes
		int[] topS = s.getTop().clone();
		int[] topN = next.getTop();
		int hs = -1, hn = -1;
		for (int i = 0; i < State.COLS; i++) {
			hs = Math.max(hs, topS[i]);
			hn = Math.max(hn, topN[i]);
		}
		feats[2] = hn - hs; //change in height
		for (int i = 3; i < FEATURE_NUMBER; i++) 
			feats[i] = (topN[i-2] - topN[i-3]) * (topN[i-2] - topN[i-3]);
		double result = 0;
		for (int i = 0; i < FEATURE_NUMBER; i++) {
			result += feats[i] * weights[i];
		}
		return result;
	}

	//need function pick move
	public void simulate() {
		ArrayList<Integer> games = new ArrayList<Integer>();
		for (int i = 0; i < MAX_GAMES; i++) 
			games.add(i);
		points = games.parallelStream()
		.mapToInt(i -> {
			ContractedState s = new ContractedState(new State());
			while(!s.hasLost()) {
				s.makeMove(pickMove(s,s.legalMoves()));
			}
			return s.getRowsCleared();
		})
		.sum() / MAX_GAMES;
	}
}