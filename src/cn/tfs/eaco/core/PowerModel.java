package cn.tfs.eaco.core;
/**
 * Power Model
 * 
 * @author Duan Lintao
 * duanlintao@cdu.edu.cn
 *
 */
public class PowerModel {

	public static double powerMobileDevice(double alpha, double beta, double speed) {
		return alpha*Math.pow(speed, beta);
	}

	public static double powerServer(double pidle, double pmax, double utility) {
		return pidle+(pmax-pidle)*utility;
	}

	public static double powerTransimitEndEdge(double w, double beta, double speed_transmit) {
		return (Math.pow(2, speed_transmit/w)-1)/beta;
	}
}
