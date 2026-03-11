package cn.tfs.eaco.core;

import java.util.List;
import java.util.Vector;
/**
 * Mobile Device i (EDi): represented by a tuple (pi, si, lei, eei, cei), 
 * where pi and si denote the power consumption and computational speed of
 * i respectively, while vectors lei, eei, and cei denote the sets of tasks
 * released on i that are scheduled for local execution, edge execution, 
 * and cloud execution respectively.
 * 
 * @author Duan Lintao
 * duanlintao@cdu.edu.cn
 *
 */
public class MobileDevice {
	public int mdid;
	public double sd;//BI/second
	public double pd;
	@Deprecated
	public double pidle;
	@Deprecated	
	public Task task;
	public List<Task> pendingTasks;
	public EdgeServer edgeServer;
	public double finishTimeLastTask;
	
	public MobileDevice() {
		
	}

	public MobileDevice(int mdid, double sd, double pd, double pidle) {
		super();
		this.mdid = mdid;
		this.sd = sd;
		this.pd = pd;
		this.pidle = pidle;
		pendingTasks = new Vector<>();
	}		

	public double getEnergyforComputing() {
		double totalEnergy = 0;
		for(Task task : pendingTasks) {
			if(task.policy==0) {
				totalEnergy += (task.r0+task.r1)/this.sd*this.pd;
			}else {
				totalEnergy += task.r0/this.sd*this.pd;
			}
		}
		return totalEnergy;
	}

	public int violateTimeConstraintNum() {
		int violateNum = 0;
		double waitingTime = 0;
		for(Task task : pendingTasks) {
			if(task.policy==0) {
				double executionTime = (task.r0+task.r1)/this.sd;
				waitingTime += executionTime;
				if(waitingTime > task.dl) {
					violateNum ++;
				}
			}else {
				waitingTime += task.r0/this.sd;
			}
		}
		this.finishTimeLastTask = waitingTime;
		return violateNum;
	}	
	
	public double getPd() {
		return pd;
	}

	public void setPd(double pd) {
		this.pd = pd;
	}

	public double getPidle() {
		return pidle;
	}

	public void setPidle(double pidle) {
		this.pidle = pidle;
	}

	public void setTask(Task task) {
		this.task = task;
	}
	
	public void setEdgeServer(EdgeServer es) {
		this.edgeServer = es;
	}
	
}
