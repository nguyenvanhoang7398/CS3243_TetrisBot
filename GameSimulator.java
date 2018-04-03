import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.*;
import java.util.*;
import java.lang.*;

public class GameSimulator {
	private double[] weights;
	private int points;
	public static int MAX_GAMES = 5;
	public static int FEATURE_NUMBER = 20;

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
		//to be editted
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
		ReentrantLock lock = new ReentrantLock();

		Runnable oneGame = () -> {
			ContractedState s = new ContractedState(new State());
			while(!s.hasLost()) {
				s.makeMove(pickMove(s,s.legalMoves()));
				//System.out.println("Made a move");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			lock.lock();
			try {
				points += s.getRowsCleared();
			} finally {
				lock.unlock();
			}
		};

		ExecutorService es = Executors.newFixedThreadPool(MAX_GAMES);
		for (int i = 0; i < MAX_GAMES; i++) {
			es.submit(oneGame);
		}
		es.shutdown();
		try {
			es.awaitTermination(30, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			es.shutdownNow();
			e.printStackTrace();
		}

		points = points / MAX_GAMES;
	}
}