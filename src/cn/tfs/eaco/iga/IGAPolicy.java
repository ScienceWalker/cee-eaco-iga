package cn.tfs.eaco.iga;

import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;

import cn.tfs.eaco.core.CloudServer;
import cn.tfs.eaco.core.EdgeServer;
import cn.tfs.eaco.core.Environment;
import cn.tfs.eaco.core.MobileDevice;
import cn.tfs.eaco.core.Task;
import cn.tfs.eaco.core.VirtualMachine;

/**
 * IGA algorithm : an energy-aware computation offloading strategy, aiming to minimize
 * the total system energy consumption and maximize task schedulability ratio.
 * 
 * @author Duan Lintao
 * duanlintao@cdu.edu.cn
 *
 */
public class IGAPolicy extends Environment{
	public static final int POPULATION_SIZE = 100;
	public static final int CHROMOSOME_LENGTH = Environment.TASK_NUM;
	public static final int MAX_GENERATIONS = 100;
	public static final double TARGET_FITNESS = 0;
	
	public static final int MAX_LOCALTRY_NUM = 1;
	
	public Vector<Chromosome> chromosomes;
	public Vector<Chromosome> historyBestChromosomes;
	public Chromosome bestChromosome;
	
	public IGAPolicy() {
		super();
		chromosomes = new Vector<>();
		historyBestChromosomes = new Vector<>();
		bestChromosome = new Chromosome();
		bestChromosome.fitnessValue=Double.MAX_VALUE;
	}
	
	public void initializePopulation() {
		double roff = TASK_OFFLOADABLE_RATE;
		Random random = new Random(1);
		for(int i = 1; i <= POPULATION_SIZE; i++) {
			Chromosome chromosome = new Chromosome();
			for(int j = 1; j <= CHROMOSOME_LENGTH; j++) {
				
				if(roff <=0) {
					chromosome.add(0);
				}else {
					chromosome.add(random.nextInt(3));
				}
			}
			chromosomes.add(chromosome);			
		}
	}

	public void clean() {
		for(MobileDevice md : mdlist) {
			md.pendingTasks.clear();
		}
		for(EdgeServer es : eslist) {
			for(VirtualMachine vm : es.vms) {
				vm.pendingTasks.clear();
			}
			es.tasks.clear();
			es.firstArrivalTime = -1;
			es.finishTimeLastTask = -1;
			es.averageUtility = 0;
			es.state = false;
		}
		for(CloudServer cs : cslist) {
			for(VirtualMachine vm : cs.vms) {
				vm.pendingTasks.clear();
			}
			cs.tasks.clear();
			cs.firstArrivalTime = -1;
			cs.finishTimeLastTask = -1;
			cs.averageUtility = 0;
			cs.state = false;
		}
		cloudTaskList.clear();
	}
	
	public void allocationResource(Vector<Integer> sequence) {
		clean();
		for(int i = 1; i <= tasklist.size(); i++) {
			Task task = tasklist.get(i-1);
			MobileDevice md = task.md;
			EdgeServer es = md.edgeServer;
			if(es.state == false)
				es.state = true;
			
			int policy = sequence.get(i-1);
			task.policy = policy;
			md.pendingTasks.add(task);
			
			if((policy == 1) && (TASK_OFFLOADABLE_RATE>0)) {
				es.tasks.add(task);
			}else if((policy == 2) && (TASK_OFFLOADABLE_RATE>0)) {
				cloudTaskList.add(task);
			}
		}

		for(MobileDevice md : mdlist) {
			Collections.sort(md.pendingTasks, new Comparator<Task>() {
				@Override
				public int compare(Task t1, Task t2) {					
					double diff = t1.dl-t2.dl;
					if(Math.abs(diff)<1e-10) {
						return 0;
					}else if(diff>0)
						return 1;
					else 
						return -1;
				}
			});
		}

		for(MobileDevice md: mdlist) {
			double waitingTime = 0;
			for(Task task : md.pendingTasks) {
				if(task.policy==0) {
					waitingTime += (task.r0+task.r1)/md.sd;
				}else if(task.policy==1) {
					waitingTime += task.r0/md.sd;
					task.arrivaleServerTime=task.d/NET_SE+waitingTime;
				}else if(task.policy == 2) {
					waitingTime += task.r0/md.sd;
					task.arrivaleServerTime=task.d/NET_SE+task.d/NET_SC+waitingTime;
				}
			}
		}

		for(EdgeServer es : eslist) {
			Collections.sort(es.tasks, new Comparator<Task>() {
				@Override
				public int compare(Task t1, Task t2) {					
					double diff = t1.arrivaleServerTime-t2.arrivaleServerTime;
					if(Math.abs(diff) < 1e-6) {
						double dldiff = t1.dl - t2.dl;
						if(Math.abs(dldiff) < 1e-10) {
							return 0;
						}else if(dldiff > 0) {
							return 1;
						}else {
							return -1;
						}
					}else if(diff>0)
						return 1;
					else
						return -1;					
				}
			});
			es.firstArrivalTime=es.tasks.size()>0?es.tasks.get(0).arrivaleServerTime:0;
			for(Task task : es.tasks) {
				es.assignTask(task);
			}
		}	

		Collections.sort(cloudTaskList, new Comparator<Task>() {
			@Override
			public int compare(Task t1, Task t2) {
				double diff = t1.arrivaleServerTime-t2.arrivaleServerTime;
				if(Math.abs(diff) < 1e-10) {
					double dldiff = t1.dl - t2.dl;
					if(Math.abs(dldiff) < 1e-6) {
						return 0;
					}else if(dldiff > 0) {
						return 1;
					}else {
						return -1;
					}
				}else if(diff>0)
					return 1;
				else
					return -1;
			}
		});
		for(Task task : cloudTaskList) {
			assignTaskCloudAimTime(task);
		}
	}
	
