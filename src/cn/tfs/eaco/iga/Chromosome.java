package cn.tfs.eaco.iga;

import java.util.Vector;
/**
 * Chromosome
 * 
 * @author Duan Lintao
 * duanlintao@cdu.edu.cn
 *
 */
public class Chromosome implements Comparable<Chromosome>{
	public Vector<Integer> sequence;
	public double energy;
	public int violateTimeConstraintNum;
	public double fitnessValue;
	public double scheduleLength;
	public double esutility;
	public double csutility;
	
	public Chromosome() {
		sequence = new Vector<>();
	}
	
	@Override
	public int compareTo(Chromosome o) {	
		double differ = this.fitnessValue-o.fitnessValue;
		if (differ>0)
			return 1;
		else if(differ < 0)
			return -1;
		else 
			return 0;
	}
	
	public void add(Integer feature) {
		sequence.add(feature);
	}
	
	public void copy(Chromosome chromosome) {
		this.energy = chromosome.energy;
		this.fitnessValue = chromosome.fitnessValue;
		this.violateTimeConstraintNum = chromosome.violateTimeConstraintNum;
		this.scheduleLength = chromosome.scheduleLength;
		this.esutility = chromosome.esutility;
		this.csutility = chromosome.csutility;
		this.sequence.clear();
		for(int i = 0; i < chromosome.sequence.size(); i++) {
			this.sequence.add(chromosome.sequence.get(i));
		}
	}
}
