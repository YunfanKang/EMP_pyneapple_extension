package edu.ucr.cs.pyneapple.utils.EMPUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BisectionSolver;
import org.apache.commons.math3.exception.NoBracketingException;

public class VarianceBound {
    public static void main(String[] args) {
        double N = 3.0;
        double A = 18.0;
        double B = 230.0;
        double V = 40.667;

        double[] boundary = findBoundary(N, A, B, V);
        System.out.println("The new value x should be less than " + boundary[0] + " or greater than " + boundary[1] + " to increase the variance.");
    }

    public static double[] findBoundary(double N, double varianceSum, double varianceSumSquare, double V) {
        //Need to set the boundary
        double lowerBound = 0;
        double upperBound = 1000000000;

        BisectionSolver solver = new BisectionSolver();

        UnivariateFunction f = x -> {
            double mean = (varianceSum + x) / (N + 1);
            double sumOfSquares = varianceSumSquare + x * x;
            double variance = (sumOfSquares / (N + 1)) - (mean * mean);
            return variance - V;
        };

        double lowerBoundary = lowerBound;
        double upperBoundary = upperBound;

        try {
            lowerBoundary = solver.solve(1000, f, lowerBound, 1000000000);
        } catch (NoBracketingException e) {
            // lower boundary remains the same
        }

        try {
            upperBoundary = solver.solve(1000, f, 0, upperBound);
        } catch (NoBracketingException e) {
            // upper boundary remains the same
        }

        return new double[] { lowerBoundary, upperBoundary };
    }
}