	public void fitness(Chromosome chromosome) {
		double totalEnergy = 0;
		int totalViolateTimeConstraintNum = 0;
		double esutility = 0;
		int esonnum = 0;
		double csutility = 0;
		int csonnum = 0;
		
		allocationResource(chromosome.sequence);
		for(MobileDevice md : mdlist) {
			totalEnergy += md.getEnergyforComputing();
		}
		
		for(EdgeServer es : eslist) {
			totalEnergy += es.getEnergyforTransmitting();
			totalEnergy += es.getEnergyforComputing();
			if(es.averageUtility>0) {
				esutility += es.averageUtility;
				esonnum++;
			}
		}
		if(esonnum > 0)
			esutility/=esonnum;
		
		for(CloudServer cs : cslist) {
			totalEnergy += cs.getEnergyforTransmitting();
			totalEnergy += cs.getEnergyforComputing();
			if(cs.averageUtility>0) {
				csutility += cs.averageUtility;
				csonnum++;
			}
		}
		if(csonnum>0)
			csutility/=csonnum;
		
		allocationResource(chromosome.sequence);		
		for(MobileDevice md : mdlist) {
			totalViolateTimeConstraintNum += md.violateTimeConstraintNum();
			if(md.finishTimeLastTask > chromosome.scheduleLength) {
				chromosome.scheduleLength = md.finishTimeLastTask;
			}
		}
		for(EdgeServer es : eslist) {
			totalViolateTimeConstraintNum += es.violateTimeConstraintNum();
			if(es.finishTimeLastTask > chromosome.scheduleLength) {
				chromosome.scheduleLength = es.finishTimeLastTask;
			}
		}
		
		for(CloudServer cs : cslist) {
			totalViolateTimeConstraintNum += cs.violateTimeConstraintNum();
			if(cs.finishTimeLastTask > chromosome.scheduleLength) {
				chromosome.scheduleLength = cs.finishTimeLastTask;
			}
		}
		chromosome.energy = totalEnergy;
		chromosome.violateTimeConstraintNum = totalViolateTimeConstraintNum;
		chromosome.esutility = esutility;
		chromosome.csutility = csutility;
	}
	
	public void printChromosomes() {
		for(Chromosome chromosome : chromosomes) {
			System.out.println(chromosome.sequence+" : "+chromosome.fitnessValue+" : "+ chromosome.energy+" : "+ chromosome.violateTimeConstraintNum+" : "+chromosome.scheduleLength+" : "+chromosome.esutility+" : "+chromosome.csutility+" : "+" : "+TASK_NUM);
		}
	}
	
	public void printHistoryBestChromosomes() {
		for(Chromosome chromosome : historyBestChromosomes) {
			System.out.println(chromosome.sequence+" : "+chromosome.fitnessValue+" : "+ chromosome.energy+" : "+ chromosome.violateTimeConstraintNum+" : "+chromosome.scheduleLength+" : "+chromosome.esutility+" : "+chromosome.csutility+" : "+" : "+TASK_NUM);
		}
	}
	
