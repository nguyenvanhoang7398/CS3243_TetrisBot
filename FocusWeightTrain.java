import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

//train the weights using a vector of 20 entries
public class FocusWeightTrain {
    public static final int GENERATION_NUMBER = 10; //to be adjusted
    public static final int POPULATION_SIZE = 10; //to be adjusted
    public static final int CARRY_OVER_SIZE = POPULATION_SIZE / 10;
    public static final int REPRODUCE_SIZE = 9 * POPULATION_SIZE / 10;
    public static final int WEIGHT_VECTOR_SIZE = 4; //to be adjusted if necessary
    public static final double EPS = 1E-14; //should make this smaller???

    private double[][] population;
    private int[] fitness;
    private int totalFitness;
    private int bestGenerationFitness;
    private int idx;
    private double[] finalWeights;
    public int finalFitness;

    public FocusWeightTrain() {
        population = new double[POPULATION_SIZE][WEIGHT_VECTOR_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            for (int j = 0; j < WEIGHT_VECTOR_SIZE; j++) {
                if (j == 0) population[i][j] = Math.random();
                else population[i][j] = -Math.random();
            }
        }

        fitness = new int[POPULATION_SIZE];
        finalWeights = new double[WEIGHT_VECTOR_SIZE];
    }

    public void printWeight() {
        String trainedWeights = "";
        for (int i = 0; i < WEIGHT_VECTOR_SIZE; i++)
            trainedWeights = trainedWeights + (i > 0 ? " " : "") + finalWeights[i];
        System.out.println(trainedWeights);
    }

    public void train() {
        for (int i = 0; i < GENERATION_NUMBER; i++) {
            System.out.println("Training the " + (i + 1) + "-th generation");
            //evaluate fitness
            ReentrantLock lock = new ReentrantLock();
            totalFitness = 0;
            Integer[] tempFitness = Arrays.stream(population)
                    .parallel()
                    .map(s -> {
                        System.out.println(s);
                        FocusGameSimulator gs = new FocusGameSimulator(s);
                        gs.simulate();
                        return gs.getPoints() + 1;
                    })
                    .toArray(Integer[]::new);

            bestGenerationFitness = 1; //for progress tracking
            for (int j = 0; j < tempFitness.length; j++) {
                fitness[j] = tempFitness[j].intValue();
                totalFitness += fitness[j];
                if (fitness[j] > bestGenerationFitness)
                    bestGenerationFitness = fitness[j];
            }
            System.out.println("Best fitness of this generation is " + bestGenerationFitness);

            //container for new generation
            double[][] newGeneration = new double[POPULATION_SIZE][WEIGHT_VECTOR_SIZE];

            //Move the fitter half stochastically to the next generation

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
                            parent1 = population[k1];
                            break;
                        }
                    }
                }
                while (parent2 == null) {
                    for (k2 = 0; k2 < POPULATION_SIZE; k2++) {
                        if (k2 != k1 && Math.random() < (double) fitness[k2] / (double) totalFitness) {
                            parent2 = population[k2];
                            break;
                        }
                    }
                }
                double[] child = reproduce(parent1, parent2);
                lock.lock();
                try {
                    newGeneration[idx] = child;
                    idx++;
                    //System.out.println("Breeded " + idx + "-th children");
                } finally {
                    lock.unlock();
                }
            };

            es = Executors.newFixedThreadPool(REPRODUCE_SIZE);
            for (int j = 0; j < REPRODUCE_SIZE; j++) {
                es.submit(reproduceChildren);
            }
            es.shutdown();
            try {
                es.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                es.shutdownNow();
                e.printStackTrace();
            }

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
        finalWeights = population[maxIdx];
        finalFitness = maxFitness;
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