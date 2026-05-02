package cn.tfs.eaco.com;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import cn.tfs.eaco.core.CloudServer;
import cn.tfs.eaco.core.EdgeServer;
import cn.tfs.eaco.core.Environment;
import cn.tfs.eaco.core.MobileDevice;
import cn.tfs.eaco.core.Task;
import cn.tfs.eaco.core.VirtualMachine;

/**
 * A computation offloading method designed for IoT-enabled cloud-edge computing. 
 * It utilizes NSGA-III (Non-dominated Sorting Genetic Algorithm III) to solve the
 * multi-objective optimization problem, aiming to simultaneously minimize the 
 * execution time and energy consumption in IoT-enabled cloud-edge computing scenarios.
 * 
 * Reference:
 * X. Xu, Q. Liu, Y. Luo, K. Peng, X. Zhang, S. Meng, L. Qi, A computation offloading 
 * method over big data for IoT-enabled cloud-edge computing, Future Generation 
 * Computer Systems 95 (2019) 522¨C533.
 *
 * @author Duan Lintao
 * 
 */
public class COMAlgorithm extends Environment{
	public static final int MAX_INTERATION = 8;
	public static final int POPULATION_SIZE = 100;
	
    double calculateExecutionTime(OffloadingStrategy strategy) {
        double totalTime = 0;
        return totalTime;
    }

    double calculateEnergyConsumption(OffloadingStrategy strategy) {
        double totalEnergy = 0;
        return totalEnergy;
    }

