/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import jrds.HostsList;
import jrds.Period;
import jrds.RdsGraph;

/**
 * A servlet wich generate a png for a graph
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public final class Graph extends HttpServlet {
	static final private Logger logger = Logger.getLogger(Graph.class);
	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		Date start = new Date();
		HostsList hl = HostsList.getRootGroup();

		String scale = req.getParameter("scale");
		Period p = null;
		int scaleVal = -1;
		if(scale != null && (scaleVal = Integer.parseInt(scale)) > 0)
			p = new Period(scaleVal);
		else
			p = new Period(req.getParameter("begin"), req.getParameter("end"));
		Date begin = p.getBegin();
		Date end = p.getEnd();
		
		if("true".equals(req.getParameter("refresh"))) {
			long delta = end.getTime() - begin.getTime();
			end = new Date();
			begin = new Date(end.getTime() - delta);
		}
		
		String rrdId = req.getParameter("id");
		RdsGraph graph = hl.getGraphById(Integer.parseInt(rrdId));
		
		res.setContentType("image/png");
		ServletOutputStream out = res.getOutputStream();
		res.addHeader("Cache-Control", "no-cache");
		Date middle = new Date();
		graph.writePng(out, begin, end);
		Date finish = new Date();
		long duration1 = middle.getTime() - start.getTime();
		long duration2 = finish.getTime() - middle.getTime();
		logger.trace("Graph " + graph + " rendering, started at " + start + ", ran for " + duration1 + ":" + duration2 + "ms");							
	}
}