	public double sumOfFitnessValue(Vector<Chromosome> dpopulation) {
		double sum = 0;
		for(Chromosome chromosome : dpopulation) {
			sum += chromosome.fitnessValue;
		}
		return sum;
	}
	
	public void selection(Vector<Chromosome> dpopulation) {
		Collections.sort(dpopulation);
		Vector<Chromosome> nextGeneration = new Vector<>();
		int eliteSize = (int)(Environment.GA_ELITE_PERCENT*POPULATION_SIZE);
		int remainSize = POPULATION_SIZE - eliteSize;
		
		for(int i = 0; i < eliteSize; i++) {
			nextGeneration.add(dpopulation.get(i));
		}

		double sumfitness = sumOfFitnessValue(dpopulation);
		for(int i = 0; i < remainSize; i++) {			
			double prob = Math.random();
			double accprob = 0;
			for(Chromosome chromosome : dpopulation) {
				accprob += chromosome.fitnessValue/sumfitness;
				if(prob <= accprob) {
					Chromosome newchromosome = new Chromosome();					
					newchromosome.copy(chromosome);	
					nextGeneration.add(newchromosome);
					break;
				}
			}
		}
		chromosomes = nextGeneration;
	}
	
	public void singlePointCrossover(Chromosome parent1, Chromosome parent2, double crossoverRate) {
		Random random = new Random();
		if (random.nextDouble() > crossoverRate) {
			return;
		}
		int point = random.nextInt(parent1.sequence.size()-1);
		for(int i = point + 1; i < parent1.sequence.size(); i++) {
			int tmp = parent1.sequence.get(i);
			parent1.sequence.set(i, parent2.sequence.get(i));
			parent2.sequence.set(i, tmp);
		}
	}
	
	public void mutation(Chromosome chromosome, double mutationRate) {
		Random random = new Random();
		for(int i = 0; i < chromosome.sequence.size(); i++) {
			if(random.nextDouble() < mutationRate) {
				int current = chromosome.sequence.get(i);
				int newValue;
				do {
					newValue = random.nextInt(3); 
				} while (newValue == current);
				chromosome.sequence.set(i, newValue);				
			}
		}
	}
	
	public Vector<Chromosome> getNeibours(Chromosome chromosome){
		Vector<Chromosome> chromosomes = new Vector<>();
		for(int i = 0; i < chromosome.sequence.size(); i++) {			
			int mode = chromosome.sequence.get(i);
			if(mode == 0) {
				Chromosome newChromosome = new Chromosome();
				newChromosome.copy(chromosome);
				newChromosome.sequence.set(i, 1);
				fitness(newChromosome);
				chromosomes.add(newChromosome);
			} else if(mode == 1) {
				Chromosome newChromosome1 = new Chromosome();
				newChromosome1.copy(chromosome);
				newChromosome1.sequence.set(i, 0);
				fitness(newChromosome1);
				chromosomes.add(newChromosome1);
				
				Chromosome newChromosome2 = new Chromosome();
				newChromosome2.copy(chromosome);
				newChromosome2.sequence.set(i, 2);
				fitness(newChromosome2);
				chromosomes.add(newChromosome2);

			}else if(mode == 2) {
				Chromosome newChromosome = new Chromosome();
				newChromosome.copy(chromosome);
				newChromosome.sequence.set(i, 1);
				fitness(newChromosome);
				chromosomes.add(newChromosome);
			}
		}
		return chromosomes;
	}

	public Vector<Chromosome> generateImages(Vector<Chromosome> population){
		Vector<Chromosome> dpopulation = new Vector<Chromosome>();
		for(int i = 1; i < population.size(); i++) {
			Chromosome self = population.get(i);
			Chromosome forward = new Chromosome();
			for(int j = 0; j < self.sequence.size(); j++) {
				int gene = 0;
				int sum = bestChromosome.sequence.get(j)+self.sequence.get(j);
				if(sum==1) {
					gene = self.sequence.get(j);
				}else if(sum == 3) {
					gene = self.sequence.get(j);
				}else {
					gene = sum/2;
				}				
				forward.sequence.add(j, gene);
			}
			fitness(forward);
			dpopulation.add(self);
			dpopulation.add(forward);
		}
		return dpopulation;
	}
	
