package perc;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Atom;

import jpgm.IntArray;

import java.util.Arrays;
import java.util.Vector;
import java.util.HashMap;
import java.util.logging.Logger;

public class BNAdapter {

	private int agentID;
	private int px, py; // Current position;
	private IntArray state;
	private IntArray estimate;
	private IntArray sensorReading;
	private double[][] belief;
	private HashMap<String,Integer> scanCode;
	private HashMap<Integer,String> codeScan;
	private HashMap<String,Integer> actionCode;
	private static IntArray JSORT = new IntArray(0,3,6,1,4,7,2,5,8);
	public int estErrors = 0;
	public int perErrors = 0;

	// Model parameters
	double[] theta;	// cell perception noise

	protected Logger logger = Logger.getLogger(BNAdapter.class.getName());

	public BNAdapter(int agentID, double[] theta) {
		this.agentID = agentID;
		this.theta = theta;

		state = new IntArray(-1,-1,-1,-1,-1,-1,-1,-1,-1);		
		sensorReading = new IntArray(-1,-1,-1,-1,-1,-1,-1,-1,-1);
		belief = BNCalculator.uniformBelief();

		scanCode = new HashMap<String,Integer>();
		scanCode.put("empty",		BNCalculator.EMPTY);
		scanCode.put("gold",		BNCalculator.GOLD);
		scanCode.put("obstacle",	BNCalculator.OBSTACLE);
		scanCode.put("ally",		BNCalculator.MINER);
		scanCode.put("enemy",		BNCalculator.MINER);
		codeScan = new HashMap<Integer,String>();
		codeScan.put(BNCalculator.EMPTY, 	"empty");
		codeScan.put(BNCalculator.GOLD,		"gold");
		codeScan.put(BNCalculator.OBSTACLE,	"obstacle");
		codeScan.put(BNCalculator.MINER, 	"ally");
		actionCode = new HashMap<String,Integer>();
		actionCode.put("skip",	BNCalculator.SKIP);
		actionCode.put("pick",	BNCalculator.PICK);
		actionCode.put("drop",	BNCalculator.DROP);
		actionCode.put("up",	BNCalculator.UP);
		actionCode.put("right",	BNCalculator.RIGHT);
		actionCode.put("down",	BNCalculator.DOWN);
		actionCode.put("left",	BNCalculator.LEFT);		
	}

	public Vector<Literal> correctPerception(
		Vector<Literal> trueState,
		Vector<Literal> per, 
		Structure act, 
		Literal pos) {
		//
		//	ACTION
		//
		int action = BNCalculator.SKIP;
		String a = "";
		try {
			a = ((Atom)act.getTerm(0)).toString();
			action = actionCode.get(a);
		} catch (Exception e) {}
		//
		//	POSITION COORDINATES
		//
		try {
			px = (int)((NumberTerm)pos.getTerm(0)).solve();
			py = (int)((NumberTerm)pos.getTerm(1)).solve();
		} catch (Exception e) {}
		//
		//	PERCEPTION
		//
		IntArray perception = importExternal(per);
		//
		//	COMOPUTE ESTIMATE
		//
		BNCalculator.agentID = agentID;
		int correctionMethod = 1;
		switch (correctionMethod) {
			case 0:
				//
				//	estimate is based in the current state
				//
				estimate = BNCalculator.update(state, perception, action, theta);
				break;

			case 1:
				//
				//	estimate is based in the current belief (a distribution)
				//			
				belief = BNCalculator.updateBelief(belief, perception, action, theta);
				estimate = BNCalculator.argmax(belief);
				break;

			case 2:
				//
				//	estimate is the perception
				//		
				estimate = importExternal(per);
				break;

			case 3:
				//
				//	CHEATING: estimate is the true state (just for evaluating purposes)
				//		
				estimate = importExternal(trueState);
				break;
		}
		//
		//	EVALUATION / LOGGING
		//
		IntArray s = importExternal(trueState);
		estErrors += s.match(estimate).get(1);
		perErrors += s.match(perception).get(1);
		//
		boolean tracing = false;
		if (agentID == 0 && tracing) {
			logger.info(
				//"\nstate   : " + BNCalculator.GridToString(state) +
				"\naction  : " + a + 
				"\nhidden  : " + BNCalculator.GridToString(s) +
				//"\npercept : " + BNCalculator.GridToString(perception) + 
				"\nbelief:\n" + BNCalculator.BelToString(belief) +
				"\nestimate: " + BNCalculator.GridToString(estimate) +
				"\nest.errors x per.errors: " + estErrors +  " x " + perErrors + 
				"");
		}
		//
		//	UPDATE STATE TO ESTIMATE
		//
		state = estimate;
		Vector<Literal> q = exportInternal(state);
		return q;
	}

	public IntArray importExternal(Vector<Literal> per) {
		int[] cellS = new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1};
		try {
			for (Literal c : per) {
				//
				//	"Open" the literal "c = cell(x,y,s)"
				//
				int cx = (int)((NumberTerm)c.getTerm(0)).solve();
				int cy = (int)((NumberTerm)c.getTerm(1)).solve();
				String cc = ((Atom)c.getTerm(2)).toString();
				//
				//	Convert cell coordinates to scanner code
				//
				int sensorId = 4;
				if (cx < px) {
					sensorId += -1;
				} else if (cx > px) {
					sensorId += 1;
				}
				if (cy < py) {
					sensorId -= 3;
				} else if (cy > py) {
					sensorId += 3;
				}
				cellS[sensorId] = scanCode.get(cc);
			}
		} catch (Exception e) {
			logger.warning("Failed conversion to IntArray: " + e);
		}

		return new IntArray(cellS); 
	}

	private Vector<Literal> exportInternal(IntArray a) {
		Vector<Literal> per = new Vector<Literal>();
		int[] cells = new int[]{0,3,6,1,4,7,2,5,8};
		for (int i : cells ) {
			int ci = a.get(i);	
			if (ci >= 0) {			
				//
				// Convert scanner code to cell coordinates
				//
				int cx = px + (i % 3) - 1;
				int cy = py + (i / 3) - 1;
				Literal c = Literal.parseLiteral(
					"cell("+cx +","+cy+","+codeScan.get(ci)+")");
				per.add(c);
			}
		}
		return per;
	}
}