package edu.ucr.cs.pyneapple.regionalization;

public class EMP_breakdown_experiments {
    public static void main(String[] args) throws Exception {
        String normalDataset = "data/LACounty/La_county_noisland.shp";
        String sumDataset = "data/LACounty_sum/La_county_noisland.shp";
        String avgDataset = "data/LACounty_avg/La_county_noisland.shp";
        String minDataset = "data/LACounty_min/La_county_noisland.shp";
        System.out.println("Original round");
        EMP_breakdown.set_input_minmax_var(normalDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                3000.0,
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
        System.out.println("Change of SUM");
        EMP_breakdown.set_input_minmax_var(sumDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                3000.0,
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
        System.out.println("Change of AVG");

        EMP_breakdown.set_input_minmax_var(avgDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                3000.0,
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
        System.out.println("Change of MIN");

        EMP_breakdown.set_input_minmax_var(minDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                3000.0,
                "unemployed",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "employed", 2000.0,
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

        System.out.println("Change of Sum  no repeat");
        EMP_breakdown.set_input_minmax_var(sumDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                3000.0,
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
                false

        );
        System.out.println("Change of AVG  no repea");

        EMP_breakdown.set_input_minmax_var(avgDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                3000.0,
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
                false

        );
        System.out.println("Change of MIN no repeat");

        EMP_breakdown.set_input_minmax_var(minDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                3000.0,
                "unemployed",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "employed", 2000.0,
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
                false

        );
    }
}
