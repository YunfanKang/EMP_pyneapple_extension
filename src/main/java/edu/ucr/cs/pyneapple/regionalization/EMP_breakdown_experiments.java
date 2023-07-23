package edu.ucr.cs.pyneapple.regionalization;

public class EMP_breakdown_experiments {
    public static void main(String[] args) throws Exception {
        //repeatTest();
        //nonRepeatTest();
        //varRangeTest();
        repeatScalabilityTest("30k");

    }
    static void varRangeTest() throws Exception{
        String normalDataset = "data/LACounty/La_county_noisland.shp";
        System.out.println("Var -inf - 10000");
        Double[] varLow = {0.0, 5000.0, 10000.0, 15000.0};
        Double[] varHigh = {10000.0, 15000.0, 20000.0, 25000.0};
        for(int i = 0; i < varLow.length; i++){
            System.out.println("Var " + varLow[i] + " " + varHigh[i]);
            EMP_breakdown.set_input_minmax_var(normalDataset,
                    "pop_16up",
                    -Double.POSITIVE_INFINITY,
                    2000.0,
                    "unemployed",
                    -Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    "employed",
                    -Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    "households",
                    varLow[i],
                    varHigh[i],
                    "pop2010",
                    -Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    -Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    "households",
                    false

            );
        }

    }
    static void nonRepeatTest() throws Exception{
        String normalDataset = "data/LACounty/La_county_noisland.shp";
        String sumDataset = "data/LACounty_sum/La_county_noisland.shp";
        String avgDataset = "data/LACounty_avg/La_county_noisland.shp";
        String minDataset = "data/LACounty_min/La_county_noisland.shp";
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
    }
    static void repeatTest() throws Exception {
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

    }

    static void repeatScalabilityTest(String dataset) throws Exception {
        String normalDataset = "data/30K/30K.shp";
        String minDataset = "data/30K_min/30K.shp";
        String avgDataset = "data/30K_avg/30K.shp";
        String sumDataset = "data/30K_sum/30K.shp";
        System.out.println(normalDataset);
        EMP_breakdown.set_input_minmax_var(normalDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                3000.0,
                "unemployed",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "employed",
                1500.0,
                3500.0,
                "households",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "unemployed",
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
                1500.0,
                3500.0,
                "households",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "unemployed",
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
                1500.0,
                3500.0,
                "households",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "unemployed",
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
                "employed",
                1500.0,
                3500.0,
                "households",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "unemployed",
                20000.0,
                Double.POSITIVE_INFINITY,
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "households",
                true

        );

    }
}
