package cn.tfs.eaco.com;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import cn.tfs.eaco.core.Environment;

/**
 * Reference:
 * X. Xu, Q. Liu, Y. Luo, K. Peng, X. Zhang, S. Meng, L. Qi, A computation offloading 
 * method over big data for IoT-enabled cloud-edge computing, Future Generation 
 * Computer Systems 95 (2019) 522¨C533.
 *
 * @author Duan Lintao
 *
 */
public class NSGA3 {	

    List<double[]> generateReferencePoints(int numObjectives, int divisions) {
        List<double[]> refPoints = new ArrayList<>();
        for (int i = 0; i <= divisions; i++) {
            double[] point = new double[numObjectives];
            point[0] = (double) i / divisions;
            point[1] = (double) (divisions - i) / divisions;
            refPoints.add(point);
        }
        return refPoints;
    }
    
    List<List<Integer>> nonDominatedSort(double[][] fitness) {
        int popSize = fitness.length;
        List<BitSet> dominates = new ArrayList<>(popSize);
        int[] dominatedCount = new int[popSize];
        List<List<Integer>> fronts = new ArrayList<>();
        fronts.add(new ArrayList<>());

        for (int i = 0; i < popSize; i++) {
        	dominates.add(new BitSet(popSize));
        }
        for (int i = 0; i < popSize; i++) {
            for (int j = 0; j < popSize; j++) {
                if (i == j) continue;
                boolean iDominatesJ = true;
                boolean hasStrictlyBetter = false;
                for (int k = 0; k < fitness[i].length; k++) {
                    if (fitness[i][k] > fitness[j][k]) {
                        iDominatesJ = false;
                        break;
                    } else if (fitness[i][k] < fitness[j][k]) {
                        hasStrictlyBetter = true;
                    }
                }
                if (iDominatesJ && hasStrictlyBetter) {
                    dominates.get(i).set(j);
                    dominatedCount[j]++;
                }
            }
            if (dominatedCount[i] == 0) {
                fronts.get(0).add(i);
            }
        }

        int currentFront = 0;
        while (fronts.size()>currentFront &&!fronts.get(currentFront).isEmpty()) {
            List<Integer> nextFront = new ArrayList<>();
            for (int i : fronts.get(currentFront)) {
            	for(int j = 0; j < popSize; j++) {
            		if (dominates.get(i).get(j)) {
            			dominatedCount[j]--;
            			if (dominatedCount[j] == 0) {
            				nextFront.add(j);
            			}
            		}
                }
            }
            currentFront++;
            if (!nextFront.isEmpty()) {
                fronts.add(nextFront);
            }
        }
        return fronts;
    }
    
    public List<OffloadingStrategy> selectNextGeneration(
    		List<OffloadingStrategy> population, 
            List<List<Integer>> fronts,
            List<double[]> referencePoints) {

        List<Integer> selectedIndividuals = new ArrayList<>();
        int currentFrontIndex = 0;

        while (currentFrontIndex < fronts.size() &&
        		selectedIndividuals.size() + fronts.get(currentFrontIndex).size() <= population.size()) {
            selectedIndividuals.addAll(fronts.get(currentFrontIndex));
            currentFrontIndex++;
        }

        if (selectedIndividuals.size() < population.size()) {
            List<Integer> lastFront = fronts.get(currentFrontIndex);           

            double[][] normalizedFitness = normalizeFitness(population, lastFront);

            Map<Integer, Integer> associationMap = associateToReferencePoints(
                normalizedFitness, 
                referencePoints
            );

            Map<Integer, Integer> nicheCount = new HashMap<>();
            associationMap.values().forEach(ref->
                nicheCount.put(ref, nicheCount.getOrDefault(ref, 0) + 1));           
            int remaining = population.size() - selectedIndividuals.size();
            while (remaining > 0 && !lastFront.isEmpty()) {
                Integer minRef = nicheCount.entrySet().stream()
                		.filter(e-> lastFront.stream().anyMatch(idx->e.getKey().equals(associationMap.get(idx))))
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry:: getKey)
                    .orElse(null);
                if(minRef==null) break;

                List<Integer> candidates = lastFront.stream()
                    .filter(idx -> associationMap.get(idx) == minRef)
                    .collect(Collectors.toList());
                
                if (!candidates.isEmpty()) {
                    Random rand = new Random();
                    int selected = candidates.get(rand.nextInt(candidates.size()));
                    selectedIndividuals.add(selected);
                    lastFront.remove((Integer) selected);
                    remaining--;
                    nicheCount.put(minRef, nicheCount.get(minRef) - 1);
                } else {
                    nicheCount.remove(minRef); 
                }
            }
        }

