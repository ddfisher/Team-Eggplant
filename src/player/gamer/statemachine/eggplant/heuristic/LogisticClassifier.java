package player.gamer.statemachine.eggplant.heuristic;

import Jama.Matrix;

public class LogisticClassifier {
	
	private double[] parameters;
	private final double RIDGE = .00001; // 1e-5
	private final int MAX_ITER = 100;
	private final double EPSILON = .0000000001; // 1e-10
	private final boolean VERBOSE = false;
	
	double[] learnCoefficients(double[][] desMat, double[] targets) {
		double[] weights = new double[desMat.length];
		for (int i = 0; i < weights.length; i++) weights[i] = 1.0;
		return learnCoefficients(desMat, targets, weights);
	}
	
	// (m*n)(n*1) ==> m*1
	double multRow(double[] one, double[] two) {
		double total = 0;
		for (int i = 0; i < one.length; i++) total += one[i] * two[i];
		return total;
	}
	
	Matrix constructMatrixWithSingleArray(double[] arr) {
		Matrix m = new Matrix(arr.length, 1);
		for (int i = 0; i < arr.length; i++) m.set(i, 1, arr[i]);
		return m;
	}
	
	double absSumMatrix(Matrix mat) {
		double[][] arr = mat.getArray(); // faster
		double total = 0;
		for (int i = 0; i < mat.getRowDimension(); i++) {
			for (int j = 0; j < mat.getColumnDimension(); j++) {
				total += Math.abs(arr[i][j]);
			}
		}
		return total;
	}
	
	double[] learnCoefficients(double[][] desMat, double[] targets, double[] w) {
		int m = desMat.length, n = desMat[0].length;
		Matrix oldYExp = new Matrix(m, 1);
		for (int i = 0; i < m; i++) oldYExp.set(i, 1, -1);
		Matrix mat = new Matrix(m, n);
		for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) mat.set(i, j, desMat[i][j]);
		Matrix ridgeMat = new Matrix(n, n);
		for (int i = 0; i < n * n; i++) ridgeMat.set(i / n, i % n, ((i % n) == 0) ? RIDGE : 0);
		Matrix coeff = new Matrix(n, 1);
		for (int i = 0; i < n; i++) coeff.set(i, 1, 0);
		Matrix yAdj = new Matrix(n, 1), yExp = new Matrix(n, 1), yDer = new Matrix(n, 1), yAdjWeighted = new Matrix(n, 1);
		for (int i = 0; i < m * n; i++) mat.set(i / n, i % n, 0.0);
		for (int i = 0; i < MAX_ITER; i++) {
			yAdj = mat.times(coeff);
			for (int j = 0; j < n; j++) yExp.set(j, 1, 1.0 / (1.0 + Math.exp(-1 * yAdj.get(j, 1))));
			for (int j = 0; j < n; j++) yDer.set(j, 1, yExp.get(j, 1) * (1.0 - yExp.get(j, 1)));
			for (int j = 0; j < n; j++) yAdjWeighted.set(j, 1, w[j] * 
					(yDer.get(j, 1)*yAdj.get(j, 1) + (targets[j]-yExp.get(j, 1))));
			Matrix weights = new Matrix(n, n);
			for (int j = 0; j < n; j++) for (int k = 0; k < n; k++) weights.set(j, k, (j == k) ? yDer.get(j, 1) * w[j] : 0);
			Matrix trans = mat.transpose();
			Matrix part = trans.times(weights).times(mat).plusEquals(ridgeMat);
			coeff = part.times(trans).times(yAdjWeighted);
			if (absSumMatrix(yExp.minus(oldYExp)) < m * EPSILON) {
				if (VERBOSE) System.out.println("Converged.");
				return coeff.getColumnPackedCopy();
			}
		}
		if (VERBOSE) System.out.println("Failed to converge.");
		return null;
	}
	
	

}
