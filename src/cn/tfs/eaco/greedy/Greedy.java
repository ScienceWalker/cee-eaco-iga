package cn.tfs.eaco.greedy;

import java.util.Collections;
import java.util.Comparator;

import cn.tfs.eaco.core.CloudServer;
import cn.tfs.eaco.core.EdgeServer;
import cn.tfs.eaco.core.Environment;
import cn.tfs.eaco.core.MobileDevice;
import cn.tfs.eaco.core.PowerModel;
import cn.tfs.eaco.core.Task;
import cn.tfs.eaco.core.VirtualMachine;

/**
 * A greedy strategy aimed at minimizing the total execution time (makespan) in a 
 * heterogeneous device-edge-cloud environment. This strategy assigns tasks to 
 * resources (ED, ES, or CS) that currently have the smallest cumulative execution time.
 * 
 * Reference:
 * K. Li, Design and analysis of heuristic algorithms for energy constrained task scheduling
 * with device-edge-cloud fusion, IEEE Transactions on Sustainable Computing
 * 8 (2023) 208¨C221.
 * 
 * @author Duan Lintao
 */
public class Greedy extends Environment{
	
	public void greedyScheduleLength() {
		
		long start = System.nanoTime();
		double[] lastTimeDevice = new double[mdlist.size()];
		double[][] lastTimeEdge = new double[eslist.size()][ES_VM_SIZE];
		double[][] lastTimeCloud = new double[cslist.size()][CS_SERVICE_SIZE];
		
		double totalEnergy = 0;
		int violateTaskNums = 0;
		
		Collections.sort(tasklist, new Comparator<Task>() {
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

		for(Task task : tasklist) {
			MobileDevice md = task.md;
			EdgeServer es = md.edgeServer;
			
			double localtime = lastTimeDevice[md.mdid-1]+(task.r0+task.r1)/md.sd;
			double eslocaltime = lastTimeDevice[md.mdid-1]+(task.r0/md.sd);
			double esvmleasttime = Double.MAX_VALUE;
			int esvmleastid = -1;
			for(int i = 0; i < ES_VM_SIZE; i++) {
				
				if(esvmleasttime > lastTimeEdge[es.esid-mdlist.size()-1][i]) {
					esvmleasttime = lastTimeEdge[es.esid-mdlist.size()-1][i];
					esvmleastid = i+1;
				}
			}
			double estime = Math.max(eslocaltime+task.d/NET_SE,lastTimeEdge[es.esid-mdlist.size()-1][esvmleastid-1])+task.r1/es.vms.get(esvmleastid-1).sv;
			
			double cslocaltime = lastTimeDevice[md.mdid-1]+(task.r0/md.sd);
			double csvmleasttime = Double.MAX_VALUE;
			int csleastid = -1;
			int csvmleastid = -1;			
			for(CloudServer cs : cslist) {
				for(VirtualMachine vm : cs.vms) {
					if(csvmleasttime > lastTimeCloud[cs.csid-mdlist.size()-eslist.size()-1][vm.vmid-1]) {
						csvmleasttime = lastTimeCloud[cs.csid-mdlist.size()-eslist.size()-1][vm.vmid-1];
						csleastid = cs.csid;
						csvmleastid = vm.vmid;
					}
				}
			}
			double cstime = Math.max(cslocaltime+task.d/NET_SE+task.d/NET_SC,lastTimeCloud[csleastid-mdlist.size()-eslist.size()-1][csvmleastid-1])+task.r1/cslist.get(csleastid-mdlist.size()-eslist.size()-1).vms.get(csvmleastid-1).sv;
			
			if(localtime < Math.min(estime, cstime)) {
				lastTimeDevice[md.mdid-1] = localtime;
				if(localtime>task.dl) {
					violateTaskNums++;
				}
				totalEnergy += (task.r0+task.r1)/md.sd*md.pd;
				
			}else if(estime < Math.min(localtime, cstime)) {
				double slacktime = eslocaltime+task.d/NET_SE-lastTimeEdge[es.esid-mdlist.size()-1][esvmleastid-1];
				if(slacktime > 0) {
					Task nulltask = new Task(0, 0, 0, 0, slacktime);
					eslist.get(es.esid-mdlist.size()-1).vms.get(esvmleastid-1).add(nulltask);
				}
				eslist.get(es.esid-mdlist.size()-1).vms.get(esvmleastid-1).add(task);
				lastTimeDevice[md.mdid-1] = eslocaltime;
				lastTimeEdge[es.esid-mdlist.size()-1][esvmleastid-1] = estime;
				if(estime>task.dl) {
					violateTaskNums++;
				}
				
				totalEnergy += task.r0/md.sd*md.pd;
				double power = PowerModel.powerTransimitEndEdge(Environment.NET_POWER_W, Environment.NET_POWER_BETA, Environment.NET_SE);
				double seconds = task.d/Environment.NET_SE;
				totalEnergy += power * seconds;						
				
			}else if(cstime < Math.min(localtime, estime)) {
				double slacktime = cslocaltime+task.d/NET_SE+task.d/NET_SC-lastTimeCloud[csleastid-mdlist.size()-eslist.size()-1][csvmleastid-1];
				if(slacktime > 0) {
					Task nulltask = new Task(0, 0, 0, 0, slacktime);
					cslist.get(csleastid-mdlist.size()-eslist.size()-1).vms.get(csvmleastid-1).add(nulltask);
				}
				cslist.get(csleastid-mdlist.size()-eslist.size()-1).vms.get(csvmleastid-1).add(task);
				lastTimeDevice[md.mdid-1] = cslocaltime;
				lastTimeCloud[csleastid-mdlist.size()-eslist.size()-1][csvmleastid-1] = cstime;
				if(cstime>task.dl) {
					violateTaskNums++;
				}
				
				totalEnergy += task.r0/md.sd*md.pd;
				double wirelesspower = PowerModel.powerTransimitEndEdge(Environment.NET_POWER_W, Environment.NET_POWER_BETA, Environment.NET_SE);
				double wirelessseconds = task.d/Environment.NET_SE;
				totalEnergy += wirelesspower * wirelessseconds;				
				double wiredpower = Environment.NET_WIRED_PBASE+Environment.NET_WIRED_PRATE*Environment.NET_WIRED_R;
				double wiredseconds = task.d/Environment.NET_SC;
				totalEnergy += wiredpower * wiredseconds;
				
				
			}
		}
		double esutility = 0;
		int esonnum = 0;
		double csutility = 0;
		int csonnum = 0;
		
		for(EdgeServer es : eslist) {
			totalEnergy += es.getEnergyforComputing();
			if(es.averageUtility>0) {
				esutility += es.averageUtility;
				esonnum++;
			}
		}
		if(esonnum > 0)
			esutility/=esonnum;
		
		for(CloudServer cs : cslist) {
			totalEnergy += cs.getEnergyforComputing();
			if(cs.averageUtility>0) {
				csutility += cs.averageUtility;
				csonnum++;
			}
		}
		if(csonnum>0)
			csutility/=csonnum;
		double schedulelength = Double.MIN_VALUE;
		String offloadPolicy = "";
		int mdid = -1;
		int esid = -1;
		int esvid = -1;
		int csid = -1;
		int csvid = -1;
		for(int i = 0; i < lastTimeDevice.length; i++) {
			if(lastTimeDevice[i] > schedulelength) {
				schedulelength = lastTimeDevice[i];
				offloadPolicy = "locally";
				mdid = i+1;
			}
		}
		for(int i = 0; i < lastTimeEdge.length; i++) {
			for(int j = 0; j < lastTimeEdge[i].length; j++) {
				if(lastTimeEdge[i][j]>schedulelength) {
					offloadPolicy = "edge";
					esid = i+1;
					esvid = j+1;
				}
			}
		}
		for(int i = 0; i < lastTimeCloud.length; i++) {
			for(int j = 0; j < lastTimeCloud[i].length; j++) {
				if(lastTimeCloud[i][j]>schedulelength) {
					offloadPolicy = "cloud";
					csid = i+1;
					csvid = j+1;
				}
			}
		}
		long duration = System.nanoTime()-start;
        
		System.out.println("population size: "+ 1);
		System.out.println("iteration number: "+ 1);
		System.out.println("mobile: "+ mdlist.size());
		System.out.println("edge: "+ eslist.size());
		System.out.println("cloud: "+ cslist.size());
		System.out.println("task/md: "+TASK_NUM_PER_MD);
		System.out.println("task: "+ tasklist.size());
		double totalR0 = 0;
		double totalR1 = 0;
		double totalData = 0;
		for(Task task : tasklist) {
			totalR0 += task.r0;
			totalR1 += task.r1;
			totalData += task.d;
		}
		System.out.println("R0+R1:"+ (totalR0+totalR1));
		System.out.println("R0:"+totalR0);
		System.out.println("R1:"+totalR1);
		System.out.println("Roff:"+ TASK_OFFLOADABLE_RATE);
		System.out.println("data size:"+totalData);
		System.out.println(offloadPolicy);
		System.out.println(totalEnergy+" : "+ violateTaskNums+" : "+schedulelength);
		int MAX_INTERATION = 1;
        System.out.println("Iteration:"+MAX_INTERATION+", duration:"+duration+"(nano second)"+", "+duration/1000000.0+"(ms)");
        System.out.println("average one cycle:"+duration*1.0/MAX_INTERATION/1000000.0+"(ms)");
        
		System.out.println("GREEDY:"+1+":"+1+":"+mdlist.size()
							+":"+eslist.size()+":"+cslist.size()+":"+TASK_NUM_PER_MD
							+":"+tasklist.size()+":"+(totalR0+totalR1)+":"+totalR0+":"+totalR1
							+":"+TASK_OFFLOADABLE_RATE+":"+totalData+":"+"null"
							+":"+totalEnergy+":"+violateTaskNums
							+":"+schedulelength+":"+(duration/1000000.0)+":"+(duration*1.0/1/1000000.0)
							+":"+esutility+":"+csutility);
	}
	
	public static void main(String[] args) {
		Greedy gl2023 = new Greedy();
		gl2023.init();
		gl2023.greedyScheduleLength();
	}
}
