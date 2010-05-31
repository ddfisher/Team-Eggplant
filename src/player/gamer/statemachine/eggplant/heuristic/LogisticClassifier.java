package player.gamer.statemachine.eggplant.heuristic;

import java.util.Random;

import Jama.Matrix;

public class LogisticClassifier {
	
	private double[] savedCoefficients;
	private boolean converged = false;
	private final double RIDGE = .00001; // 1e-5
	private final int MAX_ITER = 100;
	private final double EPSILON = .0000000001; // 1e-10
	private final boolean VERBOSE = true;
	
	private double sigmoid(double x) {
		return (1.0 / (1.0 + Math.exp(-x)));
	}
	
	public boolean predict(double[] inputs) {
		if (savedCoefficients == null || inputs.length != savedCoefficients.length-1) return false;
		double total = savedCoefficients[0];
		for (int i = 0; i < inputs.length; i++) total += inputs[i] * savedCoefficients[i+1];
		return sigmoid(total) >= .5;
	}
	
	public boolean converged() {
		return converged;
	}
	
	private double absSumMatrix(Matrix mat) {
		double[][] arr = mat.getArray(); // faster
		double total = 0;
		for (int i = 0; i < mat.getRowDimension(); i++) {
			for (int j = 0; j < mat.getColumnDimension(); j++) {
				total += Math.abs(arr[i][j]);
			}
		}
		return total;
	}
	
	public double[] learnCoefficients(double[][] desMat, int[] targets, double[] weights) {
		int m = desMat.length, n = desMat[0].length + 1;
		double[][] in = new double[m][n];
		for (int i = 0; i < m; i++) in[i][0] = 1.0;
		for (int i = 0; i < m; i++) for (int j = 1; j < n; j++) in[i][j] = desMat[i][j-1];
		return learnCoefficientsWithOffset(in, targets, weights);
	}
	
	public double[] learnCoefficients(double[][] desMat, int[] targets) {
		double[] weights = new double[desMat.length];
		for (int i = 0; i < weights.length; i++) weights[i] = 1.0;
		return learnCoefficients(desMat, targets, weights);
	}
	
	private double[] learnCoefficientsWithOffset(double[][] desMat, int[] targets, double[] w) {
		int m = desMat.length, n = desMat[0].length;
		Matrix coeff = new Matrix(n, 1);
		for (int i = 0; i < n; i++) coeff.set(i, 0, 0.0);
		try {
			Matrix oldYExp = new Matrix(m, 1);
			for (int i = 0; i < m; i++) oldYExp.set(i, 0, -1);
			Matrix mat = new Matrix(m, n);
			for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) mat.set(i, j, desMat[i][j]);
			Matrix ridgeMat = new Matrix(n, n);
			for (int i = 0; i < n * n; i++) ridgeMat.set(i / n, i % n, ((i % n) == 0) ? RIDGE : 0);
			Matrix yAdj = new Matrix(m, 1), yExp = new Matrix(m, 1), yDer = new Matrix(m, 1), yAdjWeighted = new Matrix(m, 1);
			// learning cycles
			Matrix trans = mat.transpose();
			for (int i = 0; i < MAX_ITER; i++) {
				yAdj = mat.times(coeff);
				for (int j = 0; j < m; j++) yExp.set(j, 0, 1.0 / (1.0 + Math.exp(-1 * yAdj.get(j, 0))));
				for (int j = 0; j < m; j++) yDer.set(j, 0, yExp.get(j, 0) * (1.0 - yExp.get(j, 0)));
				for (int j = 0; j < m; j++) yAdjWeighted.set(j, 0, w[j] * 
						(yDer.get(j, 0)*yAdj.get(j, 0) + (targets[j]-yExp.get(j, 0))));
				Matrix weights = new Matrix(m, m);
				for (int j = 0; j < m; j++) for (int k = 0; k < m; k++) weights.set(j, k, (j == k) ? yDer.get(j, 0) * w[j] : 0);
				Matrix part = trans.times(weights).times(mat).plusEquals(ridgeMat).inverse();
				coeff = part.times(trans).times(yAdjWeighted);
				if (VERBOSE) printCoefficients(coeff.getColumnPackedCopy());
				if (absSumMatrix(yExp.minus(oldYExp)) < m * EPSILON) {
					savedCoefficients = coeff.getColumnPackedCopy();
					converged = true;
					if (VERBOSE) System.out.println("Converged.");
					return savedCoefficients;
				}
				for (int j = 0; j < m; j++) oldYExp.set(j, 0, yExp.get(j, 0));
			}
			if (VERBOSE) System.out.println("Failed to converge.");
			savedCoefficients = coeff.getColumnPackedCopy();
			converged = false;
			return savedCoefficients;
		} catch (Exception e) { // matrix singular exception
			e.printStackTrace();
			converged = false;
			savedCoefficients = null;
			return coeff.getColumnPackedCopy();
		}
	}
	
	public static void printCoefficients(double[] co) {
		if (co == null) {
			System.out.println("no saved coefficients");
			return;
		}
		System.out.println("Coefficients:");
		for (int i = 0; i < co.length; i++) {
			System.out.println("   " + i + ": " + co[i]);
		}
		System.out.println("");
	}
	
	public void printCoefficients() {
		printCoefficients(savedCoefficients);
	}
	
	public static void testLogReg() {
		// rules: 4x1 + 10x2 > 7000
		int numEx = 1000;
		double[][] dm = new double[numEx][2];
		int[] tar = new int[numEx];
		Random random = new Random();
		for (int i = 0; i < numEx; i++) {
			for (int j = 0; j < 2; j++) dm[i][j] = random.nextDouble() * 1000;
			tar[i] = (4*dm[i][0] + 10*dm[i][1] > 7000) ? 1 : 0;
		}
		Matrix dmm = new Matrix(dm);
		Matrix tarm = new Matrix(tar.length, 1);
		for (int i = 0; i < tar.length; i++) tarm.set(i, 0, tar[i]);
		dmm.print(10, 3);
		tarm.print(10, 3);
		LogisticClassifier lc = new LogisticClassifier();
		lc.learnCoefficients(dm, tar);
		lc.printCoefficients();
		int correct = 0;
		for (int i = 0; i < tar.length; i++) {
			if (lc.predict(dm[i]) == (tar[i] == 1)) correct++;
		}
		System.out.println("Accuracy: " + correct + "/" + tar.length + ": " + (double)correct/tar.length);
	}
}
