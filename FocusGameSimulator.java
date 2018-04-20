import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class FocusGameSimulator {
    private double[] weights;
    private int points;
    public static int MAX_GAMES = 5;
    public static int FEATURE_NUMBER = 6;

    //constructor should set weights
    //constructor should set points = 0
    public FocusGameSimulator(double[] _weights) {
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
        //to be editted
        ContractedState next = new ContractedState(s);
        next.makeMove(move);
        if (next.hasLost()) return -Double.MAX_VALUE;
        double[] feats = new double[FEATURE_NUMBER];
        feats[0] = next.getRowsCleared() - s.getRowsCleared();
        feats[1] = getHoles(next);
        int[] topS = s.getTop().clone();
        int[] topN = next.getTop();
        int hs = -1, hn = -1, ls = 30, ln = 30;
        int evenness = 0;
        int sumOfHeight = 0;
        int meanOfHeight = 0;
        for (int i = 0; i < ContractedState.COLS; i++) {
            hs = Math.max(hs, topS[i]);
            hn = Math.max(hn, topN[i]);
            ls = Math.min(ls, topS[i]);
            ln = Math.min(ln, topN[i]);
            sumOfHeight += topN[i];
            if (i < ContractedState.COLS - 1) 
                evenness += (topN[i + 1] - topN[i]) * (topN[i + 1] - topN[i]);
        }
        meanOfHeight = sumOfHeight / ContractedState.COLS;
        feats[2] = hn - hs;
        feats[3] = evenness;
        feats[4] = sumOfHeight;
        feats[5] = meanOfHeight;
        double utility = 0;
        for (int i = 0; i < FEATURE_NUMBER; i++)
            utility += feats[i] * weights[i];
        return utility;
    }

    //need function pick move
    public void simulate() {
        ReentrantLock lock = new ReentrantLock();

        Runnable oneGame = () -> {
            ContractedState s = new ContractedState(new State());
            while (!s.hasLost()) {
                s.makeMove(pickMove(s, s.legalMoves()));
				try {
					Thread.sleep(0);
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
        //System.out.println("Achieve " + points + " points!");
    }
}