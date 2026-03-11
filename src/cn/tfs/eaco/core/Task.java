package cn.tfs.eaco.core;
/**
 * The computational workload of tasks released by each ED consists of two parts:
 * the non-offloadable portion that must be executed locally and the offloadable 
 * portion that can be executed on a server based on the chosen execution mode.
 * Task m represented by a 8-tuple (am; bm; dm; fm; r0m; r1m; em; vm), where am, 
 * bm, dm, and fm represent the arrival time, begin execution time, deadline, and
 * finish time of task m, respectively. r0m denotes the non-offloadable portion of
 * task m, which must be executed locally on the mobile device, while r1m represents
 * the offloadable portion of it, which can be executed in one of three execution modes.
 * em represents the execution mode of m, where em = 0 denotes local execution, em = 1
 * denotes offloading to an ES for execution, and em = 2 denotes offloading to a CS 
 * for execution. vm denotes the size of input data that requires migration during task
 * offloading for execution.
 * 
 * @author Duan Lintao
 * duanlintao@cdu.edu.cn
 *
 */
public class Task {
	public int tid;
	public MobileDevice md;
	public double r0; 
	public double r1; 
	public double r1Remain;
	public double r0BeginTime;
	public double r1BeginTime;
	@Deprecated
	public double releaseTime;
	public double arrivaleServerTime;
	public double d; 
	public double dl; 
	public int policy;
	
	public Task(int tid, double r0, double r1, double d, double dl) {
		this.tid = tid;
		this.r0 = r0;
		this.r1 = r1;
		this.r1Remain = r1;
		this.d = d;
		this.dl = dl;		
	}	
	
	public void updateR1Remain(double remain) {
		this.r1Remain = remain;
	}	
}
 