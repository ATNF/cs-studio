package org.csstudio.archive.archiveRecord;
import org.csstudio.platform.data.IDoubleValue;
import org.csstudio.platform.data.IIntegerValue;
import org.csstudio.utility.pv.PV;
import org.csstudio.utility.pv.epics.EPICS_V3_PV;
/** Handles the "archiveRecord" low-level staff 
 *  @author Albert Kagarmanov
 */
//
// Main class for ArchiveRecord package
//
public class ArchiveRecord {
	private static final boolean debug = false;
	private final static String ARsuffix = "_h";     // Suffix for archiveRecord
	private final static String numStr = ".NVAL";    // Field for # of point
	private final static String valStr = ".VAL";     // Field for value
	private final static String timStr = ".TIM";     // Field for time in sec    
	private final static String nscStr = ".NSC";     // Field for time nanosec within last sec.
	private final static String sevrStr = ".SVY";    // Field for severuty  
	private final static String flushStr = ".FLSH";  // Field for FLUSH archiveRecord
	private final static String pcabStr = ".PCAB";    // Field for PCAB archiveRecord (Absolute or Percent)
	private final static String rvarStr = ".RVAR";    // Field for RVAR (Diff. last-curr. in %)
	private final static String avarStr = ".AVAR";    // Field for AVAR (Diff. last-curr. abs)
	private String PVname;                            // PVname 
	private String archivePVname;                     //  archiveRecord
	private String nvalName;                          // Field for # of point
	private String valName;                           // Field for value
	private String timeName;                          //	 Field for time in sec    
	private String nsecName;                          // Field for time nanosec within last sec.
	private String sevrName;                          // Field for severuty
	private String flushName;                         // Field for FLUSH archiveRecord
	private String pcabName;                          // Field for PCAB archiveRecord (Absolute or Percent)
	private String rvarName;                          // Field for RVAR (Diff. last-curr. in %)
	private String avarName;                          // Field for AVAR (Diff. last-curr. abs)
	private int dim;                                  // length of all our arrays
	private double valArr[];                          // unsorted arary of values   
	private long  timeArr[];                          // unsorted arary of sec.
	private long  nsecArr[];                          // unsorted arary of nsec.
	private long  statArr[];                          // unsorted arary of status (dummy)
	private long  sevrArr[];                          // unsorted arary of severuty
	private int TIMEOUT_CA = 2000;                    // Epics CA timeout in miliSec
	private int THREAD_DELAY = 100;                   // delay in miliSec
	private int NUM_OF_ITER=TIMEOUT_CA / THREAD_DELAY;
	private long EPICS_TIME_SHIFT=631152000L;         // EPICS time starts from 01-01-1990
	
	public ArchiveRecord(String name)
	{
		this.PVname = name;
		int pos = name.indexOf(ARsuffix);
		if (pos >0) this.PVname = name.substring(0, pos);
		
		this.archivePVname = addSuffix(this.PVname,ARsuffix);
		this.nvalName      = addSuffix(this.archivePVname,numStr);
		this.valName       = addSuffix(this.archivePVname,valStr);
		this.timeName      = addSuffix(this.archivePVname,timStr);
		this.nsecName      = addSuffix(this.archivePVname,nscStr);
		this.sevrName      = addSuffix(this.archivePVname,sevrStr);
		this.flushName     = addSuffix(this.archivePVname,flushStr);
		this.pcabName      = addSuffix(this.archivePVname,pcabStr);
		this.rvarName      = addSuffix(this.archivePVname,rvarStr);
		this.avarName      = addSuffix(this.archivePVname,avarStr);		
	}
	
	public int getDimension() {
		dim = -1;
		PV pvdim = new EPICS_V3_PV(nvalName);
		
		try {
			pvdim.start();
			for(int i=0;i<NUM_OF_ITER;i++) {
			    if (pvdim.isConnected()) break;
			        Thread.sleep(THREAD_DELAY);
			}
			double dbl = ((IDoubleValue) pvdim.getValue()).getValue();
			dim= (int )dbl;             // DBR-typr for NVAL is double
			if(debug)System.out.println("dim="+dim);
			pvdim.stop();
		} catch (InterruptedException e) {
			System.out.println ("Bad CAGET-command for archive field " + nvalName +" InterruptedException:");
			e.printStackTrace();
			return -1;
		} catch (Exception e) {
			System.out.println ("Bad CAGET-command for archive field " + nvalName +" Exception:");
			e.printStackTrace();
			return -1;
		}		
 		return dim;
	}
	
