package cn.tfs.eaco.ma2opt;

import java.util.Collections;

import cn.tfs.eaco.core.Environment;
import cn.tfs.eaco.core.Task;
import cn.tfs.eaco.iga.IGAPolicy;
import cn.tfs.eaco.iga.Chromosome;

/**
 * A memetic algorithm that integrates GA with the 2-OPT local search heuristic. 
 * 2-OPT was originally developed for routing problems, where it improves a solution
 * by swapping two nodes in a path to eliminate crossings. In the context of task
 * scheduling, it is adapted to perform pairwise exchanges of task assignments to 
 * explore the local neighborhood.
 * 
 * @author Duan Lintao
 *
 */
public class MA2OPTPolicy extends IGAPolicy{
	
	private Chromosome twoOptLocalSearch(Chromosome individual) {
		Chromosome chromosome1 = new Chromosome();
		chromosome1.copy(individual);
		fitness(chromosome1);

        double bestEnergy = chromosome1.energy;
        double bestTasksNum = chromosome1.violateTimeConstraintNum;
        boolean improved = true;
        int iterations = 0;
        int maxIterations = 5;
        while (improved && iterations < maxIterations) {
           improved = false;
           iterations++;
           out:for (int i = 0; i < chromosome1.sequence.size() - 1; i++) {
                for (int j = i + 2; j < chromosome1.sequence.size(); j++) {
                    Collections.reverse(chromosome1.sequence.subList(i, j));
                    fitness(chromosome1);
                    if ((bestEnergy > chromosome1.energy && bestTasksNum>=chromosome1.violateTimeConstraintNum)||
                    	(bestEnergy >= chromosome1.energy && bestTasksNum>chromosome1.violateTimeConstraintNum)) {
                        improved = true;
                        bestEnergy = chromosome1.energy;
                        bestTasksNum=chromosome1.violateTimeConstraintNum;
                        if(improved)
                        	break out;                        
                    } else {
                        Collections.reverse(chromosome1.sequence.subList(i, j));
                    }
                }
            }  
        }        
        return chromosome1;
    }
	
	public void memeticAlgorithm() {

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
				Chromosome newChromosome = twoOptLocalSearch(chromosome);
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
		MA2OPTPolicy memetic = new MA2OPTPolicy();
		memetic.init();
		long start = System.nanoTime();
		memetic.memeticAlgorithm();;
		long duration = System.nanoTime()-start;		

		System.out.println("population size: "+ POPULATION_SIZE);
		System.out.println("iteration number: "+ MAX_GENERATIONS);
		System.out.println("mobile: "+ memetic.mdlist.size());
		System.out.println("edge: "+ memetic.eslist.size());
		System.out.println("cloud: "+ memetic.cslist.size());
		System.out.println("task/md: "+TASK_NUM_PER_MD);
		System.out.println("task: "+ memetic.tasklist.size());
		double totalR0 = 0;
		double totalR1 = 0;
		double totalData = 0;
		for(Task task : memetic.tasklist) {
			totalR0 += task.r0;
			totalR1 += task.r1;
			totalData += task.d;
		}
		System.out.println("R0+R1:"+ (totalR0+totalR1));
		System.out.println("R0:"+totalR0);
		System.out.println("R1:"+totalR1);
		System.out.println("Roff:"+ TASK_OFFLOADABLE_RATE);
		System.out.println("data size:"+totalData);
		System.out.println(memetic.bestChromosome.sequence);
		System.out.println(memetic.bestChromosome.fitnessValue+" : "+ memetic.bestChromosome.energy+" : "+ memetic.bestChromosome.violateTimeConstraintNum+" : "+memetic.bestChromosome.scheduleLength+" : "+memetic.bestChromosome.esutility+" : "+memetic.bestChromosome.csutility);
        System.out.println("Iteration:"+MAX_GENERATIONS+", duration:"+duration+"(nano second)"+", "+duration/1000000.0+"(ms)");
        System.out.println("average one cycle:"+duration*1.0/MAX_GENERATIONS/1000000.0+"(ms)");
        
		System.out.println("MA-2OPT:"+POPULATION_SIZE+":"+MAX_GENERATIONS+":"+memetic.mdlist.size()
							+":"+memetic.eslist.size()+":"+memetic.cslist.size()+":"+TASK_NUM_PER_MD
							+":"+memetic.tasklist.size()+":"+(totalR0+totalR1)+":"+totalR0+":"+totalR1
							+":"+TASK_OFFLOADABLE_RATE+":"+totalData+":"+memetic.bestChromosome.fitnessValue
							+":"+memetic.bestChromosome.energy+":"+memetic.bestChromosome.violateTimeConstraintNum
							+":"+memetic.bestChromosome.scheduleLength+":"+(duration/1000000.0)+":"+(duration*1.0/MAX_GENERATIONS/1000000.0)
							+":"+memetic.bestChromosome.esutility+":"+memetic.bestChromosome.csutility);
	}
}
