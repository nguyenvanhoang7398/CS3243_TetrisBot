import java.util.*;
import java.lang.*;

//train the weights using a vector of 20 entries
public class WeightTrainer {
	public static final int GENERATION_NUMBER = 100; //to be adjusted
	public static final int POPULATION_SIZE = 40; //to be adjusted
	public static final int WEIGHT_VECTOR_SIZE = 20; //to be adjusted if necessary
	public static final double EPS = 1E-14; //should make this smaller???

	private double[][] population;
	private int[] fitness;
	private double[] finalWeights;

	public WeightTrainer() {
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
			int totalFitness = 0;
			for (int j = 0; j < POPULATION_SIZE; j++) {
				GameSimulator gs = new GameSimulator(population[j]);
				gs.simulate();
				fitness[j] = gs.getPoints() + 1; //+1 to avoid dividing by 0
				totalFitness += fitness[j];
				System.out.println("Fitness of " + (j+1) + "-th vector: " + fitness[j]);
			}
			//container for new generation
			double[][] newGeneration = new double[POPULATION_SIZE][WEIGHT_VECTOR_SIZE];

			//Move the fitter half stochastically to the next generation
			int j = 0;
			while (j < POPULATION_SIZE / 2) {
				for (int k = 0; k < POPULATION_SIZE; k++) {
					if (Math.random() < (double)fitness[k] / (double)totalFitness) {
						newGeneration[j++] = population[k];
						break;
					}
				}
			}

			//select individuals to breed
			while (j < POPULATION_SIZE) {
				double[] parent1 = null, parent2 = null;
				int k = 0, f1 = 0, f2 = 0;
				for (k = 0; k < POPULATION_SIZE; k++) {
					if (Math.random() < (double)fitness[k] / (double)totalFitness) {
						f1 = fitness[k];
						parent1 = population[k++].clone();
						break;
					}
				}
				for (; k < POPULATION_SIZE; k++) {
					if (Math.random() < (double)fitness[k] / (double)totalFitness) {
						f2 = fitness[k];
						parent2 = population[k++].clone();
						break;
					}
				}
				if (parent1 != null && parent2 != null) {
					double[][] children = reproduce(parent1, parent2, f1, f2);
					newGeneration[j++] = children[0];
					newGeneration[j++] = children[1];
					System.out.println("Breeded " + (j-1) + " and " + j + "-th children");
				}
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

	private double[][] reproduce(double[] parent1, double[] parent2, int f1, int f2) {
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
		WeightTrainer wt = new WeightTrainer();
		wt.train();
	}
}