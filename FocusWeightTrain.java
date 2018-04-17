import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

class WeightVector {
    public static final int WEIGHT_VECTOR_SIZE = 4;

    private double[] weights;
    private int fitness;

    public WeightVector() {
        weights = new double[4];
        fitness = 0;
    }

    public WeightVector(double[] _weights, int _fitness) {
        weights = _weights;
        fitness = _fitness;
    }

    public double[] getWeights() {
        return weights;
    }

    public void setWeights(double[] _weights) {
        weights = _weights;
    }

    public int getFitness() {
        return fitness;
    }

    public void setFitness(int _fitness) {
        fitness = _fitness;
    }
}

class SortByFitness implements Comparator<WeightVector> {
    public int compare(WeightVector a, WeightVector b) {
        return a.getFitness() - b.getFitness();
    }
}

//train the weights using a vector of 20 entries
public class FocusWeightTrain {
    public static final int GENERATION_NUMBER = 100; //to be adjusted
    public static final int POPULATION_SIZE = 100; //to be adjusted
    public static final int CARRY_OVER_SIZE = POPULATION_SIZE / 10;
    public static final int REPRODUCE_SIZE = 9 * POPULATION_SIZE / 10;
    public static final int PRIMAL_PERIOD = 10;
    public static final double INIT_PRIMAL_RATIO = 0.25;
    public static final double PRIMAL_RATIO_DECREASE_RATE = 0.5;
    public static final double PRIMAL_UPPER_BOUND = 5;
    public static final int WEIGHT_VECTOR_SIZE = 4; //to be adjusted if necessary
    public static final double EPS = 1E-14; //should make this smaller???

    private WeightVector[] population;
    private int[] primalPortion;
    private int[] fitness;
    private int totalFitness;
    private int bestGenerationFitness;
    private double[] bestGenerationWeights;
    private int idx;
    private double[] finalWeights;
    public int finalFitness;

    public FocusWeightTrain() {
        population = new WeightVector[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            double[] temp = new double[4];
            for (int j = 0; j < WEIGHT_VECTOR_SIZE; j++) {
                if (j == 0) temp[j] = Math.random();
                else temp[j] = -Math.random();
            }
            population[i] = new WeightVector(temp,0);
        }
        primalPortion = new int[POPULATION_SIZE];
        fitness = new int[POPULATION_SIZE];
        finalWeights = new double[WEIGHT_VECTOR_SIZE];
    }

    public void printWeight() {
        String trainedWeights = "";
        for (int i = 0; i < WEIGHT_VECTOR_SIZE; i++)
            trainedWeights = trainedWeights + (i > 0 ? " " : "") + finalWeights[i];
        System.out.println(trainedWeights);
    }

    public void printBestWeight() {
        String bestWeights = "";
        for (int i = 0; i < WEIGHT_VECTOR_SIZE; i++)
            bestWeights = bestWeights + (i > 0 ? " " : "") + bestGenerationWeights[i];
        System.out.println(bestWeights);
    }

