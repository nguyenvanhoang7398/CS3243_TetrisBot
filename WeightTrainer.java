import java.util.*;
import java.lang.*;

//train the weights using a vector of 20 entries
public class WeightTrainer {
	public static final int GENERATION_NUMBER = 100; //to be adjusted
	public static final int POPULATION_SIZE = 100; //to be adjusted
	public static final int WEIGHT_VECTOR_SIZE = 20; //to be adjusted if necessary
	public static final double EPS = 1E-14; //should make this smaller???

	private double[][] population;
	private double[] fitness;

	public WeightTrainer() {
		population = new double[POPULATION_SIZE][WEIGHT_VECTOR_SIZE];
		for (int i = 0; i < POPULATION_SIZE; i++) {
			for (int j = 0; j < WEIGHT_VECTOR_SIZE; j++) {
				population[i][j] = Math.random();
			}
		}

		fitness = new double[POPULATION_SIZE];
	}

	public void train() {
		for (int i = 0; i < GENERATION_NUMBER; i++) {
			//evaluate fitness
			for (int j = 0; j < POPULATION_SIZE; j++) {
				GameSimulator gs = new GameSimulator(population[j]);
				gs.simulate();
				fitness[j] = gs.getPoints();
			}
			//container for new generation
			double[][] newGeneration = new double[POPULATION_SIZE][WEIGHT_VECTOR_SIZE];

			//select individuals to breed
			double RATIO_BOUND = 1.1 * Collections.max(Arrays.asList(fitness));
			for (int j = 0; j < POPULATION_SIZE; j++) {
				double[] parent1, parent2;
				int k = 0, f1, f2;
				for (k = 0; k < POPULATION_SIZE; k++) {
					if (Math.random() < fitness[k]/RATIO_BOUND) {
						f1 = fitness[k];
						parent1 = population[k++].clone();
						break;
					}
				}
				for (; k < POPULATION_SIZE; k++) {
					if (Math.random() < fitness[k]/RATIO_BOUND) {
						f2 = fitness[k];
						parent2 = population[k++].clone();
						break;
					}
				}
				if (parent1 == null || parent2 == null) newGeneration[j] = 
				newGeneration[j] = reproduce(parent1,parent2);
			}
		}
	}

	private double[] reproduce(double[] parent1, double[] parent2, double f1, double f2) {
		//breed parent1 and 2 here
		//multiple breeding method to be tried

	}
}