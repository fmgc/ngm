package arch;

import jason.asSyntax.Atom;
import jason.asSyntax.Term;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.NumberTerm;
import jason.asSemantics.ActionExec;

import jpgm.IntArray;
import perc.BNAdapter;

import java.util.Random;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** architecture for noisy capable miners. */
public class NoisyMinerArch extends MinerArch {
    public static Atom aOBSTACLE = new Atom("obstacle");
    public static Atom aGOLD     = new Atom("gold");
    public static Atom aENEMY    = new Atom("enemy");
    public static Atom aALLY     = new Atom("ally");
    public static Atom aEMPTY    = new Atom("empty");
	static Atom[] CELLPERCEPTS = new Atom[]{aOBSTACLE, aGOLD, aENEMY, aALLY, aEMPTY};

	BNAdapter bn;
	double prob_cell_noise;
	Random rand = new Random();
	Structure lastAction;

	//
	//	Use to init cycle-persistent attributes
	//
	@Override
	public void init() {
		super.init();

		//
		//	Read noise parameter
		//
		double noise = 0.0;
		if (getTS().getSettings().getUserParameter("noise") != null) {
			prob_cell_noise = Double.parseDouble(getTS().getSettings().getUserParameter("noise"));
		} else {
			prob_cell_noise = 0.0;
		}
		bn = new BNAdapter(getMyId(), new double[]{ prob_cell_noise });
	}

	@Override
	public void act(ActionExec action, List<ActionExec> feedback) {
		//
		//	Inform BNAdapter of the current action
		//
		lastAction = action.getActionTerm();
		//
		//	Then proceed as usual
		//
		super.act(action,feedback);
	}

	@Override
	public List<Literal> perceive() {
		List<Literal> per = super.perceive();
		Literal currentPos = Literal.parseLiteral("pos(0,0,0)");
		try {		
			Vector<Literal> cellsPer = new Vector<Literal>();
			Vector<Literal> otherPer = new Vector<Literal>();
			for (Literal lit : per) {
				if ("cell".equals(lit.getFunctor())) {
					cellsPer.add(lit);
				} else {
					//
					//	Save current position
					//
					if ("pos".equals(lit.getFunctor())) {
						currentPos = lit;
					}
					//
					//	Then proceed as usual
					//
					otherPer.add(lit);
				}
			}			
			//
			//	Correct the current perception of cell contents
			//
			Vector<Literal> noisyPer = addNoise(cellsPer);
			Vector<Literal> estimPer = bn.correctPerception(
				cellsPer,
				noisyPer, 
				lastAction, 
				currentPos);
			otherPer.addAll(estimPer);
			return localPerceive(otherPer);
		} catch (Exception e) {
		}
		return per;
	}

	private Vector<Literal> addNoise(List<Literal> per) {
		Vector<Literal> n = new Vector<Literal>();
		for (Literal p : per) {
			Literal q = ASSyntax.createLiteral("cell",
				p.getTerm(0),
				p.getTerm(1),
				p.getTerm(2));

			if (rand.nextDouble() < prob_cell_noise) {
				q.setTerm(2, CELLPERCEPTS[ rand.nextInt( CELLPERCEPTS.length ) ]);
			}
			n.add(q);
		}
		return n;
	}
	/**
	* This code is a clone of method LocalMinerArch.perceive()
	*/
	private List<Literal> localPerceive(List<Literal> per) {
		try {
			if (per != null) {
				Iterator<Literal> ip = per.iterator();
				while (ip.hasNext()) {
					Literal p = ip.next();
					String  ps = p.toString();
					if (ps.startsWith("cell") && ps.endsWith("obstacle)") && model != null) {
						int x = (int)((NumberTerm)p.getTerm(0)).solve();
						int y = (int)((NumberTerm)p.getTerm(1)).solve();
						if (x < model.getWidth() && y < model.getHeight())
							obstaclePerceived(x, y, p);
						ip.remove(); // the agent does not perceive obstacles

					} else if (ps.startsWith("pos") && model != null) {
						// announce my pos to others
						int x = (int)((NumberTerm)p.getTerm(0)).solve();
						int y = (int)((NumberTerm)p.getTerm(1)).solve();
						if (x < model.getWidth() && y < model.getHeight())
							locationPerceived(x, y);

					} else if (ps.startsWith("carrying_gold") && model != null) {
						// creates the model
						int n = (int)((NumberTerm)p.getTerm(0)).solve();
						carriedGoldsPerceived(n);

					//} else if (ps.startsWith("cell") && ps.endsWith("ally)")  && model != null) {
						//int x = (int)((NumberTerm)p.getTerm(0)).solve();
						//int y = (int)((NumberTerm)p.getTerm(1)).solve();
						//allyPerceived(x, y);
						//ip.remove(); // the agent does not perceive Others

					} else if (ps.startsWith("cell") && ps.endsWith("gold)")  && model != null) {
						int x = (int)((NumberTerm)p.getTerm(0)).solve();
						int y = (int)((NumberTerm)p.getTerm(1)).solve();
						if (x < model.getWidth() && y < model.getHeight())
							goldPerceived(x, y);

					} else if (ps.startsWith("cell") && ps.endsWith("enemy)") && model != null) {
						int x = (int)((NumberTerm)p.getTerm(0)).solve();
						int y = (int)((NumberTerm)p.getTerm(1)).solve();
						if (x < model.getWidth() && y < model.getHeight())
							enemyPerceived(x, y);
						//ip.remove(); // the agent does not perceive others

					} else if (model == null && ps.startsWith("gsize")) {
						// creates the model
						int w = (int)((NumberTerm)p.getTerm(1)).solve();
						int h = (int)((NumberTerm)p.getTerm(2)).solve();
						setSimId(p.getTerm(0).toString());
						gsizePerceived(w,h);
						ip.remove();

					} else if (model != null && ps.startsWith("steps")) {
						// creates the model
						int s = (int)((NumberTerm)p.getTerm(1)).solve();
						stepsPerceived(s);
						ip.remove();

					} else if (ps.startsWith("depot")) {
						int x = (int)((NumberTerm)p.getTerm(1)).solve();
						int y = (int)((NumberTerm)p.getTerm(2)).solve();
						if (x < model.getWidth() && y < model.getHeight())
							depotPerceived(x, y);
						ip.remove();
					}
				}
			}
		} catch (Exception e) {
			//TODO: uncomment this log entry.
			//logger.log(Level.SEVERE, "Error in perceive!", e);
		}
		return per;
	}
}