    public List<OffloadingStrategy> initializePopulation(int size) {
    	List<OffloadingStrategy> oss = new Vector<>();
		Random random = new Random(1);
		for(int i = 1; i <= size; i++) {
			OffloadingStrategy os = new OffloadingStrategy(tasklist.size());
			for(int j = 1; j <= tasklist.size(); j++) {
				os.strategy[j-1]=random.nextInt(3);	
			}
			oss.add(os);			
		}
		return oss;
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
	
	public void allocationResource(int[] sequence) {
		clean();
		for(int i = 1; i <= tasklist.size(); i++) {
			Task task = tasklist.get(i-1);
			MobileDevice md = task.md;
			EdgeServer es = md.edgeServer;
			if(es.state == false)
				es.state = true;
			
			int policy = sequence[i-1];
			task.policy = policy;
			md.pendingTasks.add(task);
			
			if(policy == 1) {
				es.tasks.add(task);
			}else if(policy == 2) {
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
					if(Math.abs(diff) < 1e-10) {
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
		for(Task task : cloudTaskList) {
			assignTaskCloudAimTime(task);
		}
	}

    public double[][] evaluateFitness(List<OffloadingStrategy> population, List<Task> schedule) {
        double[][] fitness = new double[population.size()][2]; // [schedulability, energy consuption]

        for (int i = 0; i < population.size(); i++) {
        	double totalEnergy = 0;
    		int totalViolateTimeConstraintNum = 0;
    		double esutility = 0;
    		int esonnum = 0;
    		double csutility = 0;
    		int csonnum = 0;
    		
            OffloadingStrategy strategy = population.get(i);
            allocationResource(strategy.strategy);
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
    		
    		allocationResource(strategy.strategy);
    		for(MobileDevice md : mdlist) {
    			totalViolateTimeConstraintNum += md.violateTimeConstraintNum();
    			if(md.finishTimeLastTask > strategy.scheduleLength) {
    				strategy.scheduleLength = md.finishTimeLastTask;
    			}
    		}
    		for(EdgeServer es : eslist) {
    			totalViolateTimeConstraintNum += es.violateTimeConstraintNum();
    			if(es.finishTimeLastTask > strategy.scheduleLength) {
    				strategy.scheduleLength = es.finishTimeLastTask;
    			}
    		}
    		
    		for(CloudServer cs : cslist) {
    			totalViolateTimeConstraintNum += cs.violateTimeConstraintNum();
    			if(cs.finishTimeLastTask > strategy.scheduleLength) {
    				strategy.scheduleLength = cs.finishTimeLastTask;
    			}
    		}
    		
            fitness[i][0] = totalViolateTimeConstraintNum;
            fitness[i][1] = totalEnergy;  
            
            strategy.energy = totalEnergy;
            strategy.violateTimeConstraintNum = totalViolateTimeConstraintNum;
            strategy.fitnessValue = fitness[i];
    		strategy.esutility = esutility;
    		strategy.csutility = csutility;
        }       
        return fitness;
    }

    public OffloadingStrategy optimize(List<Task> schedule) throws Exception{
        NSGA3 nsga3 = new NSGA3();
        List<double[]> refPoints = nsga3.generateReferencePoints(2, 4); 

        OffloadingStrategy bestStrategy = new OffloadingStrategy(schedule.size());
        List<OffloadingStrategy> population = initializePopulation(schedule.size());
        for (int iter = 0; iter < MAX_INTERATION; iter++) {
        	List<OffloadingStrategy> offspring = nsga3.evolvePopulation(population);
        	List<OffloadingStrategy> combined = new ArrayList<>(population);
            combined.addAll(offspring);
            
            double[][] fitness = evaluateFitness(combined, schedule);
            List<List<Integer>> fronts = nsga3.nonDominatedSort(fitness);
            population = nsga3.selectNextGeneration(combined, fronts, refPoints);
        }
        bestStrategy = nsga3.selectBestStrategy(population);
        return bestStrategy;
    }
    
    public static void main(String[] args) throws Exception{
    	COMAlgorithm com = new COMAlgorithm();
    	com.init();
    	long start = System.nanoTime();
        OffloadingStrategy strategy = com.optimize(com.tasklist);
        long duration = System.nanoTime()-start;
		System.out.println("population size: "+ POPULATION_SIZE);
		System.out.println("iteration number: "+ MAX_INTERATION);
		System.out.println("mobile: "+ com.mdlist.size());
		System.out.println("edge: "+ com.eslist.size());
		System.out.println("cloud: "+ com.cslist.size());
		System.out.println("task/md: "+TASK_NUM_PER_MD);
		System.out.println("task: "+ com.tasklist.size());
		double totalR0 = 0;
		double totalR1 = 0;
		double totalData = 0;
		for(Task task : com.tasklist) {
			totalR0 += task.r0;
			totalR1 += task.r1;
			totalData += task.d;
		}
		System.out.println("R0+R1:"+ (totalR0+totalR1));
		System.out.println("R0:"+totalR0);
		System.out.println("R1:"+totalR1);
		System.out.println("Roff:"+ TASK_OFFLOADABLE_RATE);
		System.out.println("data size:"+totalData);
		System.out.println(Arrays.toString(strategy.strategy));
		System.out.println(strategy.fitnessValue+" : "+ strategy.energy+" : "+ strategy.violateTimeConstraintNum+" : "+strategy.scheduleLength+" : "+strategy.esutility+" : "+strategy.csutility);
        System.out.println("Iteration:"+MAX_INTERATION+", duration:"+duration+"(nano second)"+", "+duration/1000000.0+"(ms)");
        System.out.println("average one cycle:"+duration*1.0/MAX_INTERATION/1000000.0+"(ms)");
        
		System.out.println("COM:"+POPULATION_SIZE+":"+MAX_INTERATION+":"+com.mdlist.size()
							+":"+com.eslist.size()+":"+com.cslist.size()+":"+TASK_NUM_PER_MD
							+":"+com.tasklist.size()+":"+(totalR0+totalR1)+":"+totalR0+":"+totalR1
							+":"+TASK_OFFLOADABLE_RATE+":"+totalData+":"+strategy.fitnessUtility
							+":"+strategy.energy+":"+strategy.violateTimeConstraintNum
							+":"+strategy.scheduleLength+":"+(duration/1000000.0)+":"+(duration*1.0/MAX_INTERATION/1000000.0)
							+":"+strategy.esutility+":"+strategy.csutility);
	}
}