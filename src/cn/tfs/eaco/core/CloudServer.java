package cn.tfs.eaco.core;

import java.util.List;
import java.util.Vector;
/**
 * Cloud Server k (CSk): denoted by a triple (vnk, vsk, pk), where
 * vnk represents the number of VMs on k, vsk indicates the speed
 * of these VMs, and pk indicates the power consumption of k.
 * 
 * @author Duan Lintao
 * duanlintao@cdu.edu.cn
 */
public class CloudServer {
	public int csid;
	public double pidle;
	public double pmax;
	public List<Task> tasks;
	public List<VirtualMachine> vms;
	public int maxVMsNum;
	public double firstArrivalTime;
	public double finishTimeLastTask;
	public double averageUtility;
	
	public boolean state;
	
	public CloudServer() {
		tasks = new Vector<>();
		vms = new Vector<>();
		firstArrivalTime = -1;
		this.state = false;
	}

	public CloudServer(int csid, int maxVMsNum, double pidle,double pmax) {
		super();
		this.csid = csid;
		this.maxVMsNum = maxVMsNum;
		this.pidle = pidle;
		this.pmax = pmax;
		this.tasks = new Vector<>();
		this.vms = new Vector<>();
		firstArrivalTime = -1;
		this.state = false;
	}
	
	public double getUtility() {
		int activeVMsNum = 0;
		for(int i = 1; i <= vms.size(); i++) {
			VirtualMachine vm = vms.get(i-1);
			Task task = vm.get();
			if(task!=null&&task.tid>0) {
				activeVMsNum ++;
			}
		}
		return activeVMsNum*1.0/maxVMsNum;
	}
	
	public double getPc() {
		double utility = getUtility();
		return PowerModel.powerServer(pidle, pmax, utility);
	}
	
	public double getEnergyforComputing() {
		double totalEnergy = 0;
		double duration = 0;
		while(true) {
			double minComputingTime = Double.MAX_VALUE;
			VirtualMachine minVm = null;
			for(VirtualMachine vm : vms) {
				if(vm.hasNextTask()) {
					Task task = vm.get();
					double computingTime = task.tid==0?task.dl:task.r1Remain/vm.sv;
					if(computingTime < minComputingTime) {
						minComputingTime = computingTime;
						minVm = vm;
					}
				}
			}
			
			if(minVm != null) {
				totalEnergy += minComputingTime*getPc();
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
	
	public VirtualMachine getIdleVM() {
		for(VirtualMachine vm : vms) {
			if(!vm.hasNextTask()) {
				return vm;
			}
		}
		return null;
	}

	public double getEnergyforTransmitting() {
		double totalEnergy = 0;
		for(VirtualMachine vm : vms) {
			for(Task task : vm.pendingTasks) {
				double wirelesspower = PowerModel.powerTransimitEndEdge(Environment.NET_POWER_W, Environment.NET_POWER_BETA, Environment.NET_SE);
				double wirelessseconds = task.d/Environment.NET_SE;
				totalEnergy += wirelesspower * wirelessseconds;
				double wiredpower = Environment.NET_WIRED_PBASE+Environment.NET_WIRED_PRATE*Environment.NET_WIRED_R;
				double wiredseconds = task.d/Environment.NET_SC;
				totalEnergy += wiredpower * wiredseconds;
			}
		}
		return totalEnergy;
	}
	
	public void addTask(Task task) {
		this.tasks.add(task);
	}
	
	public void addTasks(List<Task> tasks) {
		this.tasks.addAll(tasks);
	}

	public void add(VirtualMachine vm) {
		vms.add(vm);
	}
	
	public void addVMs(List<VirtualMachine> vmlist) {
		vms.addAll(vmlist);
	}
	
}
