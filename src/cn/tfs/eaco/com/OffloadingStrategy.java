package cn.tfs.eaco.com;
/**
 * 
 * @author Duan Lintao
 *
 */
public class OffloadingStrategy {
	public int[] strategy; 
	public double energy;
	public int violateTimeConstraintNum;
	public double[] fitnessValue;
	public double fitnessUtility;
	public double scheduleLength;
	public double esutility;
	public double csutility;
	 
	public OffloadingStrategy(int size) {
		this.strategy = new int[size];
		this.fitnessValue = new double[2];//number of objective
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {		
		OffloadingStrategy s1 = new OffloadingStrategy(this.strategy.length);
		for(int i = 0; i < strategy.length; i++) {
			s1.strategy[i]=strategy[i];
		}
		s1.energy = energy;
		s1.violateTimeConstraintNum = violateTimeConstraintNum;
		s1.fitnessValue[0] = fitnessValue[0];
		s1.fitnessValue[1] = fitnessValue[1];
		s1.scheduleLength = scheduleLength;
		s1.esutility = esutility;
		s1.csutility = csutility;
		return s1;
	}
}
