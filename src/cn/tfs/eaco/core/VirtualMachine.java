package cn.tfs.eaco.core;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 * Virtual Machine
 * 
 * @author Duan Lintao
 * duanlintao@cdu.edu.cn
 *
 */
public class VirtualMachine {
	public int vmid;
	public double sv;//MIPS
	public Queue<Task> pendingTasks;
	
	public VirtualMachine(int id, double sv) {
		this.vmid = id;
		this.sv = sv;
		pendingTasks = new ConcurrentLinkedQueue<>();		
	}

	public double getComputingTime() {
		double computingTime = 0;
		for(Task task : pendingTasks) {
			if(task.r0+task.r1>0) {
				computingTime += task.r1Remain/sv;
			}else {
				computingTime += task.dl;
			}
		}
		return computingTime;
	}

	public boolean add(Task task) {
		return pendingTasks.add(task);
	}

	public Task get() {
		return pendingTasks.peek();
	}

	public Task remove() {
		return pendingTasks.poll();
	}
	
	public boolean hasNextTask() {
		return pendingTasks.size()>0?true:false;
	}
}
