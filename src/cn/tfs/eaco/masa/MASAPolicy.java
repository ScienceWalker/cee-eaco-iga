package cn.tfs.eaco.masa;

import java.util.Collections;
import java.util.Random;

import cn.tfs.eaco.core.Environment;
import cn.tfs.eaco.core.Task;
import cn.tfs.eaco.iga.Chromosome;
import cn.tfs.eaco.iga.IGAPolicy;
/**
 * A memetic algorithm that combines GA with SA as its local search component. 
 * SA probabilistically accepts worse solutions during the search process based
 * on a temperature parameter, which gradually decreases to balance exploration 
 * and exploitation.
 * 
 * @author Duan Lintao
 *
 */
public class MASAPolicy extends IGAPolicy{

	public Chromosome saOptimizeSearch(Chromosome individual, int iterations) {
		Chromosome currentChromosome = new Chromosome();
		currentChromosome.copy(individual);
		Chromosome bestChromosome = new Chromosome();
		bestChromosome.copy(individual);
		
        fitness(currentChromosome);
        
        double currentEnergy = currentChromosome.energy;
        double currentTask = currentChromosome.violateTimeConstraintNum;
        double bestEnergy = currentEnergy;
        double bestTask = currentTask;
        
        double temperature = 100;
        double coolingRate = 0.01;
        
        Random random = new Random();
        
        for (int i = 0; i < iterations; i++) {
            Chromosome newChromosome = new Chromosome();
            newChromosome.copy(currentChromosome);
    		
            int index1 = random.nextInt(newChromosome.sequence.size());
            int index2 = random.nextInt(newChromosome.sequence.size());
            Collections.swap(newChromosome.sequence, index1, index2);
            
            fitness(newChromosome);
            
            double newEnergy = newChromosome.energy;
            double newTask = newChromosome.violateTimeConstraintNum;
            
            if (acceptanceProbability(currentEnergy, newEnergy, currentTask, newTask, temperature) > random.nextDouble()) {
            	currentChromosome.copy(newChromosome);
            	currentEnergy = newEnergy;
            	currentTask = newTask;
            }
            
            if ((currentEnergy <= bestEnergy && currentTask < bestTask)||
            	(currentEnergy < bestEnergy && currentTask <= bestTask)	) {
            	bestChromosome.copy(currentChromosome);
            	bestEnergy = currentEnergy;
            	bestTask = currentTask;
            }

            temperature *= 1 - coolingRate;
        }
        
        return bestChromosome;
    }
	
	private double acceptanceProbability(
			double currentEnergy, double newEnergy, 
			double currentTask, double newTask, double temperature) {
        if ((newEnergy < currentEnergy && newTask <= currentTask)||
        		(newEnergy <= currentEnergy && newTask < currentTask)) {
            return 1.0;
        }
        double e = Math.exp((currentEnergy - newEnergy) / temperature);
        double t = Math.exp((currentTask - newTask) / temperature);
        return e>t?e:t;
    }
	
