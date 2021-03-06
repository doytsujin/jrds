/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rrd4j.core.DsDef;
import org.slf4j.event.Level;

import jrds.Probe;
import jrds.PropertiesManager;
import jrds.Util;

/**
 * This abstract class can be used to parse the results of an external command
 * 
 * @author Fabrice Bacchella
 */
public abstract class ExternalCmdProbe extends Probe<String, Number> {

    protected String cmd = null;

    @Override
    public void readProperties(PropertiesManager pm) {
        cmd = resolvCmdPath(pm.getProperty("path", ""));
    }

    public Boolean configure() {
        if(cmd == null)
            return false;
        String cmdargs = getPd().getSpecific("arguments");
        if(cmdargs != null && !cmdargs.trim().isEmpty()) {
            cmd = cmd + " " + Util.parseTemplate(cmdargs, this);
        }
        return true;
    }

    protected String resolvCmdPath(String path) {
        List<String> pathelements = new ArrayList<String>();
        pathelements.addAll(Arrays.asList(path.split(";")));
        String envPath = System.getenv("PATH");
        if(envPath != null && !envPath.isEmpty()) {
            pathelements.addAll(Arrays.asList(envPath.split(System.getProperty("path.separator"))));
        }
        String cmdname = getPd().getSpecific("command");
        log(Level.DEBUG, "will look for %s in %s", cmdname, pathelements);
        for(String pathdir: pathelements) {
            File tryfile = new File(pathdir, cmdname);
            log(Level.TRACE, "trying if %s can execute", tryfile);
            if(tryfile.canExecute()) {
                log(Level.DEBUG, "will use %s as a command", tryfile.getAbsolutePath());
                cmd = tryfile.getAbsolutePath();
                break;
            }
        }
        if(cmd == null) {
            log(Level.ERROR, "command %s not found", cmdname);
        }
        return cmd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.jrds.Probe#getNewSampleValues()
     */
    public Map<String, Number> getNewSampleValues() {
        String perfstring = launchCmd();
        String values[] = perfstring.split(":");
        DsDef[] defs = getPd().getDsDefs(getRequiredUptime());
        int n = values.length;
        if(values.length != defs.length + 1) {
            throw new IllegalArgumentException("Invalid number of values specified (found " + values.length + ", " + defs.length + " allowed)");
        }
        long time;
        String timeToken = values[0];
        if(timeToken.equalsIgnoreCase("N") || timeToken.equalsIgnoreCase("NOW")) {
            time = System.currentTimeMillis() / 1000;
        } else {
            time = jrds.Util.parseStringNumber(timeToken, Long.MAX_VALUE);
            if(time == Long.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid sample timestamp: " + timeToken);
            }
        }
        Map<String, Number> retValues = new HashMap<String, Number>(n - 1);
        for(int i = 0; i < defs.length; i++) {
            double value = jrds.Util.parseStringNumber(values[i + 1], Double.NaN);
            retValues.put(defs[i].getDsName(), value);
        }
        return retValues;
    }

    protected String launchCmd() {
        String perfstring = "";
        Process urlperfps = null;
        try {
            log(Level.DEBUG, "executing: %s", cmd);
            urlperfps = Runtime.getRuntime().exec(getCmd());
            try (InputStream  stdout = urlperfps.getInputStream()) {
                BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
                perfstring = stdoutReader.readLine();
            }
        } catch (IOException e) {
            log(Level.ERROR, e, "external command failed : %s", e);
        }

        try {
            if(urlperfps != null) {
                urlperfps.waitFor();
                if(urlperfps.exitValue() != 0) {

                    InputStream stderr = urlperfps.getErrorStream();
                    BufferedReader stderrtReader = new BufferedReader(new InputStreamReader(stderr));
                    String errostring = stderrtReader.readLine();
                    if(errostring == null) {
                        errostring = "";
                    }

                    log(Level.ERROR, " command %s failed with %s", cmd, errostring);
                    perfstring = "";
                }
                urlperfps.getInputStream().close();
                urlperfps.getErrorStream().close();
                urlperfps.getOutputStream().close();
            }
        } catch (IOException e) {
            log(Level.ERROR, e, "Exception on close: %s", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log(Level.DEBUG, "returned line: %s", perfstring);
        return perfstring;
    }

    /**
     * @return Returns the cmd.
     */
    public String getCmd() {
        return cmd;
    }

    @Override
    public String getSourceType() {
        return "external command";
    }
}
