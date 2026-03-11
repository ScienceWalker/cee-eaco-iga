package cn.tfs.eaco.core;

import java.util.List;
import java.util.Random;
import java.util.Vector;
/**
 * a three-layer CEE collaborative computing environment
 * 
 * @author Duan Lintao
 * duanlintao@cdu.edu.cn
 *
 */
public class Environment {
	public static final int TASK_NUM_PER_MD =1;
	public static final int MD_NUM = 10;
	public static final int TASK_NUM = TASK_NUM_PER_MD*MD_NUM;
	public static final int ES_CONNECTION_SIZE = 4;
	public static final int ES_VM_SIZE = 2;
	public static final int CS_SERVICE_SIZE = 5;
	
	public static final double MD_SPEED = 0.3;//BI/second
	public static final double ES_VM_SPEED = 2.5;//BI/second
	public static final double ES_CAPACITY = ES_VM_SPEED*ES_VM_SIZE;
	public static final double CS_VM_SPEED = 3.5;//BI/second
	public static final double CS_CAPACITY = CS_VM_SPEED*CS_SERVICE_SIZE;
	
	public static final double MD_POWER_ALPHA = 1;
	public static final double MD_POWER_BETA = 2;
	public static final double ES_POWER_IDLE = 497;//Watt, IBM NX360
	public static final double ES_POWER_MAX = 2414;//Watt, IBM NX360
	public static final double CS_POWER_IDLE = 832;//Watt, DELL M820
	public static final double CS_POWER_MAX = 3997;//Watt, DELL M820
	
	public static final double NET_SE = 40;//Mbps
	public static final double NET_SC = 90;//Mbps
	public static final double NET_POWER_W = 30;//Mbps
	public static final double NET_POWER_BETA = 2.0;
	public static final double NET_WIRED_PBASE = 50;//Watt
	
	public static final double NET_WIRED_PRATE = 0.5;//Watt/Gbps
	public static final double NET_WIRED_R = 10;//Gbps
	
	public static final double TASK_WORKLOAD = 2.0;//BI	
	public static final double TASK_TRANS_DATA_SIZE = 0.4;//Mb	
	public static final double TASK_DEADLINE = 9;//second

	public static final double TASK_WORKLOAD_LOWER = 1.0;//BI
	public static final double TASK_WORKLOAD_UPPER = 4.0;//BI
	public static final int TASK_WORKLOAD_STEPS = 4;
	public static final double TASK_TRANS_DATA_SIZE_LOWER = 0.6;//Mb
	public static final double TASK_TRANS_DATA_SIZE_UPPER = 0.9;//Mb
	public static final int TASK_TRANS_DATA_STEPS = 4;	
	public static final double TASK_DEADLINE_LOWER = 5;//second
	public static final double TASK_DEADLINE_UPPER = 10;//second
	public static final int TASK_DEADLINE_STEPS = 6;	
	
	public static final double TASK_OFFLOADABLE_RATE = 0.9;//r1/(r0+r1)	
	public static final double OBJCTIVE_WEIGHT = 0.5;
	
	public static final double GA_ELITE_PERCENT = 0.5;
	public static final double GA_MUTATION_RATE = 0.01;
	public static final double GA_CROSSOVER_RATE = 1;
	
	public List<MobileDevice> mdlist;
	public List<EdgeServer> eslist;
	public List<CloudServer> cslist;
	public List<Task> tasklist;
	public List<Task> cloudTaskList;
	
	public Environment() {
		mdlist = new Vector<>();
		eslist = new Vector<>();
		cslist= new Vector<>();
		tasklist = new Vector<>();
		cloudTaskList = new Vector<>();
	}

