package test;

import perc.BNCalculator;
import jpgm.IntArray;

public class BNCalcTest {

	static void testUpdate() {
		IntArray state = new IntArray(0,0,3,1,1,1,0,0,2);
		IntArray perc = new IntArray(1,2,3,0,0,0,0,0,0);
		double[] theta = new double[]{0.1};

		IntArray estimate;

		for (int action : new int[]{BNCalculator.DROP}) {
			estimate = BNCalculator.update(state, perc, action, theta);
			System.out.println(">>>>> action: " + BNCalculator.NAME_ACTIONS[action]);
			System.out.println("state" + BNCalculator.GridToString(state));
			System.out.println("perc" + BNCalculator.GridToString(perc));
			System.out.println("estimate" + BNCalculator.GridToString(estimate));
		}
	}

	static double[][] step(double[][] b, IntArray p, int a, double[] t) {
		double[][] u;
		u = BNCalculator.updateBelief(b, p, a, t);
		System.out.println("\n====================================\n");
		System.out.println("grid belief\n" + BNCalculator.BelToString(b));
		System.out.println("state estimation from grid belief" + BNCalculator.GridToString(
			BNCalculator.argmax(b)));
		System.out.println("action: " +  BNCalculator.NAME_ACTIONS[a]);
		System.out.println("grid perception" + BNCalculator.GridToString(p));
		System.out.println("updated grid belief\n" + BNCalculator.BelToString(u));
		System.out.println("state estimation from updated grid belief" + BNCalculator.GridToString(BNCalculator.argmax(u)));
		return u;
	}

	static void testUpdateBelief() {
		double error = 0.05;
		IntArray bv = new IntArray(
			0,0,0,
			0,0,0,
			0,0,0);
		double[][] b = BNCalculator.noisyBelief(bv, 0.99);
		double[][] u;

		IntArray p = new IntArray(
			0,0,0,
			2,0,2,
			2,0,0);
		double[] t = new double[]{error};

		b = step(b, p, BNCalculator.DROP, t );
		p.set(1, BNCalculator.MINER);
		p.set(3, BNCalculator.EMPTY);
		b = step(b, p, BNCalculator.PICK, t );
		b = step(b, p, BNCalculator.SKIP, t );
		p = new IntArray(
			2,3,3,
			2,0,1,
			2,0,3);
		b = step(b, p, BNCalculator.LEFT, t );

	}
	static public void main(String[] args) {
		testUpdateBelief();
	}
}