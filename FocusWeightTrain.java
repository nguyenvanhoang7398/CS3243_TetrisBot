import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.*;
import java.util.*;
import java.lang.*;

//train the weights using a vector of 20 entries
public class FocusWeightTrain {
	public static final int GENERATION_NUMBER = 100; //to be adjusted
	public static final int POPULATION_SIZE = 100; //to be adjusted
	public static final int WEIGHT_VECTOR_SIZE = 4; //to be adjusted if necessary
	public static final double EPS = 1E-14; //should make this smaller???

	private double[][] population;
	private int[] fitness;
	private int totalFitness;
	private int bestGenerationFitness;
	private int idx;
	private double[] finalWeights;

	public FocusWeightTrain() {
		population = new double[POPULATION_SIZE][WEIGHT_VECTOR_SIZE];
		for (int i = 0; i < POPULATION_SIZE; i++) {
			for (int j = 0; j < WEIGHT_VECTOR_SIZE; j++) {
				population[i][j] = Math.random();
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
			System.out.println("Training the " + (i+1) + "-th generation");
			//evaluate fitness
			ReentrantLock lock = new ReentrantLock();
			totalFitness = 0;
			Integer[] tempFitness = Arrays.stream(population)
				.parallel()
				.map(s -> {
					//System.out.println(s);
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

			ExecutorService es = Executors.newFixedThreadPool(POPULATION_SIZE / 2);
			idx = 0;
			for (int j = 0; j < POPULATION_SIZE / 2; j++) {
				es.submit(() -> {
					while (true) {
						for (int k = 0; k < POPULATION_SIZE; k++) {
							if (Math.random() < (double)fitness[k] / (double)totalFitness) {
								lock.lock();
								try {
									newGeneration[idx] = population[k];
									idx++;
								} finally {
									lock.unlock();
								}
								break;
							}
						}
					}
				});
			}
			es.shutdown();
			try {
				es.awaitTermination(5,TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				es.shutdownNow();
				e.printStackTrace();
			}

			Runnable reproduceChildren = () -> {
				while (true) {
					double[] parent1 = null, parent2 = null;
					int k = 0, f1 = 0, f2 = 0;
					for (k = 0; k < POPULATION_SIZE; k++) {
						if (Math.random() < (double)fitness[k] / (double)totalFitness) {
							f1 = fitness[k];
							parent1 = population[k++];
							break;
						}
					}
					for (; k < POPULATION_SIZE; k++) {
						if (Math.random() < (double)fitness[k] / (double)totalFitness) {
							f2 = fitness[k];
							parent2 = population[k++];
							break;
						}
					}
					if (parent1 != null && parent2 != null) {
						double[][] children = reproduce(parent1, parent2);
						lock.lock();
						try {
							newGeneration[idx++] = children[0];
							newGeneration[idx++] = children[1];
							System.out.println("Breeded " + (idx-1) + " and " + idx + "-th children");
						} finally {
							lock.unlock();
						}
						break;
					}
				}
			};

			es = Executors.newFixedThreadPool(POPULATION_SIZE / 4);
			for (int j = 0; j < POPULATION_SIZE / 4; j++) {
				es.submit(reproduceChildren);
			}
			es.shutdown();
			try {
				es.awaitTermination(5,TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				es.shutdownNow();
				e.printStackTrace();
			}

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
	}

	private double[][] reproduce(double[] parent1, double[] parent2) {
		//breed parent1 and 2 here
		double[][] child = new double[2][WEIGHT_VECTOR_SIZE];
		for (int i = 0; i < WEIGHT_VECTOR_SIZE; i++) 
		{
			if (Math.random() < 0.5) {
				child[0][i] = parent1[i];
				child[1][i] = parent2[i];
			} else {
				child[0][i] = parent2[i];
				child[1][i] = parent1[i];
			}
			boolean isMutated = (Math.random() * WEIGHT_VECTOR_SIZE < 1);
			if (isMutated) {
				child[0][i] = Math.random();
				child[1][i] = Math.random();
			}
		}
		return child;
		//multiple breeding method to be tried
	}

	public static void main(String[] args) {
		FocusWeightTrain wt = new FocusWeightTrain();
		wt.train();
	}
}