	public void init() {
		int tid = 1;
		int esid = MD_NUM;
		Random random = new Random(1);
		for(int i = 1; i <= MD_NUM; i++) {
			MobileDevice md = new MobileDevice(i, MD_SPEED, PowerModel.powerMobileDevice(MD_POWER_ALPHA, MD_POWER_BETA, MD_SPEED), 0.0);
			
			double roff = TASK_OFFLOADABLE_RATE;
			if(roff==0) {
				System.out.println("mobile device:"+i+", speed:"+MD_SPEED);
			}
			
			for(int j = 1; j <= TASK_NUM_PER_MD; j++) {
				double workload = TASK_WORKLOAD_LOWER + random.nextInt(TASK_WORKLOAD_STEPS);
				double r1 = workload*TASK_OFFLOADABLE_RATE;
				double r0 = workload-r1;
				double data = TASK_TRANS_DATA_SIZE_LOWER + 0.1*random.nextInt(TASK_TRANS_DATA_STEPS);
				double deadline = TASK_DEADLINE_LOWER + random.nextInt(TASK_DEADLINE_STEPS);
				Task task = new Task(tid++, r0, r1, data, deadline);
				
				task.md = md;
				tasklist.add(task);
				
				if(roff==0) {
					System.out.println("workload:"+workload+", r0:"+r0+",deadline:"+deadline);
				}
			}
			mdlist.add(md);

			if(i%ES_CONNECTION_SIZE==0) {
				esid ++;
				EdgeServer es = new EdgeServer(esid, ES_VM_SIZE, ES_POWER_IDLE,ES_POWER_MAX);
				for(int j = 1; j <= es.maxVMsNum; j++) {
					VirtualMachine vm = new VirtualMachine(j,ES_VM_SPEED);
					es.vms.add(vm);
				}
				for(int j = 1; j <= ES_CONNECTION_SIZE; j++) {
					MobileDevice prior = mdlist.get(mdlist.size()-j);
					prior.setEdgeServer(es);
					es.mds.add(prior);
				}
				eslist.add(es);
			}
		}
		int domainMDs = MD_NUM-(esid-MD_NUM)*ES_CONNECTION_SIZE;
		if(domainMDs > 0) {
			EdgeServer es = new EdgeServer(++esid, ES_VM_SIZE, ES_POWER_IDLE,ES_POWER_MAX);
			for(int j = 1; j <= es.maxVMsNum; j++) {
				VirtualMachine vm = new VirtualMachine(j,ES_VM_SPEED);
				es.vms.add(vm);
			}
			for(int j = 1; j <= domainMDs; j++) {
				MobileDevice prior = mdlist.get(mdlist.size()-j);
				prior.setEdgeServer(es);
				es.mds.add(prior);
			}
			eslist.add(es);
		}

		int csid = esid;
		for(int k = 1; k <= TASK_NUM/CS_SERVICE_SIZE+1; k++) {
			csid ++;
			CloudServer cs = new CloudServer(csid, CS_SERVICE_SIZE, CS_POWER_IDLE, CS_POWER_MAX);
			for(int j = 1; j <= cs.maxVMsNum; j++) {
				VirtualMachine vm = new VirtualMachine(j,CS_VM_SPEED);
				cs.vms.add(vm);
			}
			cslist.add(cs);
		}
	}
	
	public void assignTaskCloudAimTime(Task task) {
		double minComputingTime = Double.MAX_VALUE;
		VirtualMachine minVM = null;
		CloudServer minCS = null;
		for(CloudServer cs : cslist) {
			for(VirtualMachine vm : cs.vms) {
				if(vm.getComputingTime() < minComputingTime) {
					minComputingTime = vm.getComputingTime();
					minVM = vm;
					minCS = cs;
				}
			}
		}
		if(minVM != null) {
			if(minCS.firstArrivalTime<0) {
				minCS.firstArrivalTime = task.arrivaleServerTime;
			}
			if(minComputingTime < (task.arrivaleServerTime-minCS.firstArrivalTime)) {
				Task nulltask = new Task(0, 0, 0, 0, task.arrivaleServerTime-minCS.firstArrivalTime-minComputingTime);
				minVM.add(nulltask);
			}
			if(minCS.state==false)
				minCS.state = true;
			minVM.add(task);
		}
	}
}
