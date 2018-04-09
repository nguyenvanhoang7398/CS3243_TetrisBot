import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class PlayerSkeleton {
    public static final int FEATURE_NUMBER = 4;
    public static final int MAX_DEPTH = 1000;
    public static final int NUM_ELEMENTS = 100;
    public static final int MAX_TRAINING_SETS = 5;
    public static final int MAX_AHEAD = 1;
    public static final String TRAINING_DATA_PATH = System.getProperty("user.dir") + "\\training_data\\";
    public static final String TRAINING_RESULT_PATH = System.getProperty("user.dir") + "\\training_result\\";
    public static final String GENERATIONS_PATH = System.getProperty("user.dir") + "\\best_candidates\\";
    public static final int MAX_PIECES = 10000000;
    public static final int MAX_WEIGHT = 1000;
    public static final int PROBABILITY_PRECISION = 100000;
    public static final int INF = 1000000000;
    //    public static double[] weightFeat = new double[FEATURE_NUMBER]; //weights of features
    public static double[] weightFeat = {0.03878734858511845, -0.7182753941998763, -0.15390039107928566, -0.11034200725751109}; // 90k
    public static double[] optimalWeight = new double[FEATURE_NUMBER]; //weights of features
    public static double[][] genWeights = new double[NUM_ELEMENTS][FEATURE_NUMBER];
    public static double[][] prevGenWeights = new double[NUM_ELEMENTS][FEATURE_NUMBER];
    public static double[] res = new double[NUM_ELEMENTS];
    public static double[] prevRes = new double[NUM_ELEMENTS];
    public static double[] prob = new double[NUM_ELEMENTS];
    public static int[] range = new int[NUM_ELEMENTS];
    public static ArrayList<ArrayList<Integer>> movesArr;

    //implement this function to have a working system
    public int pickMove(State s, int[][] legalMoves) {
        double benchmark = -Double.MAX_VALUE;
        double maxUtility = -Double.MAX_VALUE;
        int move = 0;

        for (int j = 0; j < legalMoves.length; j++) {
            double utility = 0;
//            double utility = Double.MAX_VALUE;

            for (int curSet = 1; curSet <= Math.pow(State.N_PIECES, MAX_AHEAD); curSet++) {
                AuxState next = new AuxState(s);
                int curMove = j;
                next.makeMove(curMove);
                ArrayList<Integer> moves = movesArr.get(curSet - 1);
                for (int i = 0; i < moves.size(); i++) {
                    next.setNextPiece(moves.get(i));
                    curMove = pickSingleMove(next, next.legalMoves());
                    next.makeMove(curMove);
                    if (next.hasLost()) {
                        break;
                    }
                }
                utility += getUtility(s, next);
//                utility = Math.min(utility, getUtility(s, next));
            }

            if (utility > benchmark && utility > maxUtility) {
                move = j;
                maxUtility = utility;
            }
        }

        return move;
    }

    public int pickSingleMove(AuxState s, int[][] legalMoves) {
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
        // System.out.println("S's field ");
        // Helper.print2DArr(s.getField());
        return getUtility(s, next);
    }

    private double getUtility(State s, AuxState next) {
        if (next.hasLost()) {
            return -INF;
        }
//        if (next.hasLost()) {
//            return Double.MAX_VALUE;
//        }

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
//                evenness += Math.abs(topN[i + 1] - topN[i]);
                evenness += (topN[i + 1] - topN[i]) * (topN[i + 1] - topN[i]);
//                if (topN[i + 1] == topN[i]) evenness--;
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

    //random integer, returns 0-6
    private static int randomPiece() {
        return (int) (Math.random() * State.N_PIECES);
    }

    private static void generateTrainingData(int curSet) {
        String fileName = "training_data_set_" + curSet + ".in";
        File file = new File(TRAINING_DATA_PATH + fileName);

        try {
            PrintWriter printWriter = new PrintWriter(file);

            for (int i = 0; i < MAX_PIECES; i++) {
                printWriter.print(randomPiece());
                printWriter.print(" ");
            }

            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Generated: " + TRAINING_DATA_PATH + fileName);
    }

    private static void generateTrainingData(int curSet, ArrayList<Integer> moves) {
        String fileName = "training_data_set_" + curSet + ".in";
        File file = new File(TRAINING_DATA_PATH + fileName);

        try {
            PrintWriter printWriter = new PrintWriter(file);

            System.out.println(curSet);
            for (int move : moves) {
                printWriter.print(move);
                System.out.println(move);
                printWriter.print(" ");
            }

            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Generated: " + TRAINING_DATA_PATH + fileName);
    }

    private static void generateTrainingData() {
//        for (int curSet = 1; curSet <= MAX_TRAINING_SETS; curSet++) {
//            generateTrainingData(curSet);
//        }
        movesArr = new ArrayList<>();
        for (int i = 0; i < State.N_PIECES; i++) {
//            for (int j = 0; j < State.N_PIECES; j++) {
//                for (int k = 0; k < State.N_PIECES; k++) {
            ArrayList<Integer> moves = new ArrayList<>();
//                    moves.addAll(Arrays.asList(i, j, k));
//                moves.addAll(Arrays.asList(i, j));
            moves.addAll(Arrays.asList(i));
            movesArr.add(moves);
//                generateTrainingData(curSet, moves);
//                }
//            }
        }
    }

    private static void generateGeneration0() {
//        Random random = new Random();
        for (int id = 0; id < NUM_ELEMENTS; id++) {
            double i, j, k, l;
//            i = Math.abs(random.nextInt() % MAX_WEIGHT);
//            j = -Math.abs(random.nextInt() % MAX_WEIGHT);
//            k = -Math.abs(random.nextInt() % MAX_WEIGHT);
//            l = -Math.abs(random.nextInt() % MAX_WEIGHT);
            i = Math.abs(Math.random());
            j = -Math.abs(Math.random());
            k = -Math.abs(Math.random());
            l = -Math.abs(Math.random());
            genWeights[id][0] = i;
            genWeights[id][1] = j;
            genWeights[id][2] = k;
            genWeights[id][3] = l;
        }
    }

    private static void trainCurrentWeightsWithDataSet(int id, int curSet) {
        State s = new State();
        PlayerSkeleton p = new PlayerSkeleton();
        while (!s.hasLost()) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
            System.out.println(id + " Rows cleared: " + s.getRowsCleared() + " " + weightFeat[0] + " " + weightFeat[1] + " "
                    + weightFeat[2] + " " + weightFeat[3]);
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        res[id] += s.getRowsCleared();
    }

    private static void saveCurrentGeneration(int depth) {
        String fileName = "training_result_set_" + depth + ".in";
        File file = new File(TRAINING_RESULT_PATH + fileName);

        try {
            PrintWriter printWriter = new PrintWriter(file);

            for (int id = 0; id < NUM_ELEMENTS; id++) {
                for (int index = 0; index < FEATURE_NUMBER; index++) {
                    printWriter.print(genWeights[id][index] + " ");
                }
                printWriter.println((int) res[id] / MAX_TRAINING_SETS);
            }

            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void saveCurrentGenerationAfterCombination() {
        for (int id = 0; id < NUM_ELEMENTS; id++) {
            for (int index = 0; index < FEATURE_NUMBER; index++) {
                prevGenWeights[id][index] = genWeights[id][index];
            }
            prevRes[id] = res[id];
        }
    }

    private static void calculateProbability() {
        double totalRes = 0;
        for (int id = 0; id < NUM_ELEMENTS; id++) {
            totalRes += res[id];
        }
        int curMaxRange = 0;
        for (int id = 0; id < NUM_ELEMENTS; id++) {
            prob[id] = res[id] / totalRes;
            curMaxRange += (int) (PROBABILITY_PRECISION * prob[id]);
            range[id] = curMaxRange;
        }
    }

    private static int selectCandidate(Random random) {
        int val = Math.abs(random.nextInt()) % PROBABILITY_PRECISION;
        for (int id = 0; id < NUM_ELEMENTS; id++) {
            if (range[id] > val) {
                return id;
            }
        }
        return NUM_ELEMENTS - 1;
    }

    private static void crossover(int id, int firstCandidate, int secondCandidate) {
//        double totalPoint = res[firstCandidate] + res[secondCandidate];
//
//        for (int feature = 0; feature < FEATURE_NUMBER; feature++) {
//            genWeights[id][feature] = (res[firstCandidate] / totalPoint) * prevGenWeights[firstCandidate][feature]
//                    + (res[secondCandidate] / totalPoint) * prevGenWeights[secondCandidate][feature];
//        }

        Random random = new Random();
        int pos = Math.abs(random.nextInt()) % (FEATURE_NUMBER + 1);
        for (int i = 0; i < pos; i++) {
            genWeights[id][i] = prevGenWeights[firstCandidate][i];
        }
        for (int i = pos; i < FEATURE_NUMBER; i++) {
            genWeights[id][i] = prevGenWeights[secondCandidate][i];
        }
    }

    private static void mutation(int id) {
//        Random random = new Random();
        for (int feature = 0; feature < FEATURE_NUMBER; feature++) {
//            if (Math.abs(random.nextInt()) % 1000 < 50) {
            if (Math.random() < 0.05) {
//                genWeights[id][feature] = Math.abs(random.nextInt() % MAX_WEIGHT);
                genWeights[id][feature] = Math.random();
                if (feature > 0) {
                    genWeights[id][feature] = -genWeights[id][feature];
                }
//                break;
            }
        }
    }

    private static void generateNextGeneration(int depth) {
        calculateProbability();

        Random random = new Random();
//        HashMap<String, Boolean> isUsed = new HashMap<>();
        for (int id = 0; id < NUM_ELEMENTS; id++) {
            int firstCandidate, secondCandidate;

            String stringPair;
//            do {
            firstCandidate = selectCandidate(random);
            secondCandidate = selectCandidate(random);
            while (secondCandidate == firstCandidate) {
                secondCandidate = selectCandidate(random);
            }
//                if (firstCandidate > secondCandidate) {
//                    int tmp = firstCandidate;
//                    firstCandidate = secondCandidate;
//                    secondCandidate = tmp;
//                }
            stringPair = String.valueOf(firstCandidate) + "&" + String.valueOf(secondCandidate);
//                System.out.println(id + " " + stringPair);
//            } while (isUsed.containsKey(stringPair));
//            isUsed.put(stringPair, true);

            crossover(id, firstCandidate, secondCandidate);

//            System.out.println("Before mutation");
//            System.out.println("Parent 1: ");
//            for (int feature = 0; feature < FEATURE_NUMBER; feature++) {
//                System.out.print(prevGenWeights[firstCandidate][feature] + " ");
//            }
//            System.out.println(res[firstCandidate]);
//
//            System.out.println("Parent 2: ");
//            for (int feature = 0; feature < FEATURE_NUMBER; feature++) {
//                System.out.print(prevGenWeights[secondCandidate][feature] + " ");
//            }
//            System.out.println(res[secondCandidate]);
//
//            System.out.println("Child: ");
//            for (int feature = 0; feature < FEATURE_NUMBER; feature++) {
//                System.out.print(genWeights[id][feature] + " ");
//            }
//            System.out.println();

            mutation(id);

//            System.out.println("After mutation");
//            System.out.println("Parent 1: ");
//            for (int feature = 0; feature < FEATURE_NUMBER; feature++) {
//                System.out.print(prevGenWeights[firstCandidate][feature] + " ");
//            }
//            System.out.println(res[firstCandidate]);
//
//            System.out.println("Parent 2: ");
//            for (int feature = 0; feature < FEATURE_NUMBER; feature++) {
//                System.out.print(prevGenWeights[secondCandidate][feature] + " ");
//            }
//            System.out.println(res[secondCandidate]);
//
//            System.out.println("Child: ");
//            for (int feature = 0; feature < FEATURE_NUMBER; feature++) {
//                System.out.print(genWeights[id][feature] + " ");
//            }
//            System.out.println();
        }
    }

    private static double updateOptimalWeights(double ans, int depth, int id) {
        if (res[id] > ans) {
            for (int index = 0; index < PlayerSkeleton.FEATURE_NUMBER; index++) {
                optimalWeight[index] = weightFeat[index];
            }
            ans = res[id];
        }

        System.out.println(depth + " " + id + " " + ((int) res[id] / MAX_TRAINING_SETS) + " "
                + ((int) ans / MAX_TRAINING_SETS) + " " + optimalWeight[0] + " " + optimalWeight[1] + " "
                + optimalWeight[2] + " " + optimalWeight[3] + " ");
        /*System.out.println(depth + " " + id + " " + res[id] + " " +
                genWeights[id][0] + " " + genWeights[id][1] + " " + genWeights[id][2] + " " + genWeights[id][3] + " ");*/
        return ans;
    }

    private static void updateFinalOptimalWeights() {
        for (int index = 0; index < PlayerSkeleton.FEATURE_NUMBER; index++) {
            weightFeat[index] = optimalWeight[index];
            System.out.println("Weight " + index + ": " + weightFeat[index]);
        }
    }

    private static void saveBestCandidates(int depth) {
        String fileName = "best_candidates_set_" + depth + ".in";
//        File file = new File(GENERATIONS_PATH + fileName);
        File file = new File(fileName);

        try {
            PrintWriter printWriter = new PrintWriter(file);

            for (int id = 0; id < NUM_ELEMENTS; id++) {
                for (int index = 0; index < FEATURE_NUMBER; index++) {
                    printWriter.print(genWeights[id][index] + " ");
                }
                printWriter.println((int) res[id] / MAX_TRAINING_SETS);
            }

            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void combineLatestCandidates(int depth) {
        ArrayList<FeatureWeightsWithScore> arrayList = new ArrayList<>();
        for (int id = 0; id < NUM_ELEMENTS; id++) {
            arrayList.add(new FeatureWeightsWithScore(genWeights[id][0], genWeights[id][1], genWeights[id][2],
                    genWeights[id][3], res[id]));
            arrayList.add(new FeatureWeightsWithScore(prevGenWeights[id][0], prevGenWeights[id][1],
                    prevGenWeights[id][2], prevGenWeights[id][3], prevRes[id]));
        }
        Collections.sort(arrayList, new Comparator<FeatureWeightsWithScore>() {
            @Override
            public int compare(FeatureWeightsWithScore o1, FeatureWeightsWithScore o2) {
                return (int) (o2.getScore() - o1.getScore());
            }
        });
        int curCandidate = 0;
        for (int id = 0; id < NUM_ELEMENTS; id++) {
            boolean isSameAsPrev;
            do {
                isSameAsPrev = true;
                if (id > 0) {
                    for (int index = 0; index < FEATURE_NUMBER; index++) {
                        if (arrayList.get(curCandidate).getWeights(index) != genWeights[id - 1][index]) {
                            isSameAsPrev = false;
                            break;
                        }
                    }
                } else {
                    isSameAsPrev = false;
                }

                if (isSameAsPrev) {
                    curCandidate++;
                }
            } while (isSameAsPrev && curCandidate < arrayList.size());

            if (curCandidate == arrayList.size()) {
                break;
            }

            res[id] = arrayList.get(curCandidate).getScore();
            for (int index = 0; index < FEATURE_NUMBER; index++) {
                genWeights[id][index] = arrayList.get(curCandidate).getWeights(index);
            }
        }

        saveBestCandidates(depth);
    }

    private static void trainWithTrainingData() {
        double ans = 0;

        generateGeneration0();

        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            // test current depth
            for (int id = 0; id < NUM_ELEMENTS; id++) {
                res[id] = 0;
                for (int index = 0; index < FEATURE_NUMBER; index++) {
                    weightFeat[index] = genWeights[id][index];
                }

                for (int curSet = 1; curSet <= MAX_TRAINING_SETS; curSet++) {
                    trainCurrentWeightsWithDataSet(id, curSet);
                }

                ans = updateOptimalWeights(ans, depth, id);
            }
            saveCurrentGeneration(depth);

            // combine generations to get best candidates
            combineLatestCandidates(depth);

            saveCurrentGenerationAfterCombination();

            // generate next generation
            generateNextGeneration(depth);
        }

        updateFinalOptimalWeights();
        System.out.println(ans);
    }

    public static void main(String[] args) {
        boolean isGenerating = true;
        if (isGenerating) {
            generateTrainingData();
        }

        boolean isTraining = true;
        if (isTraining) {
            trainWithTrainingData();
        }

        boolean isTestingWeight = false;
        if (isTestingWeight) {
            res[0] = 0;
            for (int curSet = 1; curSet <= MAX_TRAINING_SETS; curSet++) {
                trainCurrentWeightsWithDataSet(0, curSet);
            }
            System.out.println(res[0]);
        }

        State s = new State();
//        new TFrame(s);
        PlayerSkeleton p = new PlayerSkeleton();
        while (!s.hasLost()) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
            System.out.println(s.getRowsCleared());
//            s.draw();
//            s.drawNext(0, 0);
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("You have completed " + s.getRowsCleared() + " rows.");
    }
}

class FeatureWeightsWithScore {
    private double[] features = new double[PlayerSkeleton.FEATURE_NUMBER];
    private double score;

    public FeatureWeightsWithScore(double weight0, double weight1, double weight2, double weight3, double score) {
        features[0] = weight0;
        features[1] = weight1;
        features[2] = weight2;
        features[3] = weight3;
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public double getWeights(int index) {
        return features[index];
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
    protected static int[] pOrients = {1, 2, 4, 4, 4, 2, 2};

    //the next several arrays define the piece vocabulary in detail
    //width of the pieces [piece ID][orientation]
    protected static int[][] pWidth = {
            {2},
            {1, 4},
            {2, 3, 2, 3},
            {2, 3, 2, 3},
            {2, 3, 2, 3},
            {3, 2},
            {3, 2}
    };
    //height of the pieces [piece ID][orientation]
    private static int[][] pHeight = {
            {2},
            {4, 1},
            {3, 2, 3, 2},
            {3, 2, 3, 2},
            {3, 2, 3, 2},
            {2, 3},
            {2, 3}
    };
    private static int[][][] pBottom = {
            {{0, 0}},
            {{0}, {0, 0, 0, 0}},
            {{0, 0}, {0, 1, 1}, {2, 0}, {0, 0, 0}},
            {{0, 0}, {0, 0, 0}, {0, 2}, {1, 1, 0}},
            {{0, 1}, {1, 0, 1}, {1, 0}, {0, 0, 0}},
            {{0, 0, 1}, {1, 0}},
            {{1, 0, 0}, {0, 1}}
    };
    private static int[][][] pTop = {
            {{2, 2}},
            {{4}, {1, 1, 1, 1}},
            {{3, 1}, {2, 2, 2}, {3, 3}, {1, 1, 2}},
            {{1, 3}, {2, 1, 1}, {3, 3}, {2, 2, 2}},
            {{3, 2}, {2, 2, 2}, {2, 3}, {1, 2, 1}},
            {{1, 2, 2}, {3, 2}},
            {{2, 2, 1}, {2, 3}}
    };

    //initialize legalMoves
    {
        //for each piece type
        for (int i = 0; i < N_PIECES; i++) {
            //figure number of legal moves
            int n = 0;
            for (int j = 0; j < pOrients[i]; j++) {
                //number of locations in this orientation
                n += COLS + 1 - pWidth[i][j];
            }
            //allocate space
            legalMoves[i] = new int[n][2];
            //for each orientation
            n = 0;
            for (int j = 0; j < pOrients[i]; j++) {
                //for each slot
                for (int k = 0; k < COLS + 1 - pWidth[i][j]; k++) {
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

    public void setNextPiece(int nextPiece) {
        this.nextPiece = nextPiece;
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

    public AuxState() {
        nextPiece = 0;
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
        makeMove(move[ORIENT], move[SLOT]);
    }

    //returns false if you lose - true otherwise
    public boolean makeMove(int orient, int slot) {
        turn++;
        //height if the first column makes contact
        int height = top[slot] - pBottom[nextPiece][orient][0];
        //for each column beyond the first in the piece
        for (int c = 1; c < pWidth[nextPiece][orient]; c++) {
            height = Math.max(height, top[slot + c] - pBottom[nextPiece][orient][c]);
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

        nextPiece = 0;

        return true;
    }
}