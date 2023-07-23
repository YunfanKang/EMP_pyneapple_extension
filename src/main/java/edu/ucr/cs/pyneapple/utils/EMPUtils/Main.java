package edu.ucr.cs.pyneapple.utils.EMPUtils;

import java.lang.Math;

public class Main {
    public static void main(String[] args) {
        double N = 3.0;
        double A = 18.0;
        double B = 230.0;
        double V = 40.667;

        // coefficients of quadratic equation
        double a = N / (N + 1);
        double b = -2 * A / (N + 1);
        double c = B - A * A / (N + 1) - V * (N + 1);

        // discriminant
        double D = b * b - 4 * a * c;

        if (D >= 0) {
            // roots of the equation
            double x1 = (-b - Math.sqrt(D)) / (2 * a);
            double x2 = (-b + Math.sqrt(D)) / (2 * a);
            System.out.println("The new value x should be less than " + x1 + " or greater than " + x2 + " to increase the variance.");
        } else {
            System.out.println("No real roots found.");
        }
    }
}