	public void gaSaAlgorithm() {
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
				Vector<Chromosome> newChromosomes = getNeibours(chromosome);
				newChromosomes.add(chromosome);
				Chromosome newChromosome = getbestNeibourAndMe(newChromosomes);
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

	public void normalization(Vector<Chromosome> chromosomes) {
		double emax = Double.MIN_VALUE; 
		double emin = Double.MAX_VALUE;
		double vmax = Double.MIN_VALUE; 
		double vmin = Double.MAX_VALUE;
		for(Chromosome chromosome : chromosomes) {
			if(emax < chromosome.energy) {
				emax = chromosome.energy;
			}
			if(emin > chromosome.energy) {
				emin = chromosome.energy;
			}
			if(vmax < chromosome.violateTimeConstraintNum) {
				vmax = chromosome.violateTimeConstraintNum;
			}
			if(vmin > chromosome.violateTimeConstraintNum) {
				vmin = chromosome.violateTimeConstraintNum;
			}
		}
		for(Chromosome chromosome : chromosomes) {
			double norme = (chromosome.energy-emin)/(emax-emin+Math.pow(10, -12)); 
			double normv = (chromosome.violateTimeConstraintNum-vmin)/(vmax-vmin+Math.pow(10, -12)); 
			chromosome.fitnessValue = Environment.OBJCTIVE_WEIGHT*norme+(1-Environment.OBJCTIVE_WEIGHT)*normv;
		}
	}
	
	public Chromosome getbestNeibourAndMe(Vector<Chromosome> chromosomes) {
		Chromosome bestChromosome = null;
		double emax = Double.MIN_VALUE; 
		double emin = Double.MAX_VALUE;
		double vmax = Double.MIN_VALUE; 
		double vmin = Double.MAX_VALUE;
		for(Chromosome chromosome : chromosomes) {
			if(emax < chromosome.energy) {
				emax = chromosome.energy;
			}
			if(emin > chromosome.energy) {
				emin = chromosome.energy;
			}
			if(vmax < chromosome.violateTimeConstraintNum) {
				vmax = chromosome.violateTimeConstraintNum;
			}
			if(vmin > chromosome.violateTimeConstraintNum) {
				vmin = chromosome.violateTimeConstraintNum;
			}
		}		
		for(Chromosome chromosome : chromosomes) {
			double norme = (chromosome.energy-emin)/(emax-emin+Math.pow(10, -12)); 
			double normv = (chromosome.violateTimeConstraintNum-vmin)/(vmax-vmin+Math.pow(10, -12)); 
			chromosome.fitnessValue = Environment.OBJCTIVE_WEIGHT*norme+(1-Environment.OBJCTIVE_WEIGHT)*normv;
			if(bestChromosome==null||bestChromosome.fitnessValue>chromosome.fitnessValue) {
				bestChromosome=chromosome;
			}
		}
		return bestChromosome;
	}	

	public static void main(String[] args) {
		IGAPolicy igap = new IGAPolicy();
		igap.init();
		long start = System.nanoTime();
		igap.gaSaAlgorithm();
		long duration = System.nanoTime()-start;		
		
		double totalR0 = 0;
		double totalR1 = 0;
		double totalData = 0;
		for(Task task : igap.tasklist) {
			totalR0 += task.r0;
			totalR1 += task.r1;
			totalData += task.d;
		}
        
		System.out.println("IGA:"+POPULATION_SIZE+":"+MAX_GENERATIONS+":"+igap.mdlist.size()
							+":"+igap.eslist.size()+":"+igap.cslist.size()+":"+TASK_NUM_PER_MD
							+":"+igap.tasklist.size()+":"+(totalR0+totalR1)+":"+totalR0+":"+totalR1
							+":"+TASK_OFFLOADABLE_RATE+":"+totalData+":"+igap.bestChromosome.fitnessValue
							+":"+igap.bestChromosome.energy+":"+igap.bestChromosome.violateTimeConstraintNum
							+":"+igap.bestChromosome.scheduleLength+":"+(duration/1000000.0)+":"+(duration*1.0/MAX_GENERATIONS/1000000.0)
							+":"+igap.bestChromosome.esutility+":"+igap.bestChromosome.csutility);
	}
}
