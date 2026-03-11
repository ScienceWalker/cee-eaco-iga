package cn.tfs.eaco.core;

import java.util.List;
import java.util.Vector;
/**
 * Edge Server j (ESj): represented by a quadruple (vnj, vs j, pj, cnj), 
 * where vnj represents the number of VMs on j, vs j denotes the speed of 
 * the VMs on j, pj indicates the power consumption of j, and cnj specifies 
 * the maximum number of EDs that j can simultaneously connect to.
 * 
 * @author Duan Lintao
 * duanlintao@cdu.edu.cn
 * 
 */
public class EdgeServer {
	public int esid;
	public List<MobileDevice> mds;
	public List<Task> tasks;
	public List<VirtualMachine> vms;
	public int maxVMsNum;
	public double pidle;
	public double pmax;
	
	public double firstArrivalTime;
	
	public double finishTimeLastTask; 	
	public double averageUtility;
	
	public boolean state;
	
	public EdgeServer() {
		mds = new Vector<>();
		tasks = new Vector<>();
		vms = new Vector<>();
		state = false;
	}

	public EdgeServer(int esid, int maxVMsNum, double pidle, double pmax) {
		super();
		this.esid = esid;
		this.maxVMsNum = maxVMsNum;
		this.pidle = pidle;
		this.pmax = pmax;
		this.mds = new Vector<>();
		this.tasks = new Vector<>();
		this.vms = new Vector<>();
		this.state = false;
	}
	
	public boolean assignTask(Task task) {
		double minComputingTime = Double.MAX_VALUE;
		VirtualMachine minVM = null;
		for(VirtualMachine vm : vms) {
			if(vm.getComputingTime()< minComputingTime) {
				minComputingTime = vm.getComputingTime();
				minVM = vm;
			}
		}
		if(minVM!=null) {
			if(minComputingTime < (task.arrivaleServerTime-firstArrivalTime)) {
				Task nulltask = new Task(0, 0, 0, 0, task.arrivaleServerTime-firstArrivalTime-minComputingTime);
				minVM.add(nulltask);
			}
			minVM.add(task);
			return true;
		}
		return false;
	}
	
	public double getUtility() {
		int activeVMsNum = 0;
		for(int i = 1; i <= vms.size(); i++) {
			VirtualMachine vm = vms.get(i-1);
			Task task = vm.get();
			if(task!=null && task.tid!=0) {
				activeVMsNum ++;
			}
		}
		return activeVMsNum*1.0/maxVMsNum;
	}
	
	public double getPe() {
		double utility = getUtility();
		return PowerModel.powerServer(pidle, pmax, utility);
	}
	
	public double getEnergyforTransmitting() {
		double totalEnergy = 0;
		for(VirtualMachine vm : vms) {
			for(Task task : vm.pendingTasks) {
				double power = PowerModel.powerTransimitEndEdge(Environment.NET_POWER_W, Environment.NET_POWER_BETA, Environment.NET_SE);
				double seconds = task.d/Environment.NET_SE;
				totalEnergy += power * seconds;
			}
		}
		return totalEnergy;
	}

	public double getEnergyforComputing() {
		double totalEnergy = 0;
		double duration = 0;
		while(true) {
			double minComputingTime = Double.MAX_VALUE;
			VirtualMachine minVm = null;
			for(VirtualMachine vm : vms) {
				Task task = vm.get();
				if(task == null) continue;
				double computingTime = task.tid==0?task.dl:task.r1Remain/vm.sv;
				if(computingTime < minComputingTime) {
					minComputingTime = computingTime;
					minVm = vm;
				}
			}
			
			if(minVm != null) {
				totalEnergy += minComputingTime*getPe();
				duration += minComputingTime;
				averageUtility += minComputingTime*getUtility();
				minVm.remove();
				for(VirtualMachine vm : vms) {
					if(vm==minVm) continue;
					if(vm.hasNextTask()) {
						Task task = vm.get();
						if(task.tid>0) {
							double r1Remain = task.r1Remain-minComputingTime*vm.sv;
							if(r1Remain <= 1e-5) {
								vm.remove();
							}else {
								task.r1Remain = r1Remain;
							}				
						}else if(task.tid==0) {
							if(task.dl-minComputingTime<=1e-5) {
								vm.remove();
							}else {
								task.dl=task.dl-minComputingTime;
							}
						}
					}
				}
			}
			if(!hasRemainTasks()) {
				break;
			}
		}
		if(duration>0)
			averageUtility /= duration;
		
		return totalEnergy;
	}
	
	public boolean hasRemainTasks() {
		for(VirtualMachine vm : vms) {
			if(vm.hasNextTask()) {
				return true;
			}
		}
		return false;
	}
	
	public int violateTimeConstraintNum() {
		int violateNum = 0;
		for(VirtualMachine vm : vms) {
			double waitingTime = 0;						
			while(vm.hasNextTask()) {				
				double executionTime = 0;
				Task task = vm.remove();
				if(task.tid==0) continue;
				
				executionTime = Math.max(waitingTime, task.arrivaleServerTime);
				
				double computingTime = task.r1/vm.sv;
				executionTime += computingTime;
				waitingTime = executionTime;
				
				if(executionTime > task.dl) {
					violateNum ++;
				}
			}
			if(waitingTime > finishTimeLastTask) {
				finishTimeLastTask = waitingTime;
			}
		}
		return violateNum;
	}
	
	public void add(MobileDevice md) {
		mds.add(md);
	}
	
	public void addMobileDeives(List<MobileDevice> mdlist) {
		mds.addAll(mdlist);
	}
	
	public void add(Task task) {
		tasks.add(task);
	}
	
	public void addTasks(List<Task> tasklist) {
		tasks.addAll(tasklist);
	}

	public void add(VirtualMachine vm) {
		vms.add(vm);
	}
	
	public void addVMs(List<VirtualMachine> vmlist) {
		vms.addAll(vmlist);
	}
}