    public void train() {
        for (int i = 0; i < GENERATION_NUMBER; i++) {
            System.out.println("Training the " + (i + 1) + "-th generation");
            //start timer
            long startTime = System.nanoTime();
            //evaluate fitness
            totalFitness = 0;
            Integer[] tempFitness = Arrays.stream(population)
                    .parallel()
                    .map(s -> {
                        //System.out.println(s);
                        FocusGameSimulator gs = new FocusGameSimulator(s.getWeights());
                        gs.simulate();
                        s.setFitness(gs.getPoints() + 1);
                        return s.getFitness();
                    })
                    .toArray(Integer[]::new);

            Arrays.sort(population, new SortByFitness());

            bestGenerationFitness = 1; //for progress tracking
            for (int j = 0; j < tempFitness.length; j++) {
                fitness[j] = population[j].getFitness();
                totalFitness += fitness[j];
                if (fitness[j] > bestGenerationFitness) {
                    bestGenerationFitness = fitness[j];
                    bestGenerationWeights = population[j].getWeights();
                }
            }

            System.out.println("Best fitness of this generation is " + bestGenerationFitness);
            System.out.println("Best candidate of this generation is:");
            printBestWeight();

            // begin primal dual transform
            if (i == 0) 
                primalPortion[i] = (int)Math.ceil(INIT_PRIMAL_RATIO * POPULATION_SIZE);
            else if (i % PRIMAL_PERIOD == 0) 
                primalPortion[i] = (int) Math.max(Math.ceil(PRIMAL_RATIO_DECREASE_RATE * primalPortion[i - PRIMAL_PERIOD]),
                    PRIMAL_UPPER_BOUND);
            else 
                primalPortion[i] = primalPortion[i-1];

            for (int j = 0; j < primalPortion[i]; j++) {
                double[] dualVector = primalDualTransform(population[j].getWeights());
                FocusGameSimulator gs = new FocusGameSimulator(dualVector);
                gs.simulate();
                int dualFitness = gs.getPoints() + 1;
                if (dualFitness > population[j].getFitness()) {
                    population[j].setWeights(dualVector);
                    population[j].setFitness(dualFitness);
                }
            }
            //end primal dual transformation

            //container for new generation
            WeightVector[] newGeneration = new WeightVector[POPULATION_SIZE];

            //Move the fitter half stochastically to the next generation
            ReentrantLock lock = new ReentrantLock();
            ExecutorService es = Executors.newFixedThreadPool(CARRY_OVER_SIZE);
            idx = 0;
            for (int j = 0; j < CARRY_OVER_SIZE; j++) {
                es.submit(() -> {
                    boolean foundCarryOver = false;
                    while (!foundCarryOver) {
                        for (int k = 0; k < POPULATION_SIZE; k++) {
                            if (Math.random() < (double) fitness[k] / (double) totalFitness) {
                                lock.lock();
                                try {
                                    newGeneration[idx] = population[k];
                                    idx++;
                                } finally {
                                    lock.unlock();
                                }
                                foundCarryOver = true;
                                break;
                            }
                        }
                    }
                });
            }
            es.shutdown();
            try {
                es.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                es.shutdownNow();
                e.printStackTrace();
            }

            System.out.println(idx + " mates carried over"); //for debugging

            Runnable reproduceChildren = () -> {
                double[] parent1 = null, parent2 = null;
                int k1 = 0, k2 = 0;
                while (parent1 == null) {
                    for (k1 = 0; k1 < POPULATION_SIZE; k1++) {
                        if (Math.random() < (double) fitness[k1] / (double) totalFitness) {
                            parent1 = population[k1].getWeights();
                            break;
                        }
                    }
                }
                while (parent2 == null) {
                    for (k2 = 0; k2 < POPULATION_SIZE; k2++) {
                        if (k2 != k1 && Math.random() < (double) fitness[k2] / (double) totalFitness) {
                            parent2 = population[k2].getWeights();
                            break;
                        }
                    }
                }
                double[] child = reproduce(parent1, parent2);
                lock.lock();
                try {
                    newGeneration[idx] = new WeightVector(child, 0);
                    idx++;
                    System.out.println("Breeded " + idx + "-th children");
                } finally {
                    lock.unlock();
                }
            };

            ExecutorService breedingEs = Executors.newFixedThreadPool(REPRODUCE_SIZE);
            for (int j = 0; j < REPRODUCE_SIZE; j++) {
                breedingEs.submit(reproduceChildren);
            }
            breedingEs.shutdown();
            try {
                breedingEs.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                breedingEs.shutdownNow();
                e.printStackTrace();
            }

            long endTime = System.nanoTime();

            System.out.println("Took " + (endTime - startTime) + " to train " + (i+1) + "-th generation");
            System.out.println("New generation contains " + idx + " members"); //for debugging

            population = newGeneration;
        }

        //get the fit-tess vector
        int maxFitness = fitness[0], maxIdx = 0;
        for (int i = 0; i < POPULATION_SIZE; i++) {
            if (fitness[i] > maxFitness) {
                maxFitness = fitness[i];
                maxIdx = i;
            }
        }
        finalWeights = population[maxIdx].getWeights();
        finalFitness = maxFitness;
    }

    private double[] primalDualTransform(double[] primal) {
        double[] dual = new double[primal.length];
        for (int i = 0; i < primal.length; i++) {
            if (i == 0) dual[i] = 1 - primal[i];
            else dual[i] = -1 - primal[i];
        }
        return dual;
    }

    private double[] reproduce(double[] parent1, double[] parent2) {
        //breed parent1 and 2 here
        double[] child = new double[WEIGHT_VECTOR_SIZE];
        //one-point crossover
        int crossoverPt = ThreadLocalRandom.current().nextInt(0, WEIGHT_VECTOR_SIZE);
        for (int i = 0; i < WEIGHT_VECTOR_SIZE; i++) {
            if (i <= crossoverPt) child[i] = parent1[i];
            else child[i] = parent2[i];
        }
        //one-point mutation
        boolean isMutated = (Math.random() < 0.05); //5% mutation rate
        if (isMutated) {
            int mutationPt = ThreadLocalRandom.current().nextInt(0, WEIGHT_VECTOR_SIZE);
            int sign = (mutationPt == 0 ? 1 : -1);
            child[mutationPt] = sign * Math.random();
        }
        return child;
        //multiple breeding method to be tried
    }

    public static void main(String[] args) {
        FocusWeightTrain wt = new FocusWeightTrain();
        wt.train();
        System.out.println("Best weight vector is ");
        wt.printWeight();
        System.out.println("Best fitness = " + wt.finalFitness);
    }
}