/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.util.Map;
import java.util.Set;

import org.rrd4j.core.Sample;
import org.snmp4j.smi.OID;


/**
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ProcessStatusHostResources extends RdsSnmpSimple {
	static final private String RUNNING="running";
	static final private int RUNNINGINDEX = 1;
	static final private String RUNNABLE="runnable";
	static final private int RUNNABLEINDEX = 2;
	static final private String NOTRUNNABLE="notRunnable";
	static final private int NOTRUNNABLEINDEX = 3;
	static final private String INVALID="invalid";
	static final private int INVALIDINDEX = 4;
	
	/* (non-Javadoc)
	 * @see jrds.Probe#modifySample(org.rrd4j.core.Sample, java.util.Map)
	 */
	@Override
	public void modifySample(Sample oneSample, Map<OID, Object> snmpVars) {
		int running = 0;
		int runnable = 0;
		int notRunnable = 0;
		int invalid = 0;
		for(OID oid: (Set<OID>)snmpVars.keySet()){
			int state = ((Number)snmpVars.get(oid)).intValue();
			if(RUNNINGINDEX == state)
				running++;
			else if(RUNNABLEINDEX == state)
				runnable++;
			else if(NOTRUNNABLEINDEX == state)
				notRunnable++;
			else if(INVALIDINDEX == state)
				invalid++;
			
		}
		oneSample.setValue(RUNNING, new Double(running));
		oneSample.setValue(RUNNABLE, new Double(runnable));
		oneSample.setValue(NOTRUNNABLE, new Double(notRunnable));
		oneSample.setValue(INVALID, new Double(invalid));
	}
	
}
