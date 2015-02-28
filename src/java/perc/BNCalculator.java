package perc;

import jpgm.Factor;
import jpgm.Distribution;
import jpgm.IntArray;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.Locale;
import java.lang.StringBuffer;


public class BNCalculator {
	//
	//	LANGUAGE
	//

	//
	//	PARAMETERS
	//
	static final int CELL_NOISE = 0;

	static int agentID = 0;

	//
	//	GROUPS OF VARIABLES
	//
	public static final int STATE_OF = 1000;
	public static final int UPDATE_OF = 2000;
	public static final int SENSOR_OF = 3000;

	//
	//	SENSOR LOCATIONS
	//
	public static final int NW = 0;
	public static final int NN = 1;
	public static final int NE = 2;
	public static final int WW = 3;
	public static final int CC = 4;
	public static final int EE = 5;
	public static final int SW = 6;
	public static final int SS = 7;
	public static final int SE = 8;

	public static final int[] TOP_ROW = {NW, NN, NE};
	public static final int[] CENTER_ROW = {WW, CC, EE};
	public static final int[] BOTTOM_ROW = {SW, SS, SE};
	public static final int[] LEFT_COL = {NW, WW, SW};
	public static final int[] CENTER_COL = {NN, CC, SS};
	public static final int[] RIGHT_COL = {NE, EE, SE};
	public static final int[] ALL = {NW, NN, NE, WW, CC, EE, SW, SS, SE};
	public static final int[] ALL_BUT_CC = {NW, NN, NE, WW, EE, SW, SS, SE};

	//
	//	CELL CONTENTS / SENSOR VALUES
	//
	public static final int EMPTY		= 0;
	public static final int GOLD		= 1;
	public static final int OBSTACLE	= 2;
	public static final int MINER		= 3;
	public static final int SCAN_ERROR	= 4;

	public static final int[] CELL_CONTENTS = new int[] {EMPTY, GOLD, OBSTACLE, MINER};
	public static final int[] ALL_CELL_CONTENTS = new int[] {EMPTY, GOLD, OBSTACLE, MINER, SCAN_ERROR};
	public static final String[] NAME_CONTENTS = new String[] {"empty", "gold", "obstacle", "miner", "error"};	
	public static final String[] SHORT_CONTENTS = new String[] {"•", "o", "+", "A", "?"};
	//︎
	//	MINER ACTIONS
	//
	public static final int SKIP 	= 0;
	public static final int PICK 	= 1;
	public static final int DROP 	= 2;
	public static final int UP 		= 3;
	public static final int RIGHT	= 4;
	public static final int DOWN 	= 5;
	public static final int LEFT 	= 6;

	public static final int[] MINER_ACTIONS = new int[] {SKIP, PICK, DROP, UP, RIGHT, DOWN, LEFT};	
	public static final String[] NAME_ACTIONS = new String[] {"skip", "pick", "drop", "up", "right", "down", "left"};
	public static final String[] SHORT_ACTIONS = new String[] {"s", "p", "q", "u", "r", "d", "l"};
	//
	//	MOVABLE TO CONTENTS
	//
	static IntArray CAN_MOVE = new IntArray(EMPTY,GOLD);

	//
	//	Belief methods
	//

	static public double[][] uniformBelief() {
		return new double[][]{
			{0.25, 0.25, 0.25, 0.25},
			{0.25, 0.25, 0.25, 0.25},
			{0.25, 0.25, 0.25, 0.25},
			{0.25, 0.25, 0.25, 0.25},
			{0.25, 0.25, 0.25, 0.25},
			{0.25, 0.25, 0.25, 0.25},
			{0.25, 0.25, 0.25, 0.25},
			{0.25, 0.25, 0.25, 0.25},
			{0.25, 0.25, 0.25, 0.25}
		};
	}

	static double[] noisydelta(int x, double n) {
		double[] d = new double[CELL_CONTENTS.length];
		double a = 1.0 - 0.75*n;
		double b = (1.0 - a) / 3.0;
		switch (x) {
			case GOLD:
				d =  new double[] {b,a,b,b};
				break;
			case OBSTACLE:
				d =  new double[] {b,b,a,b};
				break;
			case MINER:
				d =  new double[] {b,b,b,a};
				break;
			case EMPTY:
			default:
				d =  new double[] {a,b,b,b};
				break;
		}
		return d;
	}