	public void hybridGeneticAlgorithm() {

		initializePopulation();

		for(Chromosome chromosome : chromosomes) {
			fitness(chromosome);
		}
		normalization(chromosomes);
		
		for(int i = 1; i <= MAX_GENERATIONS; i++) {			
			if(i==1) {
				Collections.sort(chromosomes);
				Chromosome localOptimal = new Chromosome();
				localOptimal.copy(chromosomes.get(0));
				historyBestChromosomes.add(localOptimal);
				bestChromosome.copy(chromosomes.get(0));

				printChromosomes();
				double roff = TASK_OFFLOADABLE_RATE;
				if(roff==0) {
					break;
				}				
			}
			
			selection(chromosomes);	
			for(int j = 1; j < chromosomes.size(); j=j+2) {
				Chromosome parent1 = chromosomes.get(j-1);
				Chromosome parent2 = chromosomes.get(j);
				singlePointCrossover(parent1, parent2, Environment.GA_CROSSOVER_RATE);				
			}
			if(TASK_OFFLOADABLE_RATE>0) {
				for(Chromosome chromosome : chromosomes) {
					mutation(chromosome, GA_MUTATION_RATE);
				}
			}
			
			for(Chromosome chromosome: chromosomes) {
				fitness(chromosome);
			}
			normalization(chromosomes);
			
			for(Chromosome chromosome: chromosomes) {
				Chromosome newChromosome = saOptimizeSearch(chromosome, 300);
				if(newChromosome!=chromosome) {
					chromosome.copy(newChromosome);
				}
			}			

			normalization(chromosomes);
			Collections.sort(chromosomes);
			Chromosome localOptimal = new Chromosome();
			localOptimal.copy(chromosomes.get(0));
			historyBestChromosomes.add(localOptimal);
			printChromosomes();
		}
		normalization(historyBestChromosomes);
		Collections.sort(historyBestChromosomes);
		bestChromosome.copy(historyBestChromosomes.get(0));
	}
	
	public static void main(String[] args) {
		MASAPolicy hybrid = new MASAPolicy();
		hybrid.init();
		long start = System.nanoTime();
		hybrid.hybridGeneticAlgorithm();
		long duration = System.nanoTime()-start;		
		
		System.out.println("population size: "+ POPULATION_SIZE);
		System.out.println("iteration number: "+ MAX_GENERATIONS);
		System.out.println("mobile: "+ hybrid.mdlist.size());
		System.out.println("edge: "+ hybrid.eslist.size());
		System.out.println("cloud: "+ hybrid.cslist.size());
		System.out.println("task/md: "+TASK_NUM_PER_MD);
		System.out.println("task: "+ hybrid.tasklist.size());
		double totalR0 = 0;
		double totalR1 = 0;
		double totalData = 0;
		for(Task task : hybrid.tasklist) {
			totalR0 += task.r0;
			totalR1 += task.r1;
			totalData += task.d;
		}
		System.out.println("R0+R1:"+ (totalR0+totalR1));
		System.out.println("R0:"+totalR0);
		System.out.println("R1:"+totalR1);
		System.out.println("Roff:"+ TASK_OFFLOADABLE_RATE);
		System.out.println("data size:"+totalData);
		System.out.println(hybrid.bestChromosome.sequence);
		System.out.println(hybrid.bestChromosome.fitnessValue+" : "+ hybrid.bestChromosome.energy+" : "+ hybrid.bestChromosome.violateTimeConstraintNum+" : "+hybrid.bestChromosome.scheduleLength+" : "+hybrid.bestChromosome.esutility+" : "+hybrid.bestChromosome.csutility);
        System.out.println("Iteration:"+MAX_GENERATIONS+", duration:"+duration+"(nano second)"+", "+duration/1000000.0+"(ms)");
        System.out.println("average one cycle:"+duration*1.0/MAX_GENERATIONS/1000000.0+"(ms)");
        
		System.out.println("MA-SA:"+POPULATION_SIZE+":"+MAX_GENERATIONS+":"+hybrid.mdlist.size()
							+":"+hybrid.eslist.size()+":"+hybrid.cslist.size()+":"+TASK_NUM_PER_MD
							+":"+hybrid.tasklist.size()+":"+(totalR0+totalR1)+":"+totalR0+":"+totalR1
							+":"+TASK_OFFLOADABLE_RATE+":"+totalData+":"+hybrid.bestChromosome.fitnessValue
							+":"+hybrid.bestChromosome.energy+":"+hybrid.bestChromosome.violateTimeConstraintNum
							+":"+hybrid.bestChromosome.scheduleLength+":"+(duration/1000000.0)+":"+(duration*1.0/MAX_GENERATIONS/1000000.0)
							+":"+hybrid.bestChromosome.esutility+":"+hybrid.bestChromosome.csutility);	
	}
}
