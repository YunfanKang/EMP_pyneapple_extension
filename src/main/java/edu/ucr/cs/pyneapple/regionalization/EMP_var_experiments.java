package edu.ucr.cs.pyneapple.regionalization;

public class EMP_var_experiments {
    public static void main(String args[]) throws Exception {
        String normalDataset = "data/LACity/LACity.shp";

        EMP_breakdown.set_input_minmax_var(normalDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "unemployed",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "employed",
                2000.0,
                4000.0,
                "households",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "pop2010",
                20000.0,
                Double.POSITIVE_INFINITY,
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "households",
                true

        );
    }
}