	public int getAllFromCA()  {
		int len=dim;
		if(len<=1) {
			System.out.println("bad dimension for archiveRecord="+len);
			return -1;
		}
		// get VALUE field:
		PV pvval = new EPICS_V3_PV(valName);
		
		try {
			pvval.start();
			for(int i=0;i<NUM_OF_ITER;i++) {
			    if (pvval.isConnected()) break;
			        Thread.sleep(THREAD_DELAY);
			}
			valArr = ((IDoubleValue) pvval.getValue()).getValues();
			if(valArr.length != dim ) {
				System.out.println(valName+
						"warning: valArr-dimension ("+valArr.length+") not equal NVAL-dim ("+dim+")");
				dim =Math.min(dim,valArr.length);
				if( (len=dim) <= 1) {
					System.out.println("bad dimension for archiveRecord="+len);
					return -1;
				}
			}
			pvval.stop();
		} catch (InterruptedException e) {
			System.out.println ("Bad CAGET-command for archive field " + valName +" InterruptedException:");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println ("Bad CAGET-command for archive field " + valName +" Exception:");
			e.printStackTrace();
		}	
		
		// get TIME field:
		PV pvtime = new EPICS_V3_PV(timeName);
		try {
			pvtime.start();
			for(int i=0;i<NUM_OF_ITER;i++) {
			    if (pvtime.isConnected()) break;
			        Thread.sleep(THREAD_DELAY);
			}
			double[] dblTime = ((IDoubleValue) pvtime.getValue()).getValues();
			if(dblTime.length != dim ) {
				System.out.println(timeName+
						"warning: valArr-dimension ("+dblTime.length+") not equal NVAL-dim ("+dim+")");
				dim =Math.min(dim,dblTime.length);
				if( (len=dim) <= 1) {
					System.out.println("bad dimension for archiveRecord="+len);
					return -1;
				}
			}
			timeArr = new long[dim];
			for(int i=0;i<dblTime.length;i++) {
	        	timeArr[i]= (int) (dblTime[i] + EPICS_TIME_SHIFT );
	        }
			pvtime.stop();
		} catch (InterruptedException e) {
			System.out.println ("Bad CAGET-command for archive field " + timeName +" InterruptedException:");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println ("Bad CAGET-command for archive field " + timeName +" Exception:");
			e.printStackTrace();
		}		
		
		// get Nsec field:
		
		PV pvntime = new EPICS_V3_PV(nsecName);
		try {
			pvntime.start();
			for(int i=0;i<NUM_OF_ITER;i++) {
			    if (pvntime.isConnected()) break;
			        Thread.sleep(THREAD_DELAY);
			}
			double[] dblTime = ((IDoubleValue) pvntime.getValue()).getValues();
			if(dblTime.length != dim ) {
				System.out.println(nsecName+
						"warning: valArr-dimension ("+dblTime.length+") not equal NVAL-dim ("+dim+")");
				dim =Math.min(dim,dblTime.length);
				if( (len=dim) <= 1) {
					System.out.println("bad dimension for archiveRecord="+len);
					return -1;
				}
			}
			nsecArr = new long[dim];
			for(int i=0;i<dblTime.length;i++) {
	        	nsecArr[i]= (int) dblTime[i];
	        }
			pvntime.stop();
		} catch (InterruptedException e) {
			System.out.println ("Bad CAGET-command for archive field " + nsecName +" InterruptedException:");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println ("Bad CAGET-command for archive field " + nsecName +" Exception:");
			e.printStackTrace();
		}
		
		// get sevr field:
		PV sevrname = new EPICS_V3_PV(sevrName);
		try {
			sevrname.start();
			for(int i=0;i<NUM_OF_ITER;i++) {
			    if (sevrname.isConnected()) break;
			        Thread.sleep(THREAD_DELAY);
			}
			
			int[] dblTime = ((IIntegerValue) sevrname.getValue()).getValues();
			if(dblTime.length != dim ) {
				System.out.println(sevrName+
						"warning: valArr-dimension ("+dblTime.length+") not equal NVAL-dim ("+dim+")");
				dim =Math.min(dim,dblTime.length);
				if( (len=dim) <= 1) {
					System.out.println("bad dimension for archiveRecord="+len);
					return -1;
				}
			}
			sevrArr = new long[dim];
			for(int i=0;i<dblTime.length;i++) {
	        	sevrArr[i]= (long) dblTime[i];
	        }
			sevrname.stop();
		} catch (InterruptedException e) {
			System.out.println ("Bad CAGET-command for archive field " + sevrName +" InterruptedException:");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println ("Bad CAGET-command for archive field " + sevrName +" Exception:");
			e.printStackTrace();
		}
		return dim;
	}	
	private String addSuffix(String PV,String suffix) {
		return new String(PV+suffix);
	}		
	public double[] getVal() {return valArr; }
	public long[] getTime()  {return timeArr; }
	public long[] getNsec()  {return nsecArr; }
	public long[] getSevr()  {return sevrArr; }
	public int getDim() {return dim;}
} // eof class ArchiveRecord
