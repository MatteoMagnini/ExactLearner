package org.zhaowl.settings;

public class Settings {
	public boolean oracleSat;
	public boolean oracleUnsat;
	public boolean oracleBranch;
	public boolean oracleMerge;
	
	public boolean learnerSat;
	public boolean learnerUnsat;
	public boolean learnerMerge;
	public boolean learnerBranch;
	public boolean learnerDecompose;
	
	public Settings(boolean[] values)
	{
		this.oracleSat = values[0];
		this.oracleUnsat = values[1];
		this.oracleBranch = values[2];
		this.oracleMerge = values[3];
		
		this.learnerSat = values[4];
		this.learnerUnsat = values[5];
		this.learnerMerge = values[6];
		this.learnerBranch = values[7];
		this.learnerDecompose = values[8];
	}
	
	 
	
}