        return selectedIndividuals.stream()
            .map(population::get)
            .collect(Collectors.toList());
    }

    private double[][] normalizeFitness(List<OffloadingStrategy> population, List<Integer> front) {
        int numObjectives = 2; 
        double[][] fitness = new double[front.size()][numObjectives];
        
        for (int i = 0; i < front.size(); i++) {
            OffloadingStrategy s = population.get(front.get(i));
            fitness[i][0] = s.violateTimeConstraintNum;
            fitness[i][1] = s.energy;
        }

        double[] min = new double[numObjectives];
        double[] max = new double[numObjectives];
        Arrays.fill(min, Double.MAX_VALUE);
        Arrays.fill(max, Double.MIN_VALUE);
        
        for (double[] f : fitness) {
            for (int j = 0; j < numObjectives; j++) {
                if (f[j] < min[j]) min[j] = f[j];
                if (f[j] > max[j]) max[j] = f[j];
            }
        }

        double[][] normalized = new double[fitness.length][numObjectives];
        for (int i = 0; i < fitness.length; i++) {
            for (int j = 0; j < numObjectives; j++) {
                normalized[i][j] = (fitness[i][j] - min[j]) / (max[j] - min[j] + 1e-10);
            }
        }
        return normalized;
    }

    private Map<Integer, Integer> associateToReferencePoints(
            double[][] normalizedFitness, 
            List<double[]> referencePoints) {

        Map<Integer, Integer> associationMap = new HashMap<>();
        for (int i = 0; i < normalizedFitness.length; i++) {
            double minDistance = Double.MAX_VALUE;
            int closestRef = -1;
            
            for (int r = 0; r < referencePoints.size(); r++) {
                double[] ref = referencePoints.get(r);
                double distance = 0;
                for (int d = 0; d < ref.length; d++) {
                    distance += Math.pow(normalizedFitness[i][d] - ref[d], 2);
                }
                distance = Math.sqrt(distance);
                
                if (distance < minDistance) {
                    minDistance = distance;
                    closestRef = r;
                }
            }
            associationMap.put(i, closestRef);
        }
        return associationMap;
    }
    
    public OffloadingStrategy selectBestStrategy(List<OffloadingStrategy> population) {
        if (population.isEmpty()) {
            throw new IllegalArgumentException("Population cannot be empty.");
        }

        List<Integer> violateValues = population.stream().map(s -> s.violateTimeConstraintNum).collect(Collectors.toList());
        List<Double> energyValues = population.stream().map(s -> s.energy).collect(Collectors.toList());

        double vMin = violateValues.stream().min(Double::compare).get();
        double vMax = violateValues.stream().max(Double::compare).get();
        double eMin = energyValues.stream().min(Double::compare).get();
        double eMax = energyValues.stream().max(Double::compare).get();

        double maxUtility = Double.NEGATIVE_INFINITY;
        OffloadingStrategy bestStrategy = null;

        for (int i = 0; i < population.size(); i++) {
            OffloadingStrategy s = population.get(i);

            double normViolate = 0;
            if (vMax - vMin < 1e-10) {
                normViolate = 1.0; 
            } else {
            	normViolate = (vMax - s.violateTimeConstraintNum) / (vMax - vMin);
            }

            double normEnergy;
            if (eMax - eMin < 1e-10) {
                normEnergy = 1.0; 
            } else {
                normEnergy = (eMax - s.energy) / (eMax - eMin);
            }

            double utility =Environment.OBJCTIVE_WEIGHT * normEnergy+(1-Environment.OBJCTIVE_WEIGHT)*normViolate;
            s.fitnessUtility = utility;
            if (utility > maxUtility || (utility == maxUtility && bestStrategy == null)) {
                maxUtility = utility;
                bestStrategy = s;
            }
        }

        return bestStrategy;
    }
    
    public static OffloadingStrategy singlePointCrossover(OffloadingStrategy parent1, 
            OffloadingStrategy parent2, double crossoverRate) throws Exception{
    	Random rand = new Random();
		if (rand.nextDouble() > crossoverRate) {
			return (OffloadingStrategy)parent1.clone();
		}
		
		int length = parent1.strategy.length;
		OffloadingStrategy child = new OffloadingStrategy(length);

		int crossoverPoint = rand.nextInt(length - 1) + 1; 

		System.arraycopy(parent1.strategy, 0, child.strategy, 0, crossoverPoint);
		System.arraycopy(parent2.strategy, crossoverPoint, child.strategy, crossoverPoint, length - crossoverPoint);
		
		return child;
	}
    
    public static void uniformMutation(OffloadingStrategy individual, 
            double mutationRate) {
    	Random rand = new Random();
		for (int i = 0; i < individual.strategy.length; i++) {
			if (rand.nextDouble() < mutationRate) {
				int current = individual.strategy[i];
				int newValue;
				do {
					newValue = rand.nextInt(3); // 0,1,2
				} while (newValue == current);
				individual.strategy[i] = newValue;
			}
		}
	}
    
    public List<OffloadingStrategy> evolvePopulation(List<OffloadingStrategy> population) throws Exception{
        List<OffloadingStrategy> offspring = new ArrayList<>();

        while (offspring.size() < population.size()) {
            OffloadingStrategy parent1 = tournamentSelection(population);
            OffloadingStrategy parent2 = tournamentSelection(population);

            OffloadingStrategy child = singlePointCrossover(parent1, parent2, Environment.GA_CROSSOVER_RATE);
            offspring.add(child);
        }

        for (OffloadingStrategy ind : offspring) {
            uniformMutation(ind, Environment.GA_MUTATION_RATE);
        }

        return offspring;
    }

    private OffloadingStrategy tournamentSelection(List<OffloadingStrategy> pop) {
        int k = 2; 
        Random rand = new Random();
        List<OffloadingStrategy> candidates = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            candidates.add(pop.get(rand.nextInt(pop.size())));
        }
        if((candidates.get(0).fitnessValue[1]*Environment.OBJCTIVE_WEIGHT+candidates.get(0).fitnessValue[1]*(1-Environment.OBJCTIVE_WEIGHT))
        		>(candidates.get(1).fitnessValue[1]*Environment.OBJCTIVE_WEIGHT+candidates.get(1).fitnessValue[1]*(1-Environment.OBJCTIVE_WEIGHT))) {
        	return candidates.get(1);
        }
        return candidates.get(0);
    }
}