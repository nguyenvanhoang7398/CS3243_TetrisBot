import java.util.*;
import java.lang.*;

public class GameSimulator {
	private double[] weights;
	private double points;
	public static int MAX_GAMES = 10;
	public static int FEATURE_NUMBER = 20;

	//constructor should set weights
	//constructor should set points = 0
	public GameSimulator(double[] _weights) {
		weights = _weights.clone();
		points = 0;
	}

	public double getPoints() {
		return points;
	}

	public int pickMove(State s, int[][] legalMoves) {
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

	public int getHoles(State s)
	{
		//to be editted
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
		//to be editted
		ContractedState next = new ContractedState(s);
		next.makeMove(move);
		if (next.hasLost()) return -Double.MAX_VALUE;
		double utility = 0;
		for (int i = 0; i < ContractedState.COLS; i++) {
			utility += weights[i] * next.getTop()[i];
			utility += weights[i+ContractedState.COLS] * next.getHoles(i);
		}
		return utility;
	}

	//need function pick move
	public void simulate() {
		for (int i = 0; i < MAX_GAMES; i++) {
			ContractedState s = new ContractedState();
			GameSimulator p = new GameSimulator();
			while(!s.hasLost()) {
				s.makeMove(p.pickMove(s,s.legalMoves()));
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			points += s.getRowsCleared();
		}
		points = points/MAX_GAMES;
	}
}