	static public double[][] deltaBelief(IntArray x) {
		double[][] d = new double[ ALL.length ][ CELL_CONTENTS.length];
		for (int i = 0; i < ALL.length; i++) {
			d[i] = noisydelta( x.get(i), 0.0 );
		}
		return d;
	}

	static public double[][] noisyBelief(IntArray x, double n) {
		double[][] d = new double[ ALL.length ][ CELL_CONTENTS.length];
		for (int i = 0; i < ALL.length; i++) {
			d[i] = noisydelta( x.get(i), n );
		}
		return d;
	}

	static Factor BELIEF(int pos, double[][] bel) {
		Factor f = new Factor(
			new IntArray(STATE_OF + pos),
			new IntArray(4));
		for (int val : CELL_CONTENTS) {
			f.set(new IntArray(val), bel[pos][val]);
		}

		return f;
	}

	static public boolean contains(int[] a, int x) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == x) {
				return true;
			}
		}
		return false;
	}

	static double[] normalize(double[] a) {
		double s = 0.0;
		double[] n = new double[a.length];

		for (double x : a) s += x;
		s = s == 0.0 ? 0.0 : 1.0/s;
		
		for (int i = 0; i < a.length; i++) {
			n[i] = s * a[i];
		}
		return n;
	}

	static double[] flaten(double[] d, double a) {
		double u = 1.0 / d.length;
		double[] n = new double[d.length];
		for (int i = 0; i < d.length; i++) n[i] = (1.0 - a) * d[i] + a * u;
		return normalize(n);
	}

	//
	//	MODEL FACTORS
	//

	// COPY CONTENT FROM b TO a
	static Factor COPY(int posa, int posb) {
		return new Factor(
		new IntArray(UPDATE_OF + posa, STATE_OF + posb),
		new IntArray(4,4),
		new int[][] { 
			{EMPTY, EMPTY}, {MINER, EMPTY},
			{GOLD, GOLD}, {MINER, GOLD},
			{OBSTACLE, OBSTACLE},
			{EMPTY, MINER}, {GOLD, MINER}, {MINER, MINER}
			 },
		new double[] {
			0.5, 0.5,
			0.5, 0.5,
			1.0,
			0.33, 0.33, 0.33 } );
	}

	// UNIFORM DISTRIBUTION OF a
	static Factor UNIFORM(int a) {
		return  new Factor(
		new IntArray(a),
		new IntArray(4),
		new int[][] {{EMPTY}, {GOLD}, {OBSTACLE}, {MINER} },
		new double[] {0.25, 0.25, 0.25, 0.25} );
	}

	// SCAN NOISE
	static Factor SCAN(int pos, double noise) {
		double e = 0.01;
		double y = 1.0 - 0.75*noise;
		double x = (1.0 - y) / 3.0;
		return new Factor(
			new IntArray(SENSOR_OF + pos, UPDATE_OF + pos),
			new IntArray(ALL_CELL_CONTENTS.length,CELL_CONTENTS.length),
			new int[][] {
				{EMPTY,			EMPTY},
				{GOLD,			EMPTY},
				{OBSTACLE,		EMPTY},
				{MINER,			EMPTY},
				{SCAN_ERROR,	EMPTY},
				{EMPTY,			GOLD},
				{GOLD,			GOLD},
				{OBSTACLE,		GOLD},
				{MINER,			GOLD},
				{SCAN_ERROR,	GOLD},
				{EMPTY,			OBSTACLE},
				{GOLD,			OBSTACLE},
				{OBSTACLE,		OBSTACLE},
				{MINER,			OBSTACLE},
				{SCAN_ERROR,	OBSTACLE},
				{EMPTY,			MINER},
				{OBSTACLE,		MINER},
				{GOLD,			MINER},
				{MINER,			MINER},
				{SCAN_ERROR,	MINER}  },
			new double[] {
				y, x, x, x, e,
				x, y, x, x, e,
				x, x, y, x, e,
				x, x, x, y, e } );
	}

	// PICK CURRENT LOCATION
	static Factor CC_PICK(int pos) {
		return new Factor(
			new IntArray(UPDATE_OF + pos, STATE_OF +  pos),
			new IntArray(4,4),
			new int[][] {
				{EMPTY,		EMPTY},
				{EMPTY,		GOLD},
				{OBSTACLE,	OBSTACLE},
				{MINER, 	MINER} },
			new double[] { 1.0, 1.0, 1.0, 1.0 } );
	}

	// DROP CURRENT LOCATION
	static Factor CC_DROP(int pos) {
		return new Factor(
			new IntArray(UPDATE_OF + pos, STATE_OF + pos),
			new IntArray(4,4),
			new int[][] {
				{GOLD,		EMPTY},
				{GOLD,		GOLD},
				{OBSTACLE, 	OBSTACLE},
				{MINER, 	MINER} },
			new double[] { 1.0, 1.0, 1.0, 1.0 } );
	}

	static Logger logger = Logger.getLogger(BNCalculator.class.getName());

	//
	//	"Normal" argmax
	//
	private static int argmax(double[] y) {
		int i = 0;
		double m = y[i];
		for (int j = 0; j < y.length; j++) {
			if (y[j] > m) {
				i = j;
				m = y[j];
			}
		}
		return i;
	}

	public static IntArray argmax(double[][] y) {
		int[] a = new int[y.length];
		for (int i = 0; i < y.length; i++) {
			a[i] = argmax(y[i]);
		}
		return new IntArray(a);
	}

	static TR getTransition(int pos, int action) {
		int spos = pos;
		Factor transition;
		switch (action) {
			case PICK:
				if (pos == CC) {
					transition = CC_PICK(pos);
				} else {
					transition = COPY(pos,pos);
				}
				break;
			case DROP:
				if (pos == CC) {
					transition = CC_DROP(pos);
				} else {
					transition = COPY(pos,pos);
				}
				break;
			case UP:
				if (contains(TOP_ROW, pos)) {
					transition = UNIFORM(UPDATE_OF + pos);
				} else {
					spos = pos - 3;
					transition = COPY(pos,spos);
				}
				break;
			case RIGHT:
				if (contains(RIGHT_COL, pos)) {
					transition = UNIFORM(UPDATE_OF + pos);
				} else {
					spos = pos + 1;
					transition = COPY(pos,spos);
				}
				break;
			case DOWN:
				if (contains(BOTTOM_ROW, pos)) {
					transition = UNIFORM(UPDATE_OF + pos);
				} else {
					spos = pos + 3;
					transition = COPY(pos,spos);
				}
				break;
			case LEFT:
				if (contains(LEFT_COL, pos)) {
					transition = UNIFORM(UPDATE_OF + pos);
				} else {
					spos = pos - 1;
					transition = COPY(pos,spos);
				}
				break;
			case SKIP:
			default:
				transition = COPY(pos,pos);
				break;
		}
		TR result = new TR();
		result.transition = transition;
		result.spos = spos;
		return result;
	}

	public static double[][] updateBelief(double[][] bel, IntArray per, Integer act, double[] par) {
		double[][] upd = uniformBelief();
		for (int pos : ALL) {
			//
			//	Get the **transition** factor to this (sensor) position and action
			//
			//	T(W,X) = P(W|X)
			//
			TR tr = getTransition(pos, act);
			Factor transition = tr.transition;
			// since positions "change",  the belief must come from 
			// a "target" position, spos
			int spos = tr.spos;
			//
			//	Get the **belief** factor of the target position
			//
			//	B(X) = P(X)
			//
			Factor belief = BELIEF(spos, bel);
			//
			//	Compute the **forward** factor from the **belief** and **transition**
			//
			//	F(W) = sum_x T(W, X = x)B(X = x) = sum_x P(W | X = x)P(X = x)
			//
			Factor forward = 
				transition.mul(belief)
				.marginal(new IntArray(STATE_OF + spos));
			//
			//	Get the perception of the current position
			//
			int posPercept = per.get(pos);
			//	convert "-1" perceptions to obstacles
			posPercept = posPercept < 0 ? SCAN_ERROR : posPercept;
			//
			//	Get the **scan** factor
			//
			//	S(W) = P(Y = y | W) 
			//
			Factor scan = 
				SCAN(pos, par[CELL_NOISE]).filter(
					new IntArray(SENSOR_OF + pos),
					new IntArray( posPercept ) );
			//
			//	Compute the **update** factor
			//
			//	U(W) = S(Y = y,W) F(W) = P(Y = y| W) sum_x P(W | X = x)P(X = x)
			//
			Factor update = scan.mul(forward);
			//
			//	Convert U to [ U(w0), U(w1), ..., U(wn) ]
			//
			double[] p = new double[ CELL_CONTENTS.length ];
			for (int w : CELL_CONTENTS) {
				p[w] = update.get( new IntArray(posPercept, w) );
			}
			//
			//	Make the resulting distribution more flat
			//
			p = flaten(p, 0.0);
			//
			//	Logging
			//
			/*
			logger.fine("\n----------------------------------\nPOS: " +  pos );
			logger.finer("belief\n" + belief);
			logger.finer("transition\n" + transition);
			logger.finer("forward\n" +forward);
			logger.finer("scan (" + posPercept + " = "+ SHORT_CONTENTS[ posPercept ] + ")\n"+scan);
			logger.finer("update\n" + update);
			logger.fine("p: " + BelToString( new double[][] {p}));
			*/
			upd[pos] = p;
		}
		/*
		if (agentID == 0) {
			logger.info("state" + GridToString(argmax(bel)) + 
				"\naction: " + NAME_ACTIONS[ act ] +
				"\nscan"+GridToString(per) + 
				"\nupdate\n" + BelToString(upd) + 
				"\nestimate" + GridToString(argmax(upd)));
		}
		*/
		return upd;
	}
	//
	//	Compute
	//		arg_w max Pu(Yi = yi | Wi = w, pa(Wi) = xvals)
	//	where
	//		Pu(Yi,Wi, pa(Wi)) = transition(pa(Wi), Wi) x SCAN(Yi, Wi, theta)
	//
	private static int argmaxLikelihood(
		Factor transition,	// the factor for X -> Wi
		int pos,			// the scanner position
		IntArray x_vals,	// the evidence for X, as sorted in transition
		int yi_val,			// the value of yi
		double[] theta		// model parameters
		) {
		//
		//	The query variable (as IntArray for compatibility reasons)
		//
		IntArray qvars = new IntArray(SENSOR_OF + pos);
		//
		//	The scanned value
		//
		IntArray yi = new IntArray(yi_val);
		//
		//	The full factor (with scan)
		//		
		//
		Factor scan = SCAN(pos, theta[CELL_NOISE]);
		Factor f = transition.mul(scan); 	
		//
		//	Evidence vars for the query
		//
		IntArray evars = transition.vars();
		//
		//	Index of updated state (w) in evidence vars
		//
		int wi = evars.search(UPDATE_OF + pos);

		double[] ll = new double[4];					// Gather likelihoods
		IntArray evals = new IntArray(x_vals.values());	// Evidence values
		for (int w_value = 0; w_value < 4; w_value++) {
			evals.set(wi, w_value);						// set evidence for w value
			ll[w_value] = f.query(qvars, evars, evals).get(yi);
		}
		int y = argmax(ll);

		return y;
	}

	public static IntArray update(IntArray x, IntArray y, Integer u, double[] theta) {
		// Start with a copy of the previous state
		IntArray z = new IntArray(x.values());
		switch (u) {
			case SKIP: // the environment shouldn't change
				for (int pos : ALL) {
					z.set(pos, argmaxLikelihood(
						COPY(pos, pos),
						pos,
						new IntArray(-1000, x.get(pos)),
						y.get(pos),
						theta ));
				}
				break;
			case PICK:
				for (int pos : ALL_BUT_CC) {
					z.set(pos, argmaxLikelihood(
						COPY(pos, pos),
						pos,
						new IntArray(-1000, x.get(pos)),
						y.get(pos),
						theta ));
				}
				z.set(CC, argmaxLikelihood(
					CC_PICK(CC),
					CC,
					new IntArray(-1000, x.get(CC)),
					y.get(CC),
					theta ));
				break;
			case DROP:
				for (int pos : ALL_BUT_CC) {
					z.set(pos, argmaxLikelihood(
						COPY(pos, pos),
						pos,
						new IntArray(-1000, x.get(pos)),
						y.get(pos),
						theta ));
				}
				z.set(CC, argmaxLikelihood(
					CC_DROP(CC),
					CC,
					new IntArray(-1000, x.get(CC)),
					y.get(CC),
					theta ));
				break;
			case UP:
				for (int pos : TOP_ROW) {
					z.set(pos, argmaxLikelihood(
						UNIFORM(UPDATE_OF + pos),
						pos,
						new IntArray(-1000),
						y.get(pos),
						theta ) );
				}
				for (int pos : CENTER_ROW) {
					z.set(pos, argmaxLikelihood(
						COPY(pos,pos-3),
						pos,
						new IntArray(-1000, x.get(pos-3)),
						y.get(pos),
						theta ) );
				}
				for (int pos : BOTTOM_ROW) {
					z.set(pos, argmaxLikelihood(
						COPY(pos,pos-3),
						pos,
						new IntArray(-1000, x.get(pos-3)),
						y.get(pos),
						theta ) );
				}
				break;
			case DOWN:
				for (int pos : BOTTOM_ROW) {
					z.set(pos, argmaxLikelihood(
						UNIFORM(UPDATE_OF + pos),
						pos,
						new IntArray(-1000),
						y.get(pos),
						theta ) );
				}
				for (int pos : CENTER_ROW) {
					z.set(pos, argmaxLikelihood(
						COPY(pos,pos+3),
						pos,
						new IntArray(-1000, x.get(pos+3)),
						y.get(pos),
						theta ) );
				}
				for (int pos : TOP_ROW) {
					z.set(pos, argmaxLikelihood(
						COPY(pos,pos+3),
						pos,
						new IntArray(-1000, x.get(pos+3)),
						y.get(pos),
						theta ) );
				}
				break;
			case RIGHT:
				for (int pos : RIGHT_COL) {
					z.set(pos, argmaxLikelihood(
						UNIFORM(UPDATE_OF + pos),
						pos,
						new IntArray(-1000),
						y.get(pos),
						theta ) );
				}
				for (int pos : CENTER_COL) {
					z.set(pos, argmaxLikelihood(
						COPY(pos,pos+1),
						pos,
						new IntArray(-1000, x.get(pos+1)),
						y.get(pos),
						theta ) );
				}
				for (int pos : LEFT_COL) {
					z.set(pos, argmaxLikelihood(
						COPY(pos,pos+1),
						pos,
						new IntArray(-1000, x.get(pos+1)),
						y.get(pos),
						theta ) );
				}
				break;
			case LEFT:
				for (int pos : LEFT_COL) {
					z.set(pos, argmaxLikelihood(
						UNIFORM(UPDATE_OF + pos),
						pos,
						new IntArray(-1000),
						y.get(pos),
						theta ) );
				}
				for (int pos : CENTER_COL) {
					z.set(pos, argmaxLikelihood(
						COPY(pos,pos-1),
						pos,
						new IntArray(-1000, x.get(pos-1)),
						y.get(pos),
						theta ) );
				}
				for (int pos : RIGHT_COL) {
					z.set(pos, argmaxLikelihood(
						COPY(pos,pos-1),
						pos,
						new IntArray(-1000, x.get(pos-1)),
						y.get(pos),
						theta ) );
				}
				break;
		}
		return z;
	}

	static public String BelToString(double[][] b) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			for (double x : b[i]) buf.append(String.format(Locale.US,"%.5f ",x));			
			if (i < b.length - 1) {
				buf.append(i % 3 == 2 ? "\n" : " | ");
			}
		}
		return buf.toString();
	}

	static public String GridToString(IntArray b) {
		StringBuffer buf = new StringBuffer();
		for (int pos = 0; pos < 9; pos++) {
			if (pos % 3 == 0) buf.append("\n");
			int c = b.get(pos);
			c = c < 0 ? SCAN_ERROR : c;
			buf.append( SHORT_CONTENTS[ c ]);
		}
		return buf.toString();
	}

	static class TR {
		Factor transition;
		int spos;
	}
}