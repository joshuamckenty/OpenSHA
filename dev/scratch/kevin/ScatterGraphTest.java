package scratch.kevin;

import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;

import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.XY_DataSet;
import org.opensha.commons.data.function.XY_DataSetAPI;
import org.opensha.commons.data.function.XY_DataSetList;
import org.opensha.sha.gui.infoTools.GraphPanel;
import org.opensha.sha.gui.infoTools.GraphPanelAPI;
import org.opensha.sha.gui.infoTools.PlotControllerAPI;

public class ScatterGraphTest implements GraphPanelAPI, PlotControllerAPI {
	
	private GraphPanel gp;
	private Random r = new Random();
	
	public ScatterGraphTest() {
		gp = new GraphPanel(this);
		
		gp.drawGraphPanel("X", "Y", getFuncList(), false, false, true, "Title", this);
		this.gp.setVisible(true);
		
		this.gp.togglePlot(null);
		
		this.gp.validate();
		this.gp.repaint();
		
		JFrame frame = new JFrame();
		frame.setSize(600, 600);
		frame.setContentPane(gp);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		frame.validate();
	}
	
	private ArrayList<XY_DataSetAPI> getFuncList() {
		ArrayList<XY_DataSetAPI> funcs = new ArrayList<XY_DataSetAPI>();
		
		ArbitrarilyDiscretizedFunc func = new ArbitrarilyDiscretizedFunc();
		XY_DataSet scatter = new XY_DataSet();
		for (double x=0; x<getMaxX(); x++) {
			double y = x * 0.8;
			y += r.nextDouble() - 0.5d;
			System.out.println("Adding " + x + ", " + y);
			func.set(x, y);
			
			scatter.set(x, y + r.nextDouble()*0.5);
			scatter.set(x, y - r.nextDouble()*0.5);
		}
		
		funcs.add(func);
		funcs.add(scatter);
		
		return funcs;
	}
	
	public static void main(String[] args) {
		new ScatterGraphTest();
	}

	@Override
	public double getMaxX() {
		return 10;
	}

	@Override
	public double getMaxY() {
		return 10;
	}

	@Override
	public double getMinX() {
		return 0;
	}

	@Override
	public double getMinY() {
		return 0;
	}

	@Override
	public int getAxisLabelFontSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPlotLabelFontSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTickLabelFontSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setXLog(boolean flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setYLog(boolean flag) {
		// TODO Auto-generated method stub
		
	}

}
