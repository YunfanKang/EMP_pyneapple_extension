package edu.ucr.cs.pyneapple.regionalization;

import edu.ucr.cs.pyneapple.utils.EMPUtils.*;
import edu.ucr.cs.pyneapple.utils.SpatialGrid;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Triplet;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

//public class EMP_breakdown implements RegionalizationMethod {
public class EMP_breakdown{
    static boolean debug = false;
    static boolean repeat_debug = false;
    static boolean var_debug = false;

    static boolean localrec_debug = true;
    static boolean check_p_afterAVG = false;
    static boolean labelCheck = false;
    static boolean varForSum = false;
    static int reconstructedAreas = 0;


    static double varLowerbound, varUpprbound;

    static int toleranceThreshold =100;
    static int numOfIts = 1;
    static int randFlag[] = {1,1};
    static int rand[] = {0, 1, 2};
    static String rands[] = {"S", "R", "B"};
    static int mergeLimit = 3;
    static String testName = "RandWithMergeLimit";

    static double minTime = 0;
    static double avgTime = 0;
    static double sumTime = 0;

    /*static ArrayList<Integer>[] prevConstAttrs = new ArrayList[5];
    static double[] prevConstraintValues = new double[12];

    static List<Integer> prevMinMaxSeed = new ArrayList<>();
    static int[] prevMinMaxLabels = new int[1];

    static Quartet<int[], Map<Integer, RegionWithVariance>, List<Integer>, List<Integer>> prevAvgResult = null;

    static Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> prevVarResult = null;

    static Pair<int[], Map<Integer, RegionWithVariance>> prevSumCountResult = null;
    static boolean changeFlag = false;*/


    RegionCollection constructionPartition;
    TabuReturn finalPartition;

    //@Override
    public int getP() {
        return constructionPartition.getMax_p();
    }

    //@Override
    public int[] getRegionLabels() {
        return finalPartition.labels;
    }
    //@Override
    public void execute_regionalization(Map<Integer, Set<Integer>> neighbor,
                                        ArrayList<Long> disAttr,
                                        ArrayList<Long> sumAttr,
                                        Long thresholdLong){
        int tabuLength = 100;
        Double threshold = thresholdLong.doubleValue();
        int max_no_move = disAttr.size();
        SpatialGrid sg = new SpatialGrid();
        sg.setNeighbors(neighbor);
        ArrayList idList = new ArrayList<Integer>();
        for(int i = 0 ; i < disAttr.size(); i++){
            idList.add(i);
        }
        //constructionPartition = construction_phase_breakdown_minmaxNoRepeat_variance(idList, disAttr, sg, sumAttr, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, sumAttr, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, sumAttr, -Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, sumAttr,-Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, sumAttr, threshold, Double.POSITIVE_INFINITY, -Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false, "");
        //finalPartition = EMPTabu.performTabu(constructionPartition.getLabels(), constructionPartition.getRegionMap(), sg, EMPTabu.pdist((disAttr)), tabuLength, max_no_move, sumAttr, sumAttr, sumAttr, sumAttr);

    }


    public void execute_regionalization(Map<Integer, Set<Integer>> neighbor,
                                        ArrayList<Long> disAttr,
                                        ArrayList<Long> minAttr,
                                        Double minLowerBound,
                                        Double minUpperBound,

                                        ArrayList<Long> maxAttr,
                                        Double maxLowerBound,
                                        Double maxUpperBound,

                                        ArrayList<Long> avgAttr,
                                        Double avgLowerBound,
                                        Double avgUpperBound,

                                        ArrayList<Long> sumAttr,
                                        Double sumLowerBound,
                                        Double sumUpperBound,

                                        Double countLowerBound,
                                        Double countUpperBound ){
        int tabuLength = 100;
        int max_no_move = disAttr.size();
        SpatialGrid sg = new SpatialGrid();
        sg.setNeighbors(neighbor);
        ArrayList idList = new ArrayList<Integer>();
        for(int i = 0 ; i < disAttr.size(); i++){
            idList.add(i);
        }
        //constructionPartition = construction_phase_breakdown(idList, disAttr, sg, minAttr, minLowerBound, minUpperBound, maxAttr, maxLowerBound, maxUpperBound, avgAttr, avgLowerBound, avgUpperBound, sumAttr, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
        //finalPartition = EMPTabu.performTabu(constructionPartition.getLabels(), constructionPartition.getRegionMap(), sg, EMPTabu.pdist((disAttr)), tabuLength, max_no_move, minAttr, maxAttr, sumAttr, avgAttr);

    }
    public static Pair filtering_and_seeding(ArrayList<Integer> areas,
                                             ArrayList<Long> minAttr,
                                             Double minLowerBound,
                                             Double minUpperBound,

                                             ArrayList<Long> maxAttr,
                                             Double maxLowerBound,
                                             Double maxUpperBound,


                                             ArrayList<Long> sumAttr,
                                             Double sumUpperBound){
        int[] labels = new int[maxAttr.size()];//The size of the attribute sets should be the same.
        if (minLowerBound != -Double.POSITIVE_INFINITY || maxUpperBound != Double.POSITIVE_INFINITY || sumUpperBound != Double.POSITIVE_INFINITY) {
            Iterator<Integer> idIterator = areas.iterator();
            while (idIterator.hasNext()) {
                Integer id = idIterator.next();
                if (minAttr.get(id) < minLowerBound || maxAttr.get(id) > maxUpperBound || sumAttr.get(id) > sumUpperBound) {
                    idIterator.remove();
                    labels[id] = -2; //Remove areas
                }
            }
        }

        ArrayList<Integer> seedAreas = new ArrayList<Integer>();
        if (minUpperBound != Double.POSITIVE_INFINITY || maxLowerBound != -Double.POSITIVE_INFINITY) {
            Iterator<Integer> idIterator = areas.iterator();
            while (idIterator.hasNext()) {
                Integer id = idIterator.next();
                if (minAttr.get(id) <= minUpperBound || maxAttr.get(id) >= maxLowerBound)//Changed from and to or
                    seedAreas.add(id);
            }
        } else {
            seedAreas.addAll(areas);
        }
        //System.out.println("Seed areas: " + seedAreas.size());
        //RegionCollectionNew rc = construction_phase_average(avgAttr, disAttr, 1, r, idList, avgLowerBound, avgUpperBound, seedAreas);
        if(debug){
            System.out.println("Seed areas: " + seedAreas.size());
            System.out.println(seedAreas);
        }
        Pair<int[], ArrayList<Integer>> minmaxResult = new Pair<int[], ArrayList<Integer>>(labels, seedAreas);
        return minmaxResult;
    }

    public static Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> region_initialization_var(int[] labels,
                                                                                                            ArrayList<Integer> seedAreas,
                                                                                                            SpatialGrid r,
                                                                                                            ArrayList<Long> minAttr,
                                                                                                            Double minUpperBound,
                                                                                                            ArrayList<Long> maxAttr,
                                                                                                            Double maxLowerBound,
                                                                                                            ArrayList<Long> avgAttr,
                                                                                                            ArrayList<Long> varAttr,
                                                                                                            Double varLowerBound,
                                                                                                            Double varUpperBound,
                                                                                                            ArrayList<Long> sumAttr
    ){
        int cId; //Counter for numbering the regions
        Map<Integer, RegionWithVariance> regionList = new HashMap<Integer, RegionWithVariance>();
        List<Integer> unassignedVar = new ArrayList<Integer>();

        //If the variance lower bound <= 0, then an individual area can be initialzed as a region. Otherwise, no single area can become a region because the variance would be 0.
        if(varLowerBound <= 0){
            for(int arr_index: seedAreas){
                cId = regionList.size() + 1;
                RegionWithVariance newRegion = new RegionWithVariance(cId);
                newRegion.addArea(arr_index, minAttr.get(arr_index), maxAttr.get(arr_index), avgAttr.get(arr_index), varAttr.get(arr_index),sumAttr.get(arr_index), r);
                //System.out.println(newRegion.getAreaList() + "Area var" + varAttr.get(arr_index) + " region VarSum " +newRegion.getVarianceSum());
                regionList.put(cId, newRegion);
                labels[arr_index] = cId;
            }
        }else{
            unassignedVar.addAll(seedAreas);
        }


        boolean regionChange = true;
        Set<Integer> removedVar = new HashSet<Integer>();
        //Set<Integer> removedHigh = new HashSet<Integer>();
        int count = 0;
        //RemovedLow and removedHigh also contains non-seed-areas
        while (!removedVar.containsAll(unassignedVar)  && regionChange) {
            regionChange = false;
            Iterator<Integer> iteratorVar = unassignedVar.iterator();

            if(var_debug){
                count++;
                System.out.println("Var Round " + count);
            }
            int varCount = 0;
            while (iteratorVar.hasNext()) {
                varCount++;
                if(var_debug){
                    System.out.println("varCount " + varCount);
                    checkLabels_var(labels, regionList);

                }

                Integer varArea = iteratorVar.next();
                if (removedVar.contains(varArea)) {
                    continue;
                }
                RegionWithVariance tr = new RegionWithVariance(-1);
                removedVar.add(varArea);
                labels[varArea] = -1;
                tr.addArea(varArea, minAttr.get(varArea), maxAttr.get(varArea), avgAttr.get(varArea), varAttr.get(varArea), sumAttr.get(varArea), r);
                boolean feasible = false;
                boolean updated = true;
                int worseSteps = toleranceThreshold;
                boolean roundWithNoUpdate = false;
                while (!feasible && updated) {

                    updated = false;
                    Set<Integer> neighborSet = tr.getAreaNeighborSet();
                    //Collections.shuffle((List<?>) neighborSet);
                    List<Integer> neighborList = new ArrayList<>(neighborSet);

                    /*if(randFlag[0] == 1){
                        Collections.shuffle(neighborList);
                    }*/
                    List<Integer> seedNeighbors = new ArrayList<>();
                    List<Integer> nonSeedNeighbors = new ArrayList<>();
                    boolean preferSeeds = true;
                    if(preferSeeds){
                        for(Integer neighbor: neighborList){
                            if(seedAreas.contains(neighbor)){
                                seedNeighbors.add(neighbor);
                            }else{
                                nonSeedNeighbors.add(neighbor);
                            }
                        }
                        neighborList = new ArrayList<>();
                        neighborList.addAll(seedNeighbors);
                        neighborList.addAll(nonSeedNeighbors);
                    }

                    if(debug){
                        System.out.println("Neighbor areas for region " + tr.getAreaList() + " is " + neighborList);
                        for(int neighbor : neighborList){
                            System.out.print(labels[neighbor] + " ");
                        }
                        System.out.println();
                    }


                    if (tr.getVariance() < varLowerBound) {
                        if(randFlag[0] == 2){
                            //Order>
                            neighborList.sort((Integer area1, Integer area2) -> Double.valueOf(varAttr.get(area2)-tr.getVarianceAverage()).compareTo(varAttr.get(area1)-tr.getVarianceAverage()));
                        }
                        for (Integer i : neighborList) {
                            if(labels[i] != 0)//Area is assigned or removed
                                continue;

                            //double[] varianceBoundary = VarianceBound.findBoundary(tr.getCount(), tr.getVarianceSum(), tr.getVarianceSumSquare(), tr.getVariance());
                            double newVariance = RegionWithVariance.simpleVariance(tr.getCount() + 1, tr.getVarianceSum()+ varAttr.get(i), tr.getVarianceSumSquare() + varAttr.get(i) *varAttr.get(i) );
                            if(debug){
                                //System.out.println()
                                System.out.println("Choose area " + i + " new variance becomes " + newVariance);
                            }
                            if(tr.getVariance() < varLowerBound){
                                if(newVariance > tr.getVariance() && newVariance < varUpperBound){
                                    if(var_debug){
                                        double oldVariance = tr.getVariance();
                                        System.out.println("Region below varLowerBound: Variance changes from " + oldVariance + " to " + newVariance + " compare to lower bound: " +  (newVariance > varLowerBound));
                                    }

                                    tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), varAttr.get(i), sumAttr.get(i), r);
                                    removedVar.add(i);
                                    updated = true;


                                }else if(roundWithNoUpdate && worseSteps > 0){
                                    if(var_debug){
                                        double oldVariance = tr.getVariance();
                                        System.out.println("Region below varLowerBound: Worse - lower, Variance changes from " + oldVariance + " to " + newVariance + " compare to lower bound: " +  (newVariance > varLowerBound));
                                    }
                                    worseSteps --;
                                    tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), varAttr.get(i), sumAttr.get(i), r);
                                    removedVar.add(i);
                                    updated = true;
                                    roundWithNoUpdate = false;
                                }

                            }
                        }
                    } else if (tr.getVariance() > varUpperBound) {
                        if(randFlag[0] == 2){
                            neighborList.sort((Integer area1, Integer area2) -> Double.valueOf(varAttr.get(area1)-tr.getVarianceAverage()).compareTo(varAttr.get(area2)-tr.getVarianceAverage()));
                        }
                        for (Integer i : neighborList) {
                            if(labels[i] != 0)
                                continue;
                            //double[] varianceBoundary = VarianceBound.findBoundary(tr.getCount(), tr.getVarianceSum(), tr.getVarianceSumSquare(), tr.getVariance());
                            double newVariance = RegionWithVariance.simpleVariance(tr.getCount() + 1, tr.getVarianceSum()+ varAttr.get(i), tr.getVarianceSumSquare() + varAttr.get(i) *varAttr.get(i) );
                            if(tr.getVariance() > varUpperBound){
                                if(newVariance< tr.getVariance() && newVariance > varLowerBound){
                                    if(var_debug){
                                        double oldVariance = tr.getVariance();
                                        System.out.println("Region above varUpperBound: Variance changes from " + oldVariance + " to " + newVariance + " compare to upper bound: " +  (newVariance < varUpperBound));
                                    }
                                    tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), varAttr.get(i), sumAttr.get(i), r);
                                    removedVar.add(i);
                                    updated = true;
                                }else if(roundWithNoUpdate && worseSteps > 0){
                                    if(var_debug){
                                        double oldVariance = tr.getVariance();
                                        System.out.println("Region above varUpperBound: Worse - higher Variance changes from " + oldVariance + " to " + newVariance + " compare to upper bound: " +  (newVariance < varUpperBound));
                                    }
                                    worseSteps --;
                                    tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), varAttr.get(i), sumAttr.get(i), r);
                                    removedVar.add(i);
                                    updated = true;
                                    roundWithNoUpdate = false;
                                }

                            }
                        }
                    } else {
                        feasible = true;
                        regionChange = true;
                        if(debug){System.out.println("Region Change");}
                        labels = tr.updateId(regionList.size() + 1, labels);
                        regionList.put(regionList.size() + 1, tr);
                    }
                    if(!updated && !roundWithNoUpdate){
                        roundWithNoUpdate = true;
                        updated = true;
                    }

                }
                if (!feasible) {
                    if(var_debug){
                        System.out.println("Region infeasible, revoke. Releasing " + Arrays.toString(tr.getAreaList().toArray(new Integer[0])));

                    }
                    for (Integer area : tr.getAreaList()) {
                        labels[area] = 0;
                        removedVar.remove(area);
                    }
                }

            }
        }
        unassignedVar.removeAll(removedVar);

        for (int arr_index = 0; arr_index < varAttr.size(); arr_index++) {
            if (seedAreas.contains(arr_index))
                continue;
            if(labels[arr_index] == 0) //Consider only "unassigned" areas?
                unassignedVar.add(arr_index);
        }

        boolean updated = true;
        while(updated){
            Iterator<Integer> iteratorVar = unassignedVar.iterator();
            updated = false;
            while (iteratorVar.hasNext()) {
                Integer varArea = iteratorVar.next();
                List<Integer> neighborList = new ArrayList<>(r.getNeighbors(varArea));
                if(randFlag[0] == 0){
                    Collections.shuffle(neighborList);
                }

                for (Integer neighborArea : neighborList) {
                    //if(labels[neighborArea] != 0){
                    if (labels[neighborArea] > 0) {
                        RegionWithVariance nr = regionList.get(labels[neighborArea]);
                        double tmpVarianceSum = nr.getVarianceSum() + varAttr.get(varArea);
                        double tmpVarianceSumSquare = nr.getVarianceSumSquare() + Math.pow(varAttr.get(varArea), 2);
                        //double tmpVarianceAverage = (nr.getVarianceAverage() * nr.getCount() + varAttr.get(varArea)) / (nr.getCount() + 1.0);
                        double tmpVariance = RegionWithVariance.simpleVariance(nr.getCount()+1, tmpVarianceSum, tmpVarianceSumSquare);
                        if(var_debug)
                            System.out.println(tmpVariance);
                        if(tmpVariance >= varLowerBound && tmpVariance <= varUpperBound){
                            nr.addArea(varArea, minAttr.get(varArea), maxAttr.get(varArea), avgAttr.get(varArea), varAttr.get(varArea),sumAttr.get(varArea), r);
                            labels[varArea] = regionList.get(labels[neighborArea]).getId();
                            iteratorVar.remove();
                            updated = true;
                            break;
                        }

                    }
                }
                if(var_debug){

                    for (Integer neighborArea : neighborList) {
                        System.out.print(labels[neighborArea] + " ");
                    }
                    System.out.println("Area " + varArea + " remain unassigned");
                }

            }
        }
        if(true){
            int unassignedCountBefore = 0;
            for(int i = 0; i < labels.length; i++){
                if(labels[i] <= 0){
                    unassignedCountBefore += 1;
                }
            }
            System.out.println("Before MIN/MAX, unassigned : " + unassignedCountBefore + " " + regionList.size());
        }


        List<Integer> notMin = new ArrayList<Integer>();
        List<Integer> notMax = new ArrayList<Integer>();
        for (Map.Entry<Integer, RegionWithVariance> regionEntry : regionList.entrySet()) {
            RegionWithVariance region = regionEntry.getValue();
            if(region.getMin() > minUpperBound){
                notMin.add(region.getId());
            }
            if(region.getMax() < maxLowerBound){
                notMax.add(region.getId());
            }
        }
        if(debug){
            System.out.println("Not min: " + notMin.size());
            System.out.println(notMin);
            for (Integer notMinId: notMin) {
                RegionWithVariance notMinRegion = regionList.get(notMinId);
                System.out.println("Not min ID:" + notMinRegion.getId());
                System.out.println("Min:" + notMinRegion.getMin());
                System.out.println("Max:" + notMinRegion.getMax());
                System.out.println("Avg:" + notMinRegion.getAverage());
                System.out.println("Sum:" + notMinRegion.getSum());

                System.out.println("Count:" + notMinRegion.getCount());
                System.out.println("Areas: " + notMinRegion.getAreaList());
                System.out.println("Satisfiable:" + notMinRegion.satisfiable());

            }
            System.out.println("Not max: " + notMax.size());
            System.out.println(notMax);
            for (Integer notMaxId: notMax) {
                RegionWithVariance notMaxRegion = regionList.get(notMaxId);
                System.out.println("Not max Id:" + notMaxRegion.getId());
                System.out.println("Min:" + notMaxRegion.getMin());
                System.out.println("Max:" + notMaxRegion.getMax());
                System.out.println("Avg:" + notMaxRegion.getAverage());
                System.out.println("Sum:" + notMaxRegion.getSum());

                System.out.println("Count:" + notMaxRegion.getCount());
                System.out.println("Areas: " + notMaxRegion.getAreaList());
                System.out.println("Satisfiable:" + notMaxRegion.satisfiable());

            }
        }
        boolean minMaxMerged = true;
        while(minMaxMerged){
            minMaxMerged = false;
            Iterator<Integer> notMinIterator = notMin.iterator();

            while(notMinIterator.hasNext()){
                Integer notMinRegion = notMinIterator.next();

                Set<Integer> neighborSet = regionList.get(notMinRegion).getRegionNeighborSet(labels);
                List<Integer>regionNeighbor = new ArrayList<>(neighborSet);

                //System.out.println(regionNeighbor.size());

                if(randFlag[0] >= 1){
                    Collections.shuffle(regionNeighbor);
                }

                for(Integer neighbor:regionNeighbor){
                    if(notMax.contains(neighbor)){
                        minMaxMerged = true;
                        RegionWithVariance mergedRegion = regionList.get(notMinRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                        mergedRegion.updateId(notMinRegion, labels);
                        regionList.remove(notMinRegion);
                        regionList.remove(neighbor);
                        regionList.put(notMinRegion, mergedRegion);
                        notMinIterator.remove();
                        notMax.remove(neighbor);
                        break;

                    }
                }
            }
            notMinIterator = notMin.iterator();
            while(notMinIterator.hasNext()){
                Integer notMinRegion = notMinIterator.next();

                Set<Integer> neighborSet = regionList.get(notMinRegion).getRegionNeighborSet(labels);
                List<Integer>regionNeighbor = new ArrayList<>(neighborSet);

                if(randFlag[0] >= 1){
                    Collections.shuffle(regionNeighbor);
                }

                for(Integer neighbor:regionNeighbor){
                    /*if(regionList.get(neighbor).satisfiable()){
                        System.out.println("Satisfiable");
                    }*/
                    if(!notMin.contains(neighbor)){
                        //System.out.println("Merge notMin and notMax");
                        int newCount = regionList.get(notMinRegion).getCount() + regionList.get(neighbor).getCount();
                        double newVarSum = regionList.get(notMinRegion).getVarianceSum() + regionList.get(neighbor).getVarianceSum();
                        double newVarSumSquare = regionList.get(notMinRegion).getVarianceSumSquare() + regionList.get(neighbor).getVarianceSumSquare();
                        double newVar = RegionWithVariance.simpleVariance(newCount, newVarSum, newVarSumSquare);
                        if(newVar < varLowerBound || newVar > varUpperBound)
                            continue;
                        minMaxMerged = true;
                        RegionWithVariance mergedRegion = regionList.get(notMinRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                        mergedRegion.updateId(neighbor, labels);
                        regionList.remove(notMinRegion);
                        regionList.remove(neighbor);
                        regionList.put(neighbor, mergedRegion);
                        notMinIterator.remove();
                        //notMax.remove(neighbor);
                        break;

                    }
                }
            }
            Iterator<Integer> notMaxIterator = notMax.iterator();
            while(notMaxIterator.hasNext()){
                Integer notMaxRegion = notMaxIterator.next();

                Set<Integer> neighborSet = regionList.get(notMaxRegion).getRegionNeighborSet(labels);
                List<Integer>regionNeighbor = new ArrayList<>(neighborSet);
                if(randFlag[0] >= 1){
                    Collections.shuffle(regionNeighbor);
                }
                for(Integer neighbor:regionNeighbor){
                    if(!notMax.contains(neighbor)){
                        int newCount = regionList.get(notMaxRegion).getCount() + regionList.get(neighbor).getCount();
                        double newVarSum = regionList.get(notMaxRegion).getVarianceSum() + regionList.get(neighbor).getVarianceSum();
                        double newVarSumSquare = regionList.get(notMaxRegion).getVarianceSumSquare() + regionList.get(neighbor).getVarianceSumSquare();
                        double newVar = RegionWithVariance.simpleVariance(newCount, newVarSum, newVarSumSquare);
                        if(newVar < varLowerBound || newVar > varUpperBound)
                            continue;
                        minMaxMerged = true;
                        RegionWithVariance mergedRegion = regionList.get(notMaxRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                        mergedRegion.updateId(neighbor, labels);
                        regionList.remove(notMaxRegion);
                        regionList.remove(neighbor);
                        regionList.put(neighbor, mergedRegion);
                        notMaxIterator.remove();
                        //notMax.remove(neighbor);
                        break;

                    }
                }
            }
        }

        if(var_debug){
            System.out.println("Not min after merging Min Max: " + notMin.size());
            System.out.println(notMin);
            for (Integer notMinId: notMin) {
                RegionWithVariance notMinRegion = regionList.get(notMinId);
                System.out.println("Not min ID:" + notMinRegion.getId());
                System.out.println("Min:" + notMinRegion.getMin());
                System.out.println("Max:" + notMinRegion.getMax());
                System.out.println("Avg:" + notMinRegion.getAverage());
                System.out.println("Sum:" + notMinRegion.getSum());

                System.out.println("Count:" + notMinRegion.getCount());
                System.out.println("Areas: " + notMinRegion.getAreaList());
                System.out.println("Satisfiable:" + notMinRegion.satisfiable());

            }
            System.out.println("Not max after merging Min Max: " + notMax.size());
            System.out.println(notMax);
            for (Integer notMaxId: notMax) {
                RegionWithVariance notMaxRegion = regionList.get(notMaxId);
                System.out.println("Not max Id:" + notMaxRegion.getId());
                System.out.println("Min:" + notMaxRegion.getMin());
                System.out.println("Max:" + notMaxRegion.getMax());
                System.out.println("Avg:" + notMaxRegion.getAverage());
                System.out.println("Sum:" + notMaxRegion.getSum());

                System.out.println("Count:" + notMaxRegion.getCount());
                System.out.println("Areas: " + notMaxRegion.getAreaList());
                System.out.println("Satisfiable:" + notMaxRegion.satisfiable());

            }
        }
        if(true){
            int unassignedCountAfter = 0;
            for(int i = 0; i < labels.length; i++){
                if(labels[i] <= 0){
                    unassignedCountAfter += 1;
                }
            }
            System.out.println("After MIN/MAX, unassigned : " + unassignedCountAfter + " " + regionList.size());
        }
        Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> avgInitialization = new Triplet<>(labels, regionList, unassignedVar);
        return avgInitialization;
    }

    public static Quartet<int[], Map<Integer, RegionWithVariance>, List<Integer>, List<Integer>> region_initialization(int[] labels,
                                                                                                                       ArrayList<Integer> seedAreas,
                                                                                                                       SpatialGrid r,
                                                                                                                       ArrayList<Long> minAttr,
                                                                                                                       Double minUpperBound,
                                                                                                                       ArrayList<Long> maxAttr,
                                                                                                                       Double maxLowerBound,
                                                                                                                       ArrayList<Long> avgAttr,
                                                                                                                       Double avgLowerBound,
                                                                                                                       Double avgUpperBound,
                                                                                                                       ArrayList<Long> varAttr,
                                                                                                                       Double varLowerBound,
                                                                                                                       Double varUpperBound,
                                                                                                                       ArrayList<Long> sumAttr
    ){
        int cId; //Counter for numbering the regions
        Map<Integer, RegionWithVariance> regionList = new HashMap<Integer, RegionWithVariance>();
        List<Integer> unassignedLow = new ArrayList<Integer>();
        List<Integer> unassignedHigh = new ArrayList<Integer>();
        //Classify the seed-areas
        for (int arr_index : seedAreas) {
            if (avgAttr.get(arr_index) < avgLowerBound) {
                unassignedLow.add(arr_index);
            } else if (avgAttr.get(arr_index) > avgUpperBound) {
                unassignedHigh.add(arr_index);
            } else {
                cId = regionList.size() + 1;
                RegionWithVariance newRegion = new RegionWithVariance(cId);
                newRegion.addArea(arr_index, minAttr.get(arr_index), maxAttr.get(arr_index), avgAttr.get(arr_index), varAttr.get(arr_index), sumAttr.get(arr_index), r);
                regionList.put(cId, newRegion);
                labels[arr_index] = cId;
            }
        }
        boolean regionChange = true;
        Set<Integer> removedLow = new HashSet<Integer>();
        Set<Integer> removedHigh = new HashSet<Integer>();
        int count = 0;
        //RemovedLow and removedHigh also contains non-seed-areas
        while (!(removedLow.containsAll(unassignedLow) && removedHigh.containsAll(unassignedHigh)) && regionChange) {
            regionChange = false;
            Iterator<Integer> iteratorLow = unassignedLow.iterator();

            if(debug){
                count++;
                System.out.println("Low Round " + count);
            }

            while (iteratorLow.hasNext()) {
                Integer lowArea = iteratorLow.next();
                if (removedLow.contains(lowArea)) {
                    continue;
                }
                RegionWithVariance tr = new RegionWithVariance(-1);
                removedLow.add(lowArea);
                labels[lowArea] = -1;
                tr.addArea(lowArea, minAttr.get(lowArea), maxAttr.get(lowArea), avgAttr.get(lowArea), varAttr.get(lowArea), sumAttr.get(lowArea), r);
                boolean feasible = false;
                boolean updated = true;
                while (!feasible && updated) {
                    updated = false;
                    Set<Integer> neighborSet = tr.getAreaNeighborSet();
                    //Collections.shuffle((List<?>) neighborSet);
                    List<Integer> neighborList = new ArrayList<>(neighborSet);
                    if(randFlag[0] == 1){
                        Collections.shuffle(neighborList);
                    }


                    if (tr.getAverage() < avgLowerBound) {
                        if(randFlag[0] == 2){
                            neighborList.sort((Integer area1, Integer area2) -> avgAttr.get(area2).compareTo(avgAttr.get(area1)));
                        }
                        for (Integer i : neighborList) {
                            if (labels[i] == 0 && avgAttr.get(i) > avgUpperBound && !removedHigh.contains(i)) {
                                tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), varAttr.get(i), sumAttr.get(i), r);
                                removedHigh.add(i);
                                labels[i] = -1;
                                updated = true;
                                break;
                            }
                        }
                    } else if (tr.getAverage() > avgUpperBound) {
                        if(randFlag[0] == 2){
                            neighborList.sort((Integer area1, Integer area2) -> avgAttr.get(area1).compareTo(avgAttr.get(area2)));
                        }
                        for (Integer i : neighborList) {
                            if (labels[i] == 0 && avgAttr.get(i) < avgLowerBound && !removedLow.contains(i)) {
                                tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), varAttr.get(i), sumAttr.get(i), r);
                                removedLow.add(i);
                                labels[i] = -1;
                                updated = true;
                                break;
                            }
                        }
                    } else {
                        feasible = true;
                        regionChange = true;
                        if(debug){System.out.println("Region Change");}
                        labels = tr.updateId(regionList.size() + 1, labels);
                        regionList.put(regionList.size() + 1, tr);
                    }


                }
                if (!feasible) {

                    for (Integer area : tr.getAreaList()) {
                        labels[area] = 0;
                        if (avgAttr.get(area) < avgLowerBound) {
                            removedLow.remove(area);
                        } else {
                            removedHigh.remove(area);
                        }
                    }
                }

            }
        }
        unassignedHigh.removeAll(removedHigh);
        unassignedLow.removeAll(removedLow);

        //Add the block that starts with unassigned High
        regionChange = true;
        //Maybe the condition for the while loop can be only for low or high? The order for low and high may also matter.
        while (!(removedLow.containsAll(unassignedLow) && removedHigh.containsAll(unassignedHigh)) && regionChange) {
            regionChange = false;
            Iterator<Integer> iteratorHigh = unassignedHigh.iterator();

            if(debug){
                count++;
                System.out.println("High Round " + count);
            }

            while (iteratorHigh.hasNext()) {
                Integer HighArea = iteratorHigh.next();
                if (removedHigh.contains(HighArea)) {
                    continue;
                }
                RegionWithVariance tr = new RegionWithVariance(-1);
                removedHigh.add(HighArea);
                labels[HighArea] = -1;
                tr.addArea(HighArea, minAttr.get(HighArea), maxAttr.get(HighArea), avgAttr.get(HighArea), varAttr.get(HighArea), sumAttr.get(HighArea), r);
                boolean feasible = false;
                boolean updated = true;
                while (!feasible && updated) {
                    updated = false;
                    Set<Integer> neighborSet = tr.getAreaNeighborSet();
                    //Collections.shuffle((List<?>) neighborSet);
                    List<Integer> neighborList = new ArrayList<>(neighborSet);
                    if(randFlag[0] == 1){
                        Collections.shuffle(neighborList);
                    }


                    if (tr.getAverage() < avgLowerBound) {
                        if(randFlag[0] == 2){
                            neighborList.sort((Integer area1, Integer area2) -> avgAttr.get(area2).compareTo(avgAttr.get(area1)));

                        }
                        for (Integer i : neighborList) {
                            if (labels[i] == 0 && avgAttr.get(i) > avgUpperBound && !removedHigh.contains(i)) {
                                tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), varAttr.get(i), sumAttr.get(i), r);
                                removedHigh.add(i);
                                labels[i] = -1;
                                updated = true;
                                break;
                            }
                        }
                    } else if (tr.getAverage() > avgUpperBound) {
                        if(randFlag[0] == 2){
                            neighborList.sort((Integer area1, Integer area2) -> avgAttr.get(area1).compareTo(avgAttr.get(area2)));
                        }
                        for (Integer i : neighborList) {
                            if (labels[i] == 0 && avgAttr.get(i) < avgLowerBound && !removedLow.contains(i)) {
                                tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), varAttr.get(i), sumAttr.get(i), r);
                                removedLow.add(i);
                                labels[i] = -1;
                                updated = true;
                                break;
                            }
                        }
                    } else {
                        feasible = true;
                        regionChange = true;
                        if(debug){System.out.println("Region Change");}
                        labels = tr.updateId(regionList.size() + 1, labels);
                        regionList.put(regionList.size() + 1, tr);
                    }


                }
                if (!feasible) {

                    for (Integer area : tr.getAreaList()) {
                        labels[area] = 0;
                        if (avgAttr.get(area) < avgLowerBound) {
                            removedLow.remove(area);
                        } else {
                            removedHigh.remove(area);
                        }
                    }
                }

            }
        }
        /*if(debug){
                System.out.println("Step 1:");
                System.out.println("UnassignedLow: " + unassignedLow.size());
                System.out.println("UnassignedHigh: " + unassignedHigh.size());
                System.out.println("Num of regions: " + regionList.size());
            }




            if(debug){
                System.out.println();
                System.out.println("Step 2:");
                System.out.println("UnassignedLow: " + unassignedLow.size());
                System.out.println("UnassignedHigh: " + unassignedHigh.size());
                System.out.println("Num of regions: " + regionList.size());
                System.out.print("Labels: ");
                for (int i = 0; i < labels.length; i++) {
                    System.out.print(labels[i] + " ");
                }
                System.out.println();
                for (Map.Entry<Integer, Region> e : regionList.entrySet()) {

                    System.out.println("Id after step2:" + e.getValue().getId());
                    System.out.println("Min:" + e.getValue().getMin());
                    System.out.println("Max:" + e.getValue().getMax());
                    System.out.println("Avg:" + e.getValue().getAverage());
                    System.out.println("Sum:" + e.getValue().getSum());

                    System.out.println("Count:" + e.getValue().getCount());
                    System.out.println("Satisfiable:" + e.getValue().satisfiable());

                }
            }*/
        List<Integer> infeasibleLow = new ArrayList<Integer>();
        List<Integer> infeasibleHigh = new ArrayList<Integer>();
        List<Integer> unassignedAverage = new ArrayList<Integer>();
        //labels[arr_index] > -1?
        for (int arr_index = 0; arr_index < avgAttr.size(); arr_index++) {
            if (seedAreas.contains(arr_index))
                continue;
            if (labels[arr_index] == 0 && avgAttr.get(arr_index) < avgLowerBound) {
                unassignedLow.add(arr_index);
            } else if (labels[arr_index] == 0 && avgAttr.get(arr_index) > avgUpperBound) {
                unassignedHigh.add(arr_index);
            } else if (labels[arr_index] == 0) {
                unassignedAverage.add(arr_index);
            }
        }
        Iterator<Integer> iteratorAvg = unassignedAverage.iterator();
        while (iteratorAvg.hasNext()) {
            Integer avgArea = iteratorAvg.next();
            List<Integer> neighborList = new ArrayList<>(r.getNeighbors(avgArea));
            if(randFlag[0] == 0){
                Collections.shuffle(neighborList);
            }

            for (Integer neighborArea : neighborList) {
                //if(labels[neighborArea] != 0){
                if (labels[neighborArea] > 0) {
                    regionList.get(labels[neighborArea]).addArea(avgArea, minAttr.get(avgArea), maxAttr.get(avgArea), avgAttr.get(avgArea), varAttr.get(avgArea), sumAttr.get(avgArea), r);
                    labels[avgArea] = regionList.get(labels[neighborArea]).getId();
                    iteratorAvg.remove();
                    break;
                }
            }
        }
        Iterator<Integer> iteratorLow = unassignedLow.iterator();
        while (iteratorLow.hasNext()) {
            Integer lowarea = iteratorLow.next();
            List<Integer> neighborList = new ArrayList<>(r.getNeighbors(lowarea));
            if(randFlag[0] <= 1){
                if(randFlag[0] == 1){
                    Collections.shuffle(neighborList);
                }

                for (Integer neighborArea : neighborList) {
                    if (labels[neighborArea] > 0 && regionList.get(labels[neighborArea]).getAcceptLow() <= avgAttr.get(lowarea)) {
                        if (debug) {
                            System.out.println("Add low area " + lowarea + " to region " + labels[neighborArea]);
                        }
                        regionList.get(labels[neighborArea]).addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), varAttr.get(lowarea), sumAttr.get(lowarea), r);
                        iteratorLow.remove();
                        labels[lowarea] = regionList.get(labels[neighborArea]).getId();
                        break;
                    }
                }
            }else{
                double minAcceptLow = Double.POSITIVE_INFINITY;
                int lowestRegion = 0;
                for (Integer neighborArea : neighborList) {
                    if(labels[neighborArea] > 0){
                        if(regionList.get(labels[neighborArea]).getAcceptHigh() < minAcceptLow){
                            minAcceptLow = regionList.get(labels[neighborArea]).getAcceptHigh();
                            lowestRegion = labels[neighborArea];
                        }

                    }
                }
                if(lowestRegion > 0){
                    regionList.get(lowestRegion).addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), varAttr.get(lowarea), sumAttr.get(lowarea), r);
                    iteratorLow.remove();
                    if (debug) {
                        System.out.println("Add low area " + lowarea + " to region " + labels[lowestRegion]);
                    }
                    labels[lowarea] = lowestRegion;
                }
            }



        }
        Iterator<Integer> iteratorHigh = unassignedHigh.iterator();
        while (iteratorHigh.hasNext()) {
            Integer higharea = iteratorHigh.next();
            List<Integer> neighborList = new ArrayList<>(r.getNeighbors(higharea));
            if(randFlag[0] <= 1){
                if(randFlag[0] == 1){
                    Collections.shuffle(neighborList);
                }

                for (Integer neighborArea : neighborList) {
                    if (labels[neighborArea] > 0 && regionList.get(labels[neighborArea]).getAcceptHigh() >= avgAttr.get(higharea)) {
                        if (debug) {
                            System.out.println("Add high area " + higharea + " to region " + labels[neighborArea]);
                        }
                        regionList.get(labels[neighborArea]).addArea(higharea, minAttr.get(higharea), maxAttr.get(higharea), avgAttr.get(higharea), varAttr.get(higharea), sumAttr.get(higharea), r);
                        iteratorHigh.remove();
                        labels[higharea] = regionList.get(labels[neighborArea]).getId();
                        break;
                    }
                }
            }else{
                double maxAcceptHigh = 0;
                int highestRegion = 0;
                for (Integer neighborArea : neighborList) {
                    if(labels[neighborArea] > 0){
                        if(regionList.get(labels[neighborArea]).getAcceptHigh() > maxAcceptHigh){
                            maxAcceptHigh = regionList.get(labels[neighborArea]).getAcceptHigh();
                            highestRegion = labels[neighborArea];
                        }

                    }
                }
                if(highestRegion > 0){
                    regionList.get(highestRegion).addArea(higharea, minAttr.get(higharea), maxAttr.get(higharea), avgAttr.get(higharea), varAttr.get(higharea), sumAttr.get(higharea), r);
                    iteratorHigh.remove();
                    if (debug) {
                        System.out.println("Add high area " + higharea + " to region " + labels[highestRegion]);
                    }
                    labels[higharea] = highestRegion;
                }
            }

        }
        boolean merged = true;

        while (merged) {
            merged = false;
            if(randFlag[0] == 1){
                Collections.shuffle(unassignedLow);
            }
            Iterator<Integer> interUnassignedLow = unassignedLow.iterator();
            while (interUnassignedLow.hasNext()) {
                Integer lowarea = interUnassignedLow.next();
                List<RegionWithVariance> tmpRegionList = new ArrayList<RegionWithVariance>();
                RegionWithVariance tryR = new RegionWithVariance(-1);
                Double lowestAcceptLow = Double.POSITIVE_INFINITY;
                //System.out.println(r.getNeighbors(lowarea));
                List<Integer> neighborList = new ArrayList<>(r.getNeighbors(lowarea));
                //Collections.shuffle(neighborList);
                for (Integer neighborArea : neighborList) {
                    //System.out.print(labels[neighborArea] + " ");
                    if (labels[neighborArea] > 0) {
                        RegionWithVariance tmpR = regionList.get(labels[neighborArea]);
                        //System.out.println("Safe");
                        //System.out.print("Error at " + neighborArea + " " + labels[neighborArea] + " ");
                        //System.out.println(tmpR.getId());
                        if (tmpR.getAcceptLow()
                                < lowestAcceptLow) {
                            tryR = regionList.get(labels[neighborArea]);
                            lowestAcceptLow = regionList.get(labels[neighborArea]).getAcceptLow();
                        }
                    } else {
                        continue;
                    }

                }
                if(lowestAcceptLow < avgAttr.get(lowarea) ){
                    tryR.addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), varAttr.get(lowarea), sumAttr.get(lowarea), r);
                    labels[lowarea] = tryR.getId();
                    labels = tryR.updateId(tryR.getId(), labels);
                    interUnassignedLow.remove();
                    if(debug)
                        System.out.println("Low Area: " + lowarea + " added to region " +tryR.getId() + " before merging.");
                    continue;

                }
                //System.out.println();
                        /*
                        if(tryR.getId() == -1){

                            System.out.println("Low Area: " + lowarea + " faled to find neighbor!");
                        }*/
                tmpRegionList.add(tryR);
                boolean feasible = false;
                int mergeCountLow = 0;
                while (!feasible && mergeCountLow < mergeLimit) {
                    mergeCountLow ++;
                    if(debug){
                        System.out.println("Number of unassignedLow at merge count " + mergeCountLow + " is: " + unassignedLow.size());
                    }
                    RegionWithVariance expandR = new RegionWithVariance(-1);

                    Double expandRacceptlowest = Double.POSITIVE_INFINITY;
                    //System.out.println("Neighbor Areas for " + lowarea + " " + tryR.getAreaNeighborSet());
                    List<Integer> tryRNeighborList = new ArrayList<>(tryR.getRegionNeighborSet(labels));
                    //Collections.shuffle(tryRNeighborList);
                    for (Integer lr : tryRNeighborList) {
                        //System.out.print(lr + " ");
                        if (lr > 0 && regionList.get(lr).getAcceptLow() < expandRacceptlowest && !tmpRegionList.contains(regionList.get(lr))) {
                            expandR = regionList.get(lr);
                            expandRacceptlowest = expandR.getAcceptLow();
                        }
                    }
                    //System.out.println();

                    if (expandR.getId() == -1) {
                        break;
                    }
                    //System.out.println("Try " + expandR.getId() + " for " + lowarea);
                    tmpRegionList.add(expandR);
                    tryR = tryR.mergeWith(expandR, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                    if (avgAttr.get(lowarea) >= tryR.getAcceptLow()) {
                        merged = true;
                        //System.out.println("Merged!");
                        tryR.addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), varAttr.get(lowarea), sumAttr.get(lowarea), r);
                        labels[lowarea] = expandR.getId();
                        labels = tryR.updateId(expandR.getId(), labels);
                        interUnassignedLow.remove();
                        for (RegionWithVariance tr : tmpRegionList) {

                            regionList.remove(tr.getId());

                        }
                        feasible = true;
                        regionList.put(tryR.getId(), tryR);
                        if(debug){
                            System.out.println("Number of remaining regions(Min): " + regionList.size());
                        }
                    }
                }
                if (!feasible) {
                    infeasibleLow.add(lowarea);
                    if(debug)
                        System.out.println("Low Area: " + lowarea + " fail to merge!");
                }
            }
        }
        if(randFlag[0] == 1){
            Collections.shuffle(unassignedHigh);
        }
        Iterator<Integer> iterUnassignedHigh = unassignedHigh.iterator();
        while (iterUnassignedHigh.hasNext()) {
            Integer highArea = iterUnassignedHigh.next();
            List<RegionWithVariance> tmpRegionList = new ArrayList<RegionWithVariance>();
            RegionWithVariance tryR = new RegionWithVariance(-1);
            Double highestAcceptHigh = -Double.POSITIVE_INFINITY;

            List<Integer> neighborList = new ArrayList<>(r.getNeighbors(highArea));
            //Collections.shuffle(neighborList);

            for (Integer neighborArea : neighborList) {

                if (labels[neighborArea] > 0 && regionList.get(labels[neighborArea]).getAcceptHigh() > highestAcceptHigh) {

                    tryR = regionList.get(labels[neighborArea]);
                    highestAcceptHigh = regionList.get(labels[neighborArea]).getAcceptHigh();
                }
            }
            if (tryR.getId() == -1) {
                if(debug)
                    System.out.println("HighArea: " + highArea + " faled to find neighbor!");
            }
            if(highestAcceptHigh < avgAttr.get(highArea) ){
                tryR.addArea(highArea, minAttr.get(highArea), maxAttr.get(highArea), avgAttr.get(highArea), varAttr.get(highArea), sumAttr.get(highArea), r);
                labels[highArea] = tryR.getId();
                labels = tryR.updateId(tryR.getId(), labels);
                iterUnassignedHigh.remove();
                if(debug)
                    System.out.println("High Area: " + highArea + " added to region " +tryR.getId() + " before merging.");
                continue;

            }
            tmpRegionList.add(tryR);
            int mergeCountHigh = 0;
            boolean feasible = false;
            while (!feasible && mergeCountHigh < mergeLimit) {
                mergeCountHigh++;
                if(debug){
                    System.out.println("Number of unassignedHigh at merge count " + mergeCountHigh + " is: " + unassignedHigh.size());
                }
                RegionWithVariance expandR = new RegionWithVariance(-1);
                Double expandRacceptHighest = -Double.POSITIVE_INFINITY;
                for (Integer lr : tryR.getRegionNeighborSet(labels)) {
                    if (lr > 0 && regionList.get(lr).getAcceptHigh() > expandRacceptHighest && !tmpRegionList.contains(regionList.get(lr))) {
                        expandR = regionList.get(lr);
                        expandRacceptHighest = expandR.getAcceptHigh();
                    }
                }
                if (expandR.getId() == -1) {
                    break;
                }
                tmpRegionList.add(expandR);
                tryR = tryR.mergeWith(expandR, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                if (avgAttr.get(highArea) <= tryR.getAcceptHigh()) {
                    tryR.addArea(highArea, minAttr.get(highArea), maxAttr.get(highArea), avgAttr.get(highArea), varAttr.get(highArea), sumAttr.get(highArea), r);
                    labels[highArea] = expandR.getId();
                    labels = tryR.updateId(expandR.getId(), labels);
                    iterUnassignedHigh.remove();
                    for (RegionWithVariance tr : tmpRegionList) {

                        regionList.remove(tr.getId());

                    }
                    feasible = true;
                    regionList.put(tryR.getId(), tryR);
                    if(debug){
                        System.out.println("Number of remaining regions(Max): " + regionList.size());
                    }
                }
            }
            if (!feasible) {
                infeasibleHigh.add(highArea);
                if(debug)
                    System.out.println("High Area: " + highArea + " fail to merge!");
            }
        }
        if(randFlag[0] >= 1){
            Collections.shuffle(unassignedAverage);
        }

        Iterator<Integer> iteratorAvg2 = unassignedAverage.iterator();
        while (iteratorAvg2.hasNext()) {
            Integer avgArea = iteratorAvg2.next();
            List<Integer> neighborList = new ArrayList<>(r.getNeighbors(avgArea));
            if(randFlag[0] >= 1){
                Collections.shuffle(neighborList);
            }
            for (Integer neighborArea : r.getNeighbors(avgArea)) {
                if (labels[neighborArea] > 0) {
                    regionList.get(labels[neighborArea]).addArea(avgArea, minAttr.get(avgArea), maxAttr.get(avgArea), avgAttr.get(avgArea), varAttr.get(avgArea), sumAttr.get(avgArea), r);
                    labels[avgArea] = regionList.get(labels[neighborArea]).getId();
                    iteratorAvg2.remove();
                    break;
                }
            }
        }
        if(debug){
            System.out.println();
            System.out.println("Step 3:");
            System.out.println("Infeasible low: " + infeasibleLow);
            System.out.println("Infeasible high: " + infeasibleHigh);
            System.out.println("Unassigned average: " + unassignedAverage);
            System.out.println("Number of regions after step 3: " + regionList.size());
        }

        if (debug) {
            for (Map.Entry<Integer, RegionWithVariance> e : regionList.entrySet()) {

                System.out.println("Id after step 3:" + e.getValue().getId());
                System.out.println("Min:" + e.getValue().getMin());
                System.out.println("Max:" + e.getValue().getMax());
                System.out.println("Avg:" + e.getValue().getAverage());
                System.out.println("Sum:" + e.getValue().getSum());

                System.out.println("Count:" + e.getValue().getCount());
                System.out.println("Areas: " + e.getValue().getAreaList());
                System.out.println("Satisfiable:" + e.getValue().satisfiable());

            }
        }

        //checkLabels(labels, regionList);
        //Map<Integer, RegionNew> regionList = rc.getRegionList();

        //Add one more step to resolve Min and Max (regions with single seed-area)
        List<Integer> notMin = new ArrayList<Integer>();
        List<Integer> notMax = new ArrayList<Integer>();
        for (Map.Entry<Integer, RegionWithVariance> regionEntry : regionList.entrySet()) {
            RegionWithVariance region = regionEntry.getValue();
            if(region.getMin() > minUpperBound){
                notMin.add(region.getId());
            }
            if(region.getMax() < maxLowerBound){
                notMax.add(region.getId());
            }
        }
        if(debug){
            System.out.println("Not min: " + notMin.size());
            System.out.println(notMin);
            for (Integer notMinId: notMin) {
                RegionWithVariance notMinRegion = regionList.get(notMinId);
                System.out.println("Not min ID:" + notMinRegion.getId());
                System.out.println("Min:" + notMinRegion.getMin());
                System.out.println("Max:" + notMinRegion.getMax());
                System.out.println("Avg:" + notMinRegion.getAverage());
                System.out.println("Sum:" + notMinRegion.getSum());

                System.out.println("Count:" + notMinRegion.getCount());
                System.out.println("Areas: " + notMinRegion.getAreaList());
                System.out.println("Satisfiable:" + notMinRegion.satisfiable());

            }
            System.out.println("Not max: " + notMax.size());
            System.out.println(notMax);
            for (Integer notMaxId: notMax) {
                RegionWithVariance notMaxRegion = regionList.get(notMaxId);
                System.out.println("Not max Id:" + notMaxRegion.getId());
                System.out.println("Min:" + notMaxRegion.getMin());
                System.out.println("Max:" + notMaxRegion.getMax());
                System.out.println("Avg:" + notMaxRegion.getAverage());
                System.out.println("Sum:" + notMaxRegion.getSum());

                System.out.println("Count:" + notMaxRegion.getCount());
                System.out.println("Areas: " + notMaxRegion.getAreaList());
                System.out.println("Satisfiable:" + notMaxRegion.satisfiable());

            }
        }

        boolean minMaxMerged = true;
        while(minMaxMerged){
            minMaxMerged = false;
            Iterator<Integer> notMinIterator = notMin.iterator();

            while(notMinIterator.hasNext()){
                Integer notMinRegion = notMinIterator.next();

                Set<Integer> neighborSet = regionList.get(notMinRegion).getRegionNeighborSet(labels);
                List<Integer>regionNeighbor = new ArrayList<>(neighborSet);

                //System.out.println(regionNeighbor.size());

                if(randFlag[0] >= 1){
                    Collections.shuffle(regionNeighbor);
                }

                for(Integer neighbor:regionNeighbor){
                    if(notMax.contains(neighbor)){
                        minMaxMerged = true;
                        RegionWithVariance mergedRegion = regionList.get(notMinRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                        mergedRegion.updateId(notMinRegion, labels);
                        regionList.remove(notMinRegion);
                        regionList.remove(neighbor);
                        regionList.put(notMinRegion, mergedRegion);
                        notMinIterator.remove();
                        notMax.remove(neighbor);
                        break;

                    }
                }
            }
            notMinIterator = notMin.iterator();
            while(notMinIterator.hasNext()){
                Integer notMinRegion = notMinIterator.next();

                Set<Integer> neighborSet = regionList.get(notMinRegion).getRegionNeighborSet(labels);
                List<Integer>regionNeighbor = new ArrayList<>(neighborSet);

                if(randFlag[0] >= 1){
                    Collections.shuffle(regionNeighbor);
                }

                for(Integer neighbor:regionNeighbor){
                    /*if(regionList.get(neighbor).satisfiable()){
                        System.out.println("Satisfiable");
                    }*/
                    if(!notMin.contains(neighbor)){
                        //System.out.println("Merge notMin and notMax");
                        minMaxMerged = true;
                        RegionWithVariance mergedRegion = regionList.get(notMinRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                        mergedRegion.updateId(neighbor, labels);
                        regionList.remove(notMinRegion);
                        regionList.remove(neighbor);
                        regionList.put(neighbor, mergedRegion);
                        notMinIterator.remove();
                        //notMax.remove(neighbor);
                        break;

                    }
                }
            }
            Iterator<Integer> notMaxIterator = notMax.iterator();
            while(notMaxIterator.hasNext()){
                Integer notMaxRegion = notMaxIterator.next();

                Set<Integer> neighborSet = regionList.get(notMaxRegion).getRegionNeighborSet(labels);
                List<Integer>regionNeighbor = new ArrayList<>(neighborSet);
                if(randFlag[0] >= 1){
                    Collections.shuffle(regionNeighbor);
                }
                for(Integer neighbor:regionNeighbor){
                    if(!notMax.contains(neighbor)){
                        minMaxMerged = true;
                        RegionWithVariance mergedRegion = regionList.get(notMaxRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                        mergedRegion.updateId(neighbor, labels);
                        regionList.remove(notMaxRegion);
                        regionList.remove(neighbor);
                        regionList.put(neighbor, mergedRegion);
                        notMaxIterator.remove();
                        //notMax.remove(neighbor);
                        break;

                    }
                }
            }
        }

        if(debug){
            System.out.println("Not min after merging Min Max: " + notMin.size());
            System.out.println(notMin);
            for (Integer notMinId: notMin) {
                RegionWithVariance notMinRegion = regionList.get(notMinId);
                System.out.println("Not min ID:" + notMinRegion.getId());
                System.out.println("Min:" + notMinRegion.getMin());
                System.out.println("Max:" + notMinRegion.getMax());
                System.out.println("Avg:" + notMinRegion.getAverage());
                System.out.println("Sum:" + notMinRegion.getSum());

                System.out.println("Count:" + notMinRegion.getCount());
                System.out.println("Areas: " + notMinRegion.getAreaList());
                System.out.println("Satisfiable:" + notMinRegion.satisfiable());

            }
            System.out.println("Not max after merging Min Max: " + notMax.size());
            System.out.println(notMax);
            for (Integer notMaxId: notMax) {
                RegionWithVariance notMaxRegion = regionList.get(notMaxId);
                System.out.println("Not max Id:" + notMaxRegion.getId());
                System.out.println("Min:" + notMaxRegion.getMin());
                System.out.println("Max:" + notMaxRegion.getMax());
                System.out.println("Avg:" + notMaxRegion.getAverage());
                System.out.println("Sum:" + notMaxRegion.getSum());

                System.out.println("Count:" + notMaxRegion.getCount());
                System.out.println("Areas: " + notMaxRegion.getAreaList());
                System.out.println("Satisfiable:" + notMaxRegion.satisfiable());

            }
        }
        Quartet<int[], Map<Integer, RegionWithVariance>, List<Integer>, List<Integer>> avgInitialization = new Quartet<>(labels, regionList, unassignedLow, unassignedHigh);
        return avgInitialization;
    }
    public static Pair<int[], Map<Integer, Region>> sumcount_construction(int[] labels,
                                                                          Map<Integer, Region> regionList,
                                                                          SpatialGrid r,
                                                                          ArrayList<Long> minAttr,
                                                                          ArrayList<Long> maxAttr,
                                                                          ArrayList<Long> avgAttr,
                                                                          ArrayList<Long> sumAttr,
                                                                          Double sumLowerBound,
                                                                          Double sumUpperBound,
                                                                          Double countLowerBound,
                                                                          Double countUpperBound){
        boolean updated = true;
        while (updated) {
            //checkLabels(labels, regionList);
            updated = false;
            List<Map.Entry<Integer, Region>> tmpList2 = new ArrayList<Map.Entry<Integer, Region>>(regionList.entrySet() );
            if(randFlag[1] >= 1){
                Collections.shuffle(tmpList2);
            }

            for (Map.Entry<Integer, Region> regionEntry : tmpList2) {
                Region region = regionEntry.getValue();
                if (region.getCount() < countLowerBound || region.getSum() < sumLowerBound) {
                    List<Integer> neighborList = new ArrayList<>(region.getAreaNeighborSet());
                    if(randFlag[1] <= 1){
                        if(randFlag[1] == 1){
                            Collections.shuffle(neighborList);
                        }

                        for (Integer area : neighborList) {
                            if (labels[area] > 0 && regionList.get(labels[area]).removable(area, minAttr, maxAttr, avgAttr, sumAttr, r) && region.acceptable(area, minAttr, maxAttr, avgAttr, sumAttr)) {
                                regionList.get(labels[area]).removeArea(area, minAttr, maxAttr, avgAttr, sumAttr, r);
                                region.addArea(area, minAttr.get(area), maxAttr.get(area), avgAttr.get(area), sumAttr.get(area), r);
                                labels[area] = region.getId();
                                updated = true;
                                break;
                            }
                        }
                    }else{
                        int maxIncArea = -1;
                        long maxSumInc = 0;
                        for (Integer area : neighborList) {
                            if (labels[area] > 0 && regionList.get(labels[area]).removable(area, minAttr, maxAttr, avgAttr, sumAttr, r) && region.acceptable(area, minAttr, maxAttr, avgAttr, sumAttr)) {
                                /*regionList.get(labels[area]).removeArea(area, minAttr, maxAttr, avgAttr, sumAttr, r);
                                region.addArea(area, minAttr.get(area), maxAttr.get(area), avgAttr.get(area), sumAttr.get(area), r);
                                labels[area] = region.getId();
                                updated = true;
                                break;*/
                                if(sumAttr.get(area) > maxSumInc){
                                    maxSumInc= sumAttr.get(area);
                                    maxIncArea = area;
                                }
                            }
                        }
                        if(maxIncArea > 0){
                            regionList.get(labels[maxIncArea]).removeArea(maxIncArea, minAttr, maxAttr, avgAttr, sumAttr, r);
                            region.addArea(maxIncArea, minAttr.get(maxIncArea), maxAttr.get(maxIncArea), avgAttr.get(maxIncArea), sumAttr.get(maxIncArea), r);
                            labels[maxIncArea] = region.getId();
                            updated = true;
                        }
                    }

                }
                if (region.getCount() > countUpperBound || region.getSum() > sumUpperBound) {
                    boolean removed = false;
                    //Iterator<Integer> it = region.areaList.iterator();
                    List<Integer> tmpList = new ArrayList<Integer>();
                    for (Integer area : region.getAreaList()) {
                        tmpList.add(area);
                    }
                    if(randFlag[1] == 1){
                        Collections.shuffle(tmpList);
                    }
                    if(randFlag[1] == 2){
                        tmpList.sort((Integer area1, Integer area2) -> sumAttr.get(area2).compareTo(sumAttr.get(area1)));
                    }

                    for (Integer area : tmpList) {

                        if (region.removable(area, minAttr, maxAttr, avgAttr, sumAttr, r)) {
                            List<Integer> neighborList = new ArrayList<>(r.getNeighbors(area));
                            for (Integer neighbor : neighborList) {
                                //存在未分配的area？- sumUpper, 一部分被移除的是0 -> 改成-2
                                if (labels[neighbor] > 0 && labels[neighbor] != labels[area] && regionList.get(labels[neighbor]).acceptable(area, minAttr, maxAttr, avgAttr, sumAttr)) {
                                    regionList.get(labels[neighbor]).addArea(area, minAttr.get(area), maxAttr.get(area), avgAttr.get(area), sumAttr.get(area), r);
                                    region.removeArea(area, minAttr, maxAttr, avgAttr, sumAttr, r);
                                    labels[area] = labels[neighbor];
                                    updated = true;
                                    removed = true;
                                    break;
                                }

                            }
                            if (!removed) {
                                region.removeArea(area, minAttr, maxAttr, avgAttr, sumAttr, r);
                                labels[area] = -4;
                                updated = true;
                                removed = true;
                            }
                        }
                    }
                }

            }
        }
        //checkLabels(labels, regionList);
        List<Integer> idToBeRemoved = new ArrayList<Integer>();
        for (Map.Entry<Integer, Region> regionEntry : regionList.entrySet()) {
            if (!regionEntry.getValue().satisfiable()) {
                //idToBeRemoved.add(regionEntry.getValue().getId());
                idToBeRemoved.add(regionEntry.getKey());
                if (debug) {


                    System.out.println("Id to be removed:" + regionEntry.getValue().getId());
                    System.out.println("Min:" + regionEntry.getValue().getMin());
                    System.out.println("Max:" + regionEntry.getValue().getMax());
                    System.out.println("Avg:" + regionEntry.getValue().getAverage());
                    System.out.println("Sum:" + regionEntry.getValue().getSum());

                    System.out.println("Count:" + regionEntry.getValue().getCount());
                    System.out.println("Areas: " + regionEntry.getValue().getAreaList());
                    System.out.println("Satisfiable:" + regionEntry.getValue().satisfiable());
                }
                //System.out.print(regionEntry.getKey() + " ");

            }
        }
        //System.out.println("Not satisfiable region size:" + idToBeRemoved.size());
        //System.out.println();
        List<Integer> idMerged = new ArrayList<Integer>();
        updated = true;
        //checkLabels(labels, regionList);
        if (debug) {
            System.out.println("P in the middle of merge: " + regionList.size());
            System.out.println(idToBeRemoved);
        }

        while (updated) {

            updated = false;
            for (Integer region : idToBeRemoved) {
                if (!idMerged.contains(region) && !regionList.get(region).satisfiable() && (regionList.get(region).getCount() < countLowerBound || regionList.get(region).getSum() < sumLowerBound)) {
                    List<Integer> regionMerged = new ArrayList<Integer>();
                    regionMerged.add(region);
                    //idMerged.add(region);//之前为啥注释掉了?idMerged表示因为merge消失的
                    List<Integer> neighborRegions = new ArrayList<>(regionList.get(region).getRegionNeighborSet(labels));

                    Region newRegion = regionList.get(region);
                    if(randFlag[1] >= 1){
                        Collections.shuffle(neighborRegions);
                    }


                    for (Integer neighborRegion : neighborRegions) {
                        if (neighborRegion <= 0) {
                            continue;
                        }
                        if(regionList.get(neighborRegion).getSum() >= sumLowerBound && regionList.get(neighborRegion).getCount() >= countLowerBound){
                            continue;
                        }
                        if (sumUpperBound - regionList.get(neighborRegion).getSum() < newRegion.getSum() || countUpperBound - regionList.get(neighborRegion).getCount() < newRegion.getCount()) {
                            continue;
                        }
                        regionMerged.add(neighborRegion);
                        idMerged.add(neighborRegion);
                        //System.out.println("Region to be merged: " + neighborRegion);
                        if (!regionList.containsKey(region)) {
                            System.out.println(neighborRegion + "does not exist in regionList");
                            System.exit(123);
                        }
                        newRegion = newRegion.mergeWith(regionList.get(neighborRegion), minAttr, maxAttr, avgAttr, sumAttr, r);
                        labels = newRegion.updateId(region, labels);
                        if (newRegion.satisfiable()) {
                            break;
                        }
                    }
                    // Whether a feasible region will be merged and the new region is not feasible and needs to be removed?
                    labels = newRegion.updateId(region, labels);
                    if (regionMerged.size() > 1)
                        updated = true;
                    for (Integer regionRemoved : regionMerged) {
                        regionList.remove(regionRemoved);
                        //System.out.print(regionRemoved + " ");
                    }
                    //System.out.println(region + " added to RegionList");

                    regionList.put(region, newRegion);
                    if (!regionList.containsKey(region)) {
                        System.out.println(region + "???");
                        System.exit(124);
                    }
                }
            }
        }
        //Assign Enclave:
        for (Integer region : idToBeRemoved) {
            if (!idMerged.contains(region) && !regionList.get(region).satisfiable() && (regionList.get(region).getCount() < countLowerBound || regionList.get(region).getSum() < sumLowerBound)) {
                List<Integer> regionMerged = new ArrayList<Integer>();
                regionMerged.add(region);

                List<Integer> neighborRegions = new ArrayList<>(regionList.get(region).getRegionNeighborSet(labels));

                Region newRegion = regionList.get(region);
                if(randFlag[1] >= 1){
                    Collections.shuffle(neighborRegions);
                }


                for (Integer neighborRegion : neighborRegions) {
                    if (neighborRegion <= 0) {
                        continue;
                    }

                    if (sumUpperBound - regionList.get(neighborRegion).getSum() < newRegion.getSum() || countUpperBound - regionList.get(neighborRegion).getCount() < newRegion.getCount()) {
                        continue;
                    }
                    regionMerged.add(neighborRegion);
                    idMerged.add(neighborRegion);
                    //System.out.println("Region to be merged: " + neighborRegion);
                    if (!regionList.containsKey(region)) {
                        System.out.println(neighborRegion + "does not exist in regionList");
                        System.exit(123);
                    }
                    newRegion = newRegion.mergeWith(regionList.get(neighborRegion), minAttr, maxAttr, avgAttr, sumAttr, r);
                    labels = newRegion.updateId(region, labels);
                    if (newRegion.satisfiable()) {
                        break;
                    }
                }
                // Whether a feasible region will be merged and the new region is not feasible and needs to be removed?
                labels = newRegion.updateId(region, labels);
                if (regionMerged.size() > 1)
                    updated = true;
                for (Integer regionRemoved : regionMerged) {
                    regionList.remove(regionRemoved);
                    //System.out.print(regionRemoved + " ");
                }
                //System.out.println(region + " added to RegionList");

                regionList.put(region, newRegion);
                if (!regionList.containsKey(region)) {
                    System.out.println(region + "???");
                    System.exit(124);
                }
            }
        }



        if (debug) {
            System.out.println("P returned after merge: " + regionList.size());
            for (int i = 0; i < labels.length; i++) {
                System.out.print(labels[i] + " ");
            }
            for (Map.Entry<Integer, Region> e : regionList.entrySet()) {
                System.out.println();
                System.out.println("Id:" + e.getValue().getId());
                System.out.println("Min:" + e.getValue().getMin());
                System.out.println("Max:" + e.getValue().getMax());
                System.out.println("Avg:" + e.getValue().getAverage());
                System.out.println("Sum:" + e.getValue().getSum());

                System.out.println("Count:" + e.getValue().getCount());
                System.out.println("Areas: " + e.getValue().getAreaList());
                System.out.println("Satisfiable:" + e.getValue().satisfiable());

            }
        }
        if (debug) {
            System.out.println("IdToBeRemoved: " + idToBeRemoved);
            System.out.println("IdMerged: " + idMerged);
            System.out.println("P returned before removal: " + regionList.size());
        }

        for (Integer id : idToBeRemoved) {
            if (!idMerged.contains(id) && !regionList.get(id).satisfiable()) {
                regionList.get(id).updateId(-3, labels);
                regionList.remove(id);
            }


        }
        if(debug){
            System.out.println("P returned after removal: " + regionList.size());
            System.out.println("-------------------------------------------------------------------------------");
        }
        Pair<int[], Map<Integer, Region>> sumcount_result = new Pair<>(labels, regionList);
        return  sumcount_result;
    }
    public static Pair<int[], Map<Integer, RegionWithVariance>> sumcount_construction_var(int[] labels,
                                                                                          Map<Integer, RegionWithVariance> regionList,
                                                                                          SpatialGrid r,
                                                                                          ArrayList<Long> minAttr,
                                                                                          ArrayList<Long> maxAttr,
                                                                                          ArrayList<Long> avgAttr,
                                                                                          ArrayList<Long> varAttr,
                                                                                          ArrayList<Long> sumAttr,
                                                                                          Double sumLowerBound,
                                                                                          Double sumUpperBound,
                                                                                          Double countLowerBound,
                                                                                          Double countUpperBound){
        boolean updated = true;
        if(var_debug){
            System.out.println("No. of regions before sum " + regionList.size());
            checkLabels_var(labels, regionList);
        }

        while (updated) {
            //checkLabels(labels, regionList);
            updated = false;
            List<Map.Entry<Integer, RegionWithVariance>> tmpList2 = new ArrayList<Map.Entry<Integer, RegionWithVariance>>(regionList.entrySet());
            if(randFlag[1] >= 1){
                Collections.shuffle(tmpList2);
            }

            for (Map.Entry<Integer, RegionWithVariance> regionEntry : tmpList2) {
                RegionWithVariance region = regionEntry.getValue();
                if (region.getCount() < countLowerBound || region.getSum() < sumLowerBound) {
                    if(var_debug){
                        System.out.println("Region " + region.getId() + " under count or sum" );
                    }
                    int worseTolerance = toleranceThreshold;
                    List<Integer> tolerantAreas = new ArrayList<Integer>();
                    List<Integer> tolerantRegions = new ArrayList<Integer>();
                    boolean regionVarSatisfiable = true;
                    List<Integer> neighborList = new ArrayList<>(region.getAreaNeighborSet());
                    if(randFlag[1] <= 1){
                        if(var_debug){
                            System.out.println("Rand flag <= 1" );
                        }
                        if(randFlag[1] == 1){
                            Collections.shuffle(neighborList);
                        }

                        for (Integer area : neighborList) {
                            if(var_debug){
                                System.out.println("Try out area " + area );
                                /*checkLabels_var(labels, regionList);
                                if(labels[area] > 0)
                                    System.out.println(regionList.get(labels[area]).removable(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r) );
                                else{
                                    System.out.println("Area unassigned " + labels[area] );
                                }*/
                            }
                            if (labels[area] > 0 && regionList.get(labels[area]).removable(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r) && region.acceptable(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr) ) {
                                if(var_debug){
                                    System.out.println("Try to move area " + area + " from region " + labels[area] + " to " + region.getId());
                                }
                                if(!region.acceptable(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr))
                                    worseTolerance--;
                                regionList.get(labels[area]).removeArea(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                                region.addArea(area, minAttr.get(area), maxAttr.get(area), avgAttr.get(area), varAttr.get(area), sumAttr.get(area), r);
                                labels[area] = region.getId();
                                updated = true;
                                regionVarSatisfiable = true;
                                break;
                            }else if(labels[area] > 0 && regionList.get(labels[area]).removable(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r) && (region.acceptable_withoutVar(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr) && worseTolerance > 0)){
                                if(var_debug){
                                    System.out.println("Tolerant: Try to move area " + area + " from region " + labels[area] + " to " + region.getId());
                                }
                                worseTolerance--;
                                tolerantAreas.add(area);
                                tolerantRegions.add(labels[area]);
                                regionList.get(labels[area]).removeArea(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                                region.addArea(area, minAttr.get(area), maxAttr.get(area), avgAttr.get(area), varAttr.get(area), sumAttr.get(area), r);
                                labels[area] = region.getId();
                                regionVarSatisfiable = false;

                                //updated = true;
                            }
                        }
                        if(!regionVarSatisfiable){//Var still valiated within tolerance, reverse the area swap
                            for(int i =  tolerantAreas.size() -1; i >= 0; i--){
                                int area = tolerantAreas.get(i);
                                if(var_debug)
                                    System.out.println("Tolerance: move area " + area + " back to region " +  tolerantRegions.get(i));
                                labels[area] = tolerantRegions.get(i);
                                region.removeArea(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                                regionList.get(tolerantRegions.get(i)).addArea(area, minAttr.get(area), maxAttr.get(area), avgAttr.get(area), varAttr.get(area), sumAttr.get(area), r);
                            }
                        }
                    }else{
                        int maxIncArea = -1;
                        long maxSumInc = 0;
                        for (Integer area : neighborList) {
                            if (labels[area] > 0 && regionList.get(labels[area]).removable(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r) && region.acceptable(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr)) {
                                if(sumAttr.get(area) > maxSumInc){
                                    maxSumInc= sumAttr.get(area);
                                    maxIncArea = area;
                                }
                            }
                        }
                        if(maxIncArea > 0){
                            regionList.get(labels[maxIncArea]).removeArea(maxIncArea, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                            region.addArea(maxIncArea, minAttr.get(maxIncArea), maxAttr.get(maxIncArea), avgAttr.get(maxIncArea), varAttr.get(maxIncArea), sumAttr.get(maxIncArea), r);
                            labels[maxIncArea] = region.getId();
                            updated = true;
                        }
                    }

                }
                if (region.getCount() > countUpperBound || region.getSum() > sumUpperBound) {
                    boolean removed = false;
                    //Iterator<Integer> it = region.areaList.iterator();
                    List<Integer> tmpList = new ArrayList<Integer>();
                    for (Integer area : region.getAreaList()) {
                        tmpList.add(area);
                    }
                    if(randFlag[1] == 1){
                        Collections.shuffle(tmpList);
                    }
                    if(randFlag[1] == 2){
                        tmpList.sort((Integer area1, Integer area2) -> sumAttr.get(area2).compareTo(sumAttr.get(area1)));
                    }

                    for (Integer area : tmpList) {

                        if (region.removable(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r)) {
                            List<Integer> neighborList = new ArrayList<>(r.getNeighbors(area));
                            for (Integer neighbor : neighborList) {
                                //存在未分配的area？- sumUpper, 一部分被移除的是0 -> 改成-2
                                if (labels[neighbor] > 0 && labels[neighbor] != labels[area] && regionList.get(labels[neighbor]).acceptable(area, minAttr, maxAttr, avgAttr,varAttr, sumAttr)) {
                                    regionList.get(labels[neighbor]).addArea(area, minAttr.get(area), maxAttr.get(area), avgAttr.get(area), varAttr.get(area), sumAttr.get(area), r);
                                    region.removeArea(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                                    labels[area] = labels[neighbor];
                                    updated = true;
                                    removed = true;
                                    break;
                                }

                            }
                            if (!removed) {
                                region.removeArea(area, minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                                labels[area] = -4;
                                updated = true;
                                removed = true;
                            }
                        }
                    }
                }

            }
        }
        //checkLabels(labels, regionList);
        List<Integer> idToBeRemoved = new ArrayList<Integer>();
        if(var_debug){

        }
        for (Map.Entry<Integer, RegionWithVariance> regionEntry : regionList.entrySet()) {
            if (!regionEntry.getValue().satisfiable()) {
                //idToBeRemoved.add(regionEntry.getValue().getId());
                idToBeRemoved.add(regionEntry.getKey());
                if (var_debug) {


                    System.out.println("Id to be removed:" + regionEntry.getValue().getId());
                    System.out.println("Min:" + regionEntry.getValue().getMin());
                    System.out.println("Max:" + regionEntry.getValue().getMax());
                    System.out.println("Avg:" + regionEntry.getValue().getAverage());
                    System.out.println("Var:" + regionEntry.getValue().getVariance());

                    System.out.println("Sum:" + regionEntry.getValue().getSum());

                    System.out.println("Count:" + regionEntry.getValue().getCount());
                    System.out.println("Areas: " + regionEntry.getValue().getAreaList());
                    System.out.println("Satisfiable:" + regionEntry.getValue().satisfiable());
                }
                //System.out.print(regionEntry.getKey() + " ");

            }
        }
        //System.out.println("Not satisfiable region size:" + idToBeRemoved.size());
        //System.out.println();
        List<Integer> idMerged = new ArrayList<Integer>();
        updated = true;
        //checkLabels(labels, regionList);
        if (var_debug) {
            System.out.println("P in the middle of merge: " + regionList.size());
            System.out.println(idToBeRemoved);
            checkLabels_var(labels, regionList);
        }

        while (updated) {

            updated = false;
            for (Integer region : idToBeRemoved) {
                if (!idMerged.contains(region) && !regionList.get(region).satisfiable() && (regionList.get(region).getCount() < countLowerBound || regionList.get(region).getSum() < sumLowerBound)) {
                    List<Integer> regionMerged = new ArrayList<Integer>();
                    regionMerged.add(region);
                    //idMerged.add(region);//之前为啥注释掉了?idMerged表示因为merge消失的
                    List<Integer> neighborRegions = new ArrayList<>(regionList.get(region).getRegionNeighborSet(labels));

                    RegionWithVariance newRegion = regionList.get(region);
                    if(randFlag[1] >= 1){
                        Collections.shuffle(neighborRegions);
                    }
                    RegionWithVariance sourceRegion = regionList.get(region);
                    double mergedVarSum = sourceRegion.getVarianceSum();
                    double mergedVarSumSquare = sourceRegion.getVarianceSumSquare();
                    int mergedCount = sourceRegion.getCount();
                    int worseTolerant = toleranceThreshold;
                    for (Integer neighborRegion : neighborRegions) {
                        if(idMerged.contains(neighborRegion)){ //To prevent a merged region to be accessed again
                            continue;
                        }
                        if(var_debug){
                            System.out.println("Neighbor region: " + neighborRegion);
                        }
                        if (neighborRegion <= 0) {
                            continue;
                        }
                        if(regionList.get(neighborRegion).getSum() >= sumLowerBound && regionList.get(neighborRegion).getCount() >= countLowerBound){
                            continue;
                        }
                        if (sumUpperBound - regionList.get(neighborRegion).getSum() < newRegion.getSum() || countUpperBound - regionList.get(neighborRegion).getCount() < newRegion.getCount()) {
                            continue;
                        }
                        RegionWithVariance nRegion = regionList.get(neighborRegion);
                        mergedVarSum+= nRegion.getVarianceSum();
                        mergedVarSumSquare += nRegion.getVarianceSumSquare();
                        mergedCount += nRegion.getCount();
                        double mergedVar = RegionWithVariance.simpleVariance(mergedCount, mergedVarSum, mergedVarSumSquare);
                        if(worseTolerant > 0){
                            if(!(mergedVar >= varLowerbound && mergedVar <= varUpprbound)){
                                worseTolerant -= 1;
                            }
                        }else if(mergedVar >= varLowerbound && mergedVar <= varUpprbound){
                            worseTolerant = toleranceThreshold;
                        }else{
                            continue;
                        }


                        regionMerged.add(neighborRegion);
                        idMerged.add(neighborRegion);
                        if(var_debug)
                            System.out.println("Region to be merged: " + neighborRegion);
                        if (!regionList.containsKey(region)) {
                            System.out.println(neighborRegion + "does not exist in regionList");
                            System.exit(123);
                        }
                        newRegion = newRegion.mergeWith(regionList.get(neighborRegion), minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                        //labels = newRegion.updateId(region, labels);
                        if(var_debug)
                            System.out.println("Sum after merge " + newRegion.getSum());
                        if (newRegion.satisfiable()) {

                            break;
                        }
                    }
                    // Whether a feasible region will be merged and the new region is not feasible and needs to be removed?
                    if(!(newRegion.getVariance() >= varLowerbound && newRegion.getVariance() <= varUpprbound)){
                        idMerged.removeAll(regionMerged);
                        continue;
                    }

                    labels = newRegion.updateId(region, labels);
                    if (regionMerged.size() > 1)
                        updated = true;
                    for (Integer regionRemoved : regionMerged) {
                        regionList.remove(regionRemoved);
                        //System.out.print(regionRemoved + " ");
                    }
                    //System.out.println(region + " added to RegionList");

                    regionList.put(region, newRegion);
                    if (!regionList.containsKey(region)) {
                        System.out.println(region + "???");
                        System.exit(124);
                    }
                }
            }
        }
        //Assign Enclave:
        for (Integer region : idToBeRemoved) {
            if (!idMerged.contains(region) && !regionList.get(region).satisfiable() && (regionList.get(region).getCount() < countLowerBound || regionList.get(region).getSum() < sumLowerBound)) {
                List<Integer> regionMerged = new ArrayList<Integer>();
                regionMerged.add(region);

                List<Integer> neighborRegions = new ArrayList<>(regionList.get(region).getRegionNeighborSet(labels));

                RegionWithVariance newRegion = regionList.get(region);
                if(randFlag[1] >= 1){
                    Collections.shuffle(neighborRegions);
                }


                for (Integer neighborRegion : neighborRegions) {
                    if(idMerged.contains(neighborRegion))continue;
                    if (neighborRegion <= 0) {
                        continue;
                    }

                    if (sumUpperBound - regionList.get(neighborRegion).getSum() < newRegion.getSum() || countUpperBound - regionList.get(neighborRegion).getCount() < newRegion.getCount()) {
                        continue;
                    }
                    regionMerged.add(neighborRegion);
                    idMerged.add(neighborRegion);
                    //System.out.println("Region to be merged: " + neighborRegion);
                    if (!regionList.containsKey(region)) {
                        System.out.println(neighborRegion + "does not exist in regionList");
                        System.exit(123);
                    }
                    newRegion = newRegion.mergeWith(regionList.get(neighborRegion), minAttr, maxAttr, avgAttr, varAttr, sumAttr, r);
                    labels = newRegion.updateId(region, labels);
                    if (newRegion.satisfiable()) {
                        break;
                    }
                }
                // Whether a feasible region will be merged and the new region is not feasible and needs to be removed?
                labels = newRegion.updateId(region, labels);
                if (regionMerged.size() > 1)
                    updated = true;
                for (Integer regionRemoved : regionMerged) {
                    regionList.remove(regionRemoved);
                    //System.out.print(regionRemoved + " ");
                }
                //System.out.println(region + " added to RegionList");

                regionList.put(region, newRegion);
                if (!regionList.containsKey(region)) {
                    System.out.println(region + "???");
                    System.exit(124);
                }
            }
        }



        if (debug) {
            System.out.println("P returned after merge: " + regionList.size());
            for (int i = 0; i < labels.length; i++) {
                System.out.print(labels[i] + " ");
            }
            for (Map.Entry<Integer, RegionWithVariance> e : regionList.entrySet()) {
                System.out.println();
                System.out.println("Id:" + e.getValue().getId());
                System.out.println("Min:" + e.getValue().getMin());
                System.out.println("Max:" + e.getValue().getMax());
                System.out.println("Avg:" + e.getValue().getAverage());
                System.out.println("Sum:" + e.getValue().getSum());

                System.out.println("Count:" + e.getValue().getCount());
                System.out.println("Areas: " + e.getValue().getAreaList());
                System.out.println("Satisfiable:" + e.getValue().satisfiable());

            }
        }
        if (debug) {
            System.out.println("IdToBeRemoved: " + idToBeRemoved);
            System.out.println("IdMerged: " + idMerged);
            System.out.println("P returned before removal: " + regionList.size());
        }

        if(varForSum){
            System.out.println("IdToBeRemoved: " + idToBeRemoved);
            System.out.println("IdMerged: " + idMerged);
            System.out.println("P returned before removal: " + regionList.size());
        }

        for (Integer id : idToBeRemoved) {
            if (!idMerged.contains(id) && !regionList.get(id).satisfiable()) {
                regionList.get(id).updateId(-3, labels);
                regionList.remove(id);
            }


        }
        if(debug){
            System.out.println("P returned after removal: " + regionList.size());
            System.out.println("-------------------------------------------------------------------------------");
        }
        if(varForSum){
            boolean satisfySum = true;
            int notSumCount = 0;
            for(Map.Entry<Integer, RegionWithVariance> e: regionList.entrySet()){

                if(e.getValue().getSum() < sumLowerBound){
                    notSumCount += 1;
                    System.out.println("Region " + e.getKey() + " " + (e.getValue().getSum()));
                    satisfySum = false;
                }
            }
            System.out.println("Sum low check for " + regionList.size() + " regions: " + satisfySum + " " + notSumCount);
        }
        Pair<int[], Map<Integer, RegionWithVariance>> sumcount_result = new Pair<>(labels, regionList);

        return  sumcount_result;
    }




    public static RegionCollectionWithVariance construction_phase_breakdown_minmaxNoRepeat_variance(ArrayList<Integer> idList,
                                                                                                    ArrayList<Long> disAttr,
                                                                                                    SpatialGrid r,
                                                                                                    ArrayList<Long> minAttr,
                                                                                                    Double minLowerBound,
                                                                                                    Double minUpperBound,

                                                                                                    ArrayList<Long> maxAttr,
                                                                                                    Double maxLowerBound,
                                                                                                    Double maxUpperBound,

                                                                                                    ArrayList<Long> avgAttr,
                                                                                                    Double avgLowerBound,
                                                                                                    Double avgUpperBound,

                                                                                                    ArrayList<Long> varAttr,
                                                                                                    Double varLowerBound,
                                                                                                    Double varUpperBound,

                                                                                                    ArrayList<Long> sumAttr,
                                                                                                    Double sumLowerBound,
                                                                                                    Double sumUpperBound,

                                                                                                    Double countLowerBound,
                                                                                                    Double countUpperBound, boolean repeatQuery, String recordName) {
        int maxIt = 100;
        RegionWithVariance.setRange(minLowerBound, minUpperBound, maxLowerBound, maxUpperBound, avgLowerBound, avgUpperBound, varLowerBound, varUpperBound, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);



        //boolean var_debug = true;

        //RegionWithVariance.setRange(minLowerBound, minUpperBound, maxLowerBound, maxUpperBound, avgLowerBound, avgUpperBound, varianceLoweBound, varianceUpperBound, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
        ObjectInputStream ois = null;
        boolean differentConstraint = false;
        int stateOfChange = 0;
        if(repeatQuery){
            try{
                ois = new ObjectInputStream(new FileInputStream(recordName + "/constraints.txt"));
                ArrayList<Long> preMinAttr = (ArrayList<Long>) ois.readObject();
                Double preMinLowerBound = (Double) ois.readObject();
                Double preMinUpperBound = (Double) ois.readObject();
                ArrayList<Long> preMaxAttr = (ArrayList<Long>) ois.readObject();
                Double preMaxLowerBound = (Double) ois.readObject();
                Double preMaxUpperBound = (Double) ois.readObject();
                ArrayList<Long> preAvgAttr = (ArrayList<Long>) ois.readObject();
                Double preAvgLowerBound = (Double) ois.readObject();
                Double preAvgUpperBound = (Double) ois.readObject();
                ArrayList<Long> preVarAttr = (ArrayList<Long>) ois.readObject();
                Double preVarLowerBound = (Double) ois.readObject();
                Double preVarUpperBound = (Double) ois.readObject();
                ArrayList<Long> preSumAttr = (ArrayList<Long>) ois.readObject();
                Double preSumLowerBound = (Double) ois.readObject();
                Double preSumUpperBound = (Double) ois.readObject();
                Double preCountLowerBound = (Double) ois.readObject();
                Double preCountUpperBound = (Double) ois.readObject();
                if(preMinAttr.equals(minAttr) && preMaxAttr.equals(maxAttr) && preMinLowerBound.equals(minLowerBound) && preMinUpperBound.equals(minUpperBound) && preMaxLowerBound .equals( maxLowerBound) && preMaxUpperBound .equals( maxUpperBound)){
                    stateOfChange = 1;
                }else{
                    System.out.println("Different MIN/MAX");
                    differentConstraint = true;
                }
                if(!differentConstraint){
                    if(preVarAttr.equals(varAttr) && preVarLowerBound.equals(varLowerBound) && preVarUpperBound.equals(varUpperBound)){
                        stateOfChange = 2;
                    }else{
                        System.out.println("Different VAR");
                        differentConstraint = true;
                    }
                }
                if(!differentConstraint){
                    if(preAvgAttr.equals(avgAttr) && preAvgLowerBound.equals(avgLowerBound) && preAvgUpperBound.equals(avgUpperBound)){
                        stateOfChange = 2;
                    }else{
                        System.out.println("Different AVG");
                        differentConstraint = true;
                    }
                }
                if(!differentConstraint){
                    if(preSumAttr.equals(sumAttr) && preSumUpperBound.equals(sumUpperBound) && preSumLowerBound.equals(sumLowerBound) &&  preCountUpperBound.equals(countUpperBound) && preCountLowerBound.equals(countLowerBound)){
                        stateOfChange = 3;
                    }else{
                        System.out.println("Different SUM/COUNT");
                        differentConstraint = true;
                    }
                }


            }catch(EOFException e){//Previous result not found
                e.printStackTrace();
                differentConstraint = true;
                stateOfChange = 0;

            }catch(FileNotFoundException e) {
                //e.printStackTrace();
                differentConstraint = true;
                stateOfChange = 0;
            }catch(Exception e){

                e.printStackTrace();
            }
        }
        //if(var_debug)
        System.out.println("Different constraint check: " + differentConstraint + " " + stateOfChange);
        ObjectOutputStream oos = null;
        if(repeatQuery && differentConstraint){

            try{
                File recordFolder = new File(recordName);
                if(!recordFolder.exists()){
                    recordFolder.mkdirs();
                }

                oos = new ObjectOutputStream(new FileOutputStream(recordName + "/constraints.txt"));
                oos.writeObject(minAttr);
                oos.writeObject(minLowerBound);
                oos.writeObject(minUpperBound);
                oos.writeObject(maxAttr);
                oos.writeObject(maxLowerBound);
                oos.writeObject(maxUpperBound);
                oos.writeObject(avgAttr);
                oos.writeObject(avgLowerBound);
                oos.writeObject(avgUpperBound);
                oos.writeObject(varAttr);
                oos.writeObject(varLowerBound);
                oos.writeObject(varUpperBound);
                oos.writeObject(sumAttr);
                oos.writeObject(sumLowerBound);
                oos.writeObject(sumUpperBound);
                oos.writeObject(countLowerBound);
                oos.writeObject(countUpperBound);

            }catch(Exception e){
                e.printStackTrace();
            }
        }
        int max_p = 0;

        RegionCollectionWithVariance bestCollection = null;
        double minStart = System.currentTimeMillis() / 1000.0;

        ArrayList<Integer> areas = new ArrayList<Integer>();
        areas.addAll(idList);
        int min_unAssigned = maxAttr.size();
        //First step: Filtering and seeding for MIN, MAX.
        Pair<int[], ArrayList<Integer>> minmaxResult = null;
        if(repeatQuery && (!differentConstraint || stateOfChange > 0)){
            try{
                ois = new ObjectInputStream(new FileInputStream(recordName + "/minmax.txt"));
                minmaxResult = (Pair<int[], ArrayList<Integer>>) ois.readObject();
                //minmaxseedAreas = (ArrayList<Integer>)ois.readObject();
            }catch(Exception e){
                e.printStackTrace();
            }

        }else{
            minmaxResult = filtering_and_seeding(areas, minAttr, minLowerBound, minUpperBound, maxAttr, maxLowerBound, maxUpperBound, sumAttr, sumUpperBound);
            if(repeatQuery){
                try{
                    oos = new ObjectOutputStream(new FileOutputStream(recordName + "/minmax.txt", true));
                    oos.writeObject(minmaxResult);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }


        }
        int[] minmaxlabels = minmaxResult.getValue0();
        ArrayList<Integer> minmaxseedAreas = minmaxResult.getValue1();


        double minEnd = System.currentTimeMillis() / 1000.0;
        minTime = minTime + (minEnd - minStart);

        for (int it = 0; it < maxIt; it++) {

            int[] labels = minmaxlabels.clone();
            ArrayList<Integer> seedAreas = new ArrayList<>(minmaxseedAreas);
            if(var_debug)
                System.out.println("Before initialiazation");
            Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> varInitialization = null;
            if(repeatQuery &&(!differentConstraint || stateOfChange > 1)){
                try{
                    ois = new ObjectInputStream(new FileInputStream(recordName + "/var.txt"));
                    varInitialization = ( Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>>)ois.readObject();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }else{
                varInitialization = region_initialization_var(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, varAttr, varLowerBound, varUpperBound, sumAttr);
                try{
                    oos = new ObjectOutputStream(new FileOutputStream(recordName + "/var.txt", true));
                    oos.writeObject(varInitialization);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            //Second step: AVG and MIN MAX revisit
            //Quartet<int[], Map<Integer, Region>, List<Integer>, List<Integer>> avgInitialization = region_initialization(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, avgLowerBound, avgUpperBound, sumAttr);
            //Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> varInitialization = region_initialization_var(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, varAttr, varLowerBound, varUpperBound, sumAttr);
            if(var_debug)
                System.out.println("After initialization");
            labels = varInitialization.getValue0();
            Map<Integer, RegionWithVariance> regionList = varInitialization.getValue1();
            if(debug){
                checkLabels_var(labels, regionList);
            }
            double avgEnd = System.currentTimeMillis() / 1000.0;
            avgTime += (avgEnd - minEnd);
            //Start sum and count
            if (var_debug) {
                System.out.println("-------------");
                System.out.println("P after AVG: " + regionList.size());
                int unassignedCount = 0;
                for(int i = 0; i < labels.length; i++){
                    System.out.print(labels[i] + " ");
                    if(labels[i] < 1){
                        unassignedCount++;
                    }
                }

                System.out.println("No of unassigned areas after var" + unassignedCount);
                System.out.println("-------------");

            }
            /*if (var_debug) {
                for (Map.Entry<Integer, RegionWithVariance> regionEntry : regionList.entrySet()) {
                    // if (!regionEntry.getValue().satisfiable()) {
                    //idToBeRemoved.add(regionEntry.getValue().getId());
                    //regionList_MaxP_AVG.add(regionEntry.getKey());



                    System.out.println("Id to be removed:" + regionEntry.getValue().getId());
                    System.out.println("Min:" + regionEntry.getValue().getMin());
                    System.out.println("Max:" + regionEntry.getValue().getMax());
                    System.out.println("Avg:" + regionEntry.getValue().getAverage());
                    System.out.println("Var:" +  regionEntry.getValue().getVariance());
                    System.out.println("Sum:" + regionEntry.getValue().getSum());

                    System.out.println("Count:" + regionEntry.getValue().getCount());
                    System.out.println("Areas: " + regionEntry.getValue().getAreaList());
                    System.out.println("Satisfiable:" + regionEntry.getValue().satisfiable());
                }
                //System.out.print(regionEntry.getKey() + " ");

            }*/
            Pair<int[], Map<Integer, RegionWithVariance>> result = null;
            if(repeatQuery &&(!differentConstraint || stateOfChange > 2)){
                try{
                    ois = new ObjectInputStream(new FileInputStream(recordName + "/sumcount.txt"));
                    result = ( Pair<int[], Map<Integer, RegionWithVariance>>)ois.readObject();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }else{
                result = sumcount_construction_var(labels, regionList, r, minAttr, maxAttr, avgAttr, varAttr, sumAttr, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
                try{
                    ObjectOutputStream sumOos = new ObjectOutputStream(new FileOutputStream(recordName + "/sumcount.txt", true));
                    sumOos.writeObject(result);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            //Pair<int[], Map<Integer, RegionWithVariance>> result = sumcount_construction_var(labels, regionList, r, minAttr, maxAttr, avgAttr, varAttr, sumAttr, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
            labels = result.getValue0();
            regionList = result.getValue1();
            double sumEnd = System.currentTimeMillis() / 1000.0;
            sumTime += (sumEnd - avgEnd);
            //System.out.println("Finish SUM");
            /*if(repeatQuery){
                ObjectOutputStream oos = null;
                try{
                    oos = new ObjectOutputStream(new FileOutputStream("./prevResults.txt"));
                    oos.writeObject(recordName);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }*/
            if (var_debug) {
                for (Map.Entry<Integer, RegionWithVariance> regionEntry : regionList.entrySet()) {
                    // if (!regionEntry.getValue().satisfiable()) {
                    //idToBeRemoved.add(regionEntry.getValue().getId());
                    //regionList_MaxP_AVG.add(regionEntry.getKey());



                    System.out.println("Id to be removed:" + regionEntry.getValue().getId());
                    System.out.println("Min:" + regionEntry.getValue().getMin());
                    System.out.println("Max:" + regionEntry.getValue().getMax());
                    System.out.println("Avg:" + regionEntry.getValue().getAverage());
                    System.out.println("Var:" +  regionEntry.getValue().getVariance());
                    System.out.println("Sum:" + regionEntry.getValue().getSum());

                    System.out.println("Count:" + regionEntry.getValue().getCount());
                    System.out.println("Areas: " + regionEntry.getValue().getAreaList());
                    System.out.println("Satisfiable:" + regionEntry.getValue().satisfiable());
                }
                //System.out.print(regionEntry.getKey() + " ");

            }

            /*for(Map.Entry<Integer, RegionNew> entry: regionList.entrySet()){
                System.out.println( entry.getValue().getSum() + " " + entry.getValue().getCount());
            }*/

            int unAssignedCount = 0;
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] <= 0)
                    unAssignedCount++;
            }
            //System.out.println("Distance for this regionList" + calculateWithinRegionDistance(regionList, distanceMatrix));
            if (regionList.size() > max_p || (regionList.size() == max_p && unAssignedCount < min_unAssigned)) {
                max_p = regionList.size();
                min_unAssigned = unAssignedCount;
                bestCollection = new RegionCollectionWithVariance(regionList.size(), labels, regionList);
            }

            if (debug) {
                Map<Integer, RegionWithVariance> rcn = bestCollection.getRegionMap();
                for (RegionWithVariance rn : rcn.values()) {
                    if (!rn.satisfiable()) {
                        System.out.println("Region " + rn.getId() + " not satisfiable!");
                        System.exit(125);
                    }
                }
            }


        }
        return bestCollection;
    }


    public static void checkLabels(int[] labels, Map<Integer, RegionWithVariance> regionList){
        boolean consistent = true;
        for(int i = 0; i < labels.length; i++){
            if (labels[i] > 0){
                if(!regionList.get(labels[i]).getAreaList().contains(i)){
                    System.out.println("Area " + i + " not in region " + labels[i]);
                    consistent = false;
                }
            }
        }
        for(Map.Entry<Integer, RegionWithVariance> mapEntry: regionList.entrySet()){
            RegionWithVariance region = mapEntry.getValue();
            for(Integer area: region.getAreaList()){
                if(labels[area] != region.getId()){
                    System.out.println("Region " + region.getId() + " contains area " + area + " but labeled as in region " + labels[area]);
                    consistent = false;
                }
            }
        }
        if(!consistent){
            System.out.println("Label checking failed!");
            System.exit(126);
        }else{
            System.out.println("Pass label checking!");
        }


    }



    public static void  set_input_minmax_var(String fileName,
                                             String minAttrName,
                                             Double minAttrLow,
                                             Double minAttrHigh,
                                             String maxAttrName,
                                             Double maxAttrLow,
                                             Double maxAttrHigh,
                                             String avgAttrName,
                                             Double avgAttrLow,
                                             Double avgAttrHigh,
                                             String varAttrName,
                                             Double varAttrLow,
                                             Double varAttrHigh,
                                             String sumAttrName,
                                             Double sumAttrLow,
                                             Double sumAttrHigh,
                                             Double countLow,
                                             Double countHigh,
                                             String distAttrName,
                                             boolean repeatQuery

    ) throws Exception {
        double startTime = System.currentTimeMillis()/ 1000.0;
        File file = new File(fileName);
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source =
                dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
        ArrayList<Long> minAttr = new ArrayList<>();
        ArrayList<Long> maxAttr = new ArrayList<>();
        ArrayList<Long> avgAttr = new ArrayList<>();
        ArrayList<Long> varAttr = new ArrayList<>();
        ArrayList<Long> sumAttr = new ArrayList<>();
        ArrayList<Long> distAttr = new ArrayList<>();

        ArrayList<SimpleFeature> fList = new ArrayList<>();
        ArrayList<Integer> idList = new ArrayList<>();
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = - Double.POSITIVE_INFINITY, maxY = -Double.POSITIVE_INFINITY;
        Double minAttrMin = Double.POSITIVE_INFINITY;
        Double minAttrMax = -Double.POSITIVE_INFINITY;
        Double maxAttrMax = -Double.POSITIVE_INFINITY;
        Double maxAttrMin = Double.POSITIVE_INFINITY;
        double sumMin = Double.POSITIVE_INFINITY;
        int count = 0;
        Double avgTotal = 0.0;
        Double sumTotal = 0.0;
        ArrayList<Geometry> geometryList = new ArrayList<>();
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                //System.out.print(feature.getID());
                //System.out.print(": ");

                minAttr.add(Long.parseLong(feature.getAttribute(minAttrName).toString()));
                maxAttr.add(Long.parseLong(feature.getAttribute(maxAttrName).toString()));
                avgAttr.add(Long.parseLong(feature.getAttribute(avgAttrName).toString()));
                varAttr.add(Long.parseLong(feature.getAttribute(varAttrName).toString()));
                sumAttr.add(Long.parseLong(feature.getAttribute(sumAttrName).toString()));
                distAttr.add(Long.parseLong(feature.getAttribute(distAttrName).toString()));
                fList.add(feature);
                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < sumMin){
                    sumMin = Long.parseLong(feature.getAttribute(sumAttrName).toString());
                }
                if (Long.parseLong(feature.getAttribute(avgAttrName).toString()) < 0){
                    System.out.println("AVG attribute contains negative value(s)");
                    return;
                }
                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < 0){
                    System.out.println("SUM attribute contains negative value(s)");
                    return;
                }
                if (Long.parseLong(feature.getAttribute(minAttrName).toString()) < minAttrMin){
                    minAttrMin = Double.parseDouble(feature.getAttribute(minAttrName).toString());
                }
                if (Long.parseLong(feature.getAttribute(minAttrName).toString()) > minAttrMax){
                    minAttrMax = Double.parseDouble(feature.getAttribute(minAttrName).toString());
                }
                if(Long.parseLong(feature.getAttribute(maxAttrName).toString()) > maxAttrMax){
                    maxAttrMax = Double.parseDouble(feature.getAttribute(maxAttrName).toString());
                }
                if(Long.parseLong(feature.getAttribute(maxAttrName).toString()) < maxAttrMin){
                    maxAttrMin = Double.parseDouble(feature.getAttribute(maxAttrName).toString());
                }
                count ++;
                avgTotal += Double.parseDouble(feature.getAttribute(avgAttrName).toString());
                sumTotal += Double.parseDouble(feature.getAttribute(sumAttrName).toString());
                //System.out.println(feature.getID());
                idList.add(Integer.parseInt(feature.getID().split("\\.")[1]) - 1);
                //System.out.print(feature.getID());
                //System.out.print(": ");
                //fList.add(feature);
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                geometryList.add(geometry);
                double cminx = geometry.getEnvelope().getCoordinates()[0].getX();
                double cminy = geometry.getEnvelope().getCoordinates()[0].getY();
                double cmaxx = geometry.getEnvelope().getCoordinates()[2].getX();
                double cmaxy = geometry.getEnvelope().getCoordinates()[2].getY();
                if (minX > cminx){
                    minX = cminx;
                }
                if (minY > cminy){
                    minY = cminy;
                }
                if (maxX < cmaxx){
                    maxX = cmaxx;
                }
                if (maxY < cmaxy){
                    maxY = cmaxy;
                }

                //idList.add(Integer.parseInt(feature.getID().split("\\.")[1]) - 1);
            }
            features.close();

            //sg.printIndex();
        }
        dataStore.dispose();
        avgTotal = avgTotal / count;


        //Feasibility checking
        // (1)The situation for AVG will change after removing infeasible areas
        // (2) Even when avgTotal does not lie with in avgAttrMin and avgAttrMax, the algorithm will
        if(minAttrMin > minAttrHigh|| minAttrMax < minAttrLow|| maxAttrMin > maxAttrHigh || maxAttrMax < maxAttrLow||  sumMin > sumAttrHigh || sumTotal < sumAttrLow || count < countLow){
            System.out.println("The constraint settings are infeasible. The program will terminate immediately.");
            System.exit(1);
        }
        if(varAttrLow > varAttrHigh){
            System.out.println("The constraint settings are infeasible. The program will terminate immediately.");
            System.exit(1);
        }
        if(minAttrMin > minAttrHigh){
            System.out.println("There is no area satisfying the MIN <=. The program will terminate immediately.");
            System.exit(1);
        }else if(minAttrMax < minAttrLow){
            System.out.println("There is no area satisfying the MIN >=. The program will terminate immediately.");
            System.exit(1);
        }

        double rookstartTime = System.currentTimeMillis()/ 1000.0;

        SpatialGrid sg = new SpatialGrid(minX, minY, maxX, maxY);

        HashMap<Integer, Set<Integer>> neighborMap = calculateNeighbors(geometryList);
        sg.setNeighbors(neighborMap);
        double rookendTime = System.currentTimeMillis()/ 1000.0;
        System.out.println("Rook time: " + (rookendTime - rookstartTime));

        double dataLoadTime = System.currentTimeMillis()/ 1000.0;
        System.out.println("Input size: " + distAttr.size());
        long [][] distanceMatrix = EMPTabu.pdist(distAttr);
        Date t = new Date();

        String fileNameSplit[] = fileName.split("/");
        String mapName = fileNameSplit[fileNameSplit.length-1].split("\\.")[0];
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

        String timeStamp = df.format(t);
        String folderName = "data/AlgorithmTesting/FaCT_" + mapName + "_MIN-" + minAttrLow + "-" + minAttrHigh + "_AVG-" + avgAttrLow + "-" + avgAttrHigh + "_VAR-" + varAttrLow + "-" + varAttrHigh + "_SUM-" +sumAttrLow + "-" + sumAttrHigh + "-" + timeStamp;
        File folder = new File(folderName);
        folder.mkdirs();
        File settingFile = new File(folderName + "/Settings.csv");
        if(!settingFile.exists()){
            settingFile.createNewFile();
        }
        Writer settingWriter = new FileWriter(settingFile);
        settingWriter.write("Constraint, Attribute Name, Lower Bound, Upper Bound\n");
        settingWriter.write("Min, " + minAttrName + ", " + minAttrLow + ", " + minAttrHigh + "\n");
        settingWriter.write("Max, " + maxAttrName + ", " + maxAttrLow + ", " + maxAttrHigh + "\n");
        settingWriter.write("Avg, " + avgAttrName + ", " + avgAttrLow + ", " + avgAttrHigh + "\n");
        settingWriter.write("Var, " + varAttrName + ", " + varAttrLow + ", " + varAttrHigh + "\n");
        settingWriter.write("Sum, " + sumAttrName + ", " + sumAttrLow + ", " + sumAttrHigh + "\n");
        settingWriter.write("Count, "  + ", " + countLow + ", " + countHigh + "\n");
        settingWriter.write("Rand," + randFlag[0] + "," + randFlag[1] + "\n");
        settingWriter.close();
        File csvFile = new File(folderName + "/Result_" + mapName+"_"+ timeStamp + ".csv");
        //System.out.println(csvFile);
        if(!csvFile.exists()){
            csvFile.createNewFile();
        }
        Writer csvWriter = new FileWriter(csvFile);
        csvWriter.write("Iteration, Max P, Construction Time, Heuristic Time, Construction + Heuristic, Score Before Heuristic, Score After Heuristic, Score Difference，Unassigned areas\n");
        //RegionCollection rc = construction_phase_gene(population, income, 1, sg, idList,4000,Double.POSITIVE_INFINITY);

        String recordName = "data/emp_record/emp_" + mapName + "_" + minAttrName +  minAttrLow + "-" + minAttrHigh + "_" + maxAttrName +maxAttrLow + "-" + maxAttrHigh + "_"+ avgAttrName + avgAttrLow + "-" + avgAttrHigh + "_" +varAttrName + varAttrLow + "-" + varAttrHigh + "_" + sumAttrName +sumAttrLow + "-" + sumAttrHigh + "_" + countLow + "-" + countHigh;
        varLowerbound = varAttrLow;
        varUpprbound = varAttrHigh;
        for(int i = 0; i < numOfIts; i++){
            double constructionStart = System.currentTimeMillis() / 1000.0;
            RegionCollectionWithVariance rc = construction_phase_changeOfAttribute(idList, distAttr, sg,
                    minAttr,
                    minAttrLow,
                    minAttrHigh,

                    maxAttr,
                    maxAttrLow,
                    maxAttrHigh,

                    avgAttr,
                    avgAttrLow,
                    avgAttrHigh,

                    varAttr,
                    varAttrLow,
                    varAttrHigh,

                    sumAttr,
                    sumAttrLow,
                    sumAttrHigh,
                    countLow, countHigh, repeatQuery, recordName);
            int[] labelsCons = rc.getLabels();
            int unassingedConstruct = 0;
            for(int k = 0; k < labelsCons.length; k++){
                if(labelsCons[k] < 1){
                    unassingedConstruct += 1;
                }
            }
            System.out.println("Unassigned after construction: " + unassingedConstruct);
            double constructionEnd = System.currentTimeMillis() / 1000.0;
            double constructionDuration = constructionEnd - constructionStart;
            //System.out.println("Time for construction phase:\n" + (constructionTime - rookendTime));
            System.out.println("Construction time: " + constructionDuration);
            int max_p = rc.getMax_p();
            //System.out.println("MaxP: " + max_p);
            //Map<Integer, Integer> regionSpatialAttr = rc.getRegionSpatialAttr();
        /*System.out.println("regionSpatialAttr after construction_phase:");
        for(Map.Entry<Integer, Integer> entry: regionSpatialAttr.entrySet()){
            Integer rid = entry.getKey();
            Integer rval = entry.getValue();
            System.out.print(rid + ": ");
            System.out.print(rval + " ");
            //System.out.println();
        }*/

            long totalWDS = EMPTabu.calculateWithinRegionDistance_var(rc.getRegionMap(), distanceMatrix);
            //System.out.println("totalWithinRegionDistance before tabu: \n" + totalWDS);
            int tabuLength = 10;
            //int max_no_move = distAttr.size();
            int max_no_move =1000;
            //checkLabels(rc.getLabels(), rc.getRegionList());

            //System.out.println("Start tabu");
            if(debug){
                System.out.println(Arrays.toString(rc.getLabels()));
                System.out.println(rc.getRegionMap().keySet());
                checkLabels_var(rc.getLabels(), rc.getRegionMap());
            }
            System.out.println("Var 2343 " + varAttr.get(2343));

            //System.out.println(labels.length);
            //long WDSDifference = totalWDS - tr.WDS;
            long WDSDifference;
            long WDS;
            int[] labels;
            if(false){
                labels = rc.getLabels();
                WDSDifference = 0;
                WDS = totalWDS;
            }else{
                TabuReturn tr = EMPTabu.performTabu_var(rc.getLabels(), rc.getRegionMap(), sg, EMPTabu.pdist((distAttr)), tabuLength, max_no_move, minAttr, maxAttr, avgAttr, varAttr, sumAttr);
                labels = tr.labels;
                WDSDifference = totalWDS - tr.WDS;
                WDS = tr.WDS;
            }
            //int[] labels = SimulatedAnnealing.performSimulatedAnnealing(rc.getLabels(), rc.getRegionList(), sg, pdist((distAttr)), minAttr, maxAttr, sumAttr, avgAttr);
            //int[] labels = rc.getLabels();
            double endTime = System.currentTimeMillis()/ 1000.0;
            //System.out.println("MaxP: " + max_p);
            double heuristicDuration = endTime - constructionEnd;
            //System.out.println("Time for tabu(s): \n" + (endTime - constructionTime));
            // System.out.println("total time: \n" +(endTime - startTime));
            File f = new File(folderName +"/" + i + ".txt");
            if(!f.exists()){
                f.createNewFile();
            }
            int unassignedCount = 0;
            Writer w = new FileWriter(f);
            for( int j = 0; j < labels.length; j++){
                w.write(labels[j] + "\n");
                if(labels[j] < 1){
                    unassignedCount++;
                }
            }
            w.close();
            System.out.println("minTime: " + minTime);
            System.out.println("avgTime: " + avgTime);
            System.out.println("sumTime: " + sumTime);

            System.out.println("Iteration: " + i);
            System.out.println("p: "+ max_p);
            System.out.println("Construction time: " + constructionDuration);
            System.out.println("Tabu search time: " + heuristicDuration);
            System.out.println("Heterogeneity score before Tabu: "  + totalWDS);
            System.out.println("Heterogeneity score after Tabu: " + WDS);
            System.out.println("Number of unassigned areas: " + unassignedCount + "\n");

            csvWriter.write(i + ", " + max_p + ", " + constructionDuration + ", " + heuristicDuration + ", " + (constructionDuration+heuristicDuration) + ", " + totalWDS + ", " + WDS + ", " + (WDS - totalWDS) + "," + unassignedCount +"," + minTime + "," + avgTime + "," + sumTime + "\n");
            csvWriter.flush();
            minTime = 0;
            avgTime = 0;
            sumTime = 0;

        }
        csvWriter.close();

        if(debug)
            System.out.println("End of setipnput");

    }

    public static HashMap<Integer, Set<Integer>> calculateNeighbors(ArrayList<Geometry> polygons) {

        HashMap<Integer, Set<Integer>> neighborMap = new HashMap<>();

        for (int i = 0; i < polygons.size(); i++) {

            neighborMap.put(i, new TreeSet<>());
        }


        for (int i = 0; i < polygons.size(); i++) {

            for (int j = i + 1; j < polygons.size(); j++) {

                if (polygons.get(i).intersects(polygons.get(j))) {

                    Geometry intersection = polygons.get(i).intersection(polygons.get(j));

                    if (intersection.getGeometryType() != "Point") {

                        neighborMap.get(i).add(j);
                        neighborMap.get(j).add(i);

                    } // end if
                } // end if
            } // end for
        } // end for

        return neighborMap;
    }
    static public void checkLabels_var(int[] labels, Map<Integer, RegionWithVariance> regionMap){
        boolean consistent = true;
        for(int i = 0; i < labels.length; i++){
            if (labels[i] > 0){
                if(!regionMap.get(labels[i]).getAreaList().contains(i)){
                    System.out.println("Area " + i + " not in region " + labels[i]);
                    consistent = false;
                }
            }
        }
        for(Map.Entry<Integer, RegionWithVariance> mapEntry: regionMap.entrySet()){
            RegionWithVariance region = mapEntry.getValue();
            for(Integer area: region.getAreaList()){
                if(labels[area] != region.getId()){
                    System.out.println("Region " + region.getId() + " contains area " + area + " but labeled as in region " + labels[area]);
                    consistent = false;
                }
            }
        }
        if(!consistent){
            System.out.println("Label checking failed!");
            System.exit(126);
        }else{
            System.out.println("Pass label checking!");
        }


    }
    public static RegionCollectionWithVariance construction_phase_changeOfAttribute(ArrayList<Integer> idList,
                                                                                    ArrayList<Long> disAttr,
                                                                                    SpatialGrid r,
                                                                                    ArrayList<Long> minAttr,
                                                                                    Double minLowerBound,
                                                                                    Double minUpperBound,

                                                                                    ArrayList<Long> maxAttr,
                                                                                    Double maxLowerBound,
                                                                                    Double maxUpperBound,

                                                                                    ArrayList<Long> avgAttr,
                                                                                    Double avgLowerBound,
                                                                                    Double avgUpperBound,

                                                                                    ArrayList<Long> varAttr,
                                                                                    Double varLowerBound,
                                                                                    Double varUpperBound,

                                                                                    ArrayList<Long> sumAttr,
                                                                                    Double sumLowerBound,
                                                                                    Double sumUpperBound,

                                                                                    Double countLowerBound,
                                                                                    Double countUpperBound, boolean repeatQuery, String recordName)
    {

        int maxIt = 1;
        RegionWithVariance.setRange(minLowerBound, minUpperBound, maxLowerBound, maxUpperBound, avgLowerBound, avgUpperBound, varLowerBound, varUpperBound, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);



        //boolean var_debug = true;

        //RegionWithVariance.setRange(minLowerBound, minUpperBound, maxLowerBound, maxUpperBound, avgLowerBound, avgUpperBound, varianceLoweBound, varianceUpperBound, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
        ObjectInputStream ois = null;
        boolean differentConstraint = false;
        int stateOfChange = 0;
        Double startTime =  System.currentTimeMillis() / 1000.0;
        if(repeatQuery){
            try{
                ois = new ObjectInputStream(new FileInputStream(recordName + "/constraints.txt"));
                String flag = (String) ois.readObject();
                System.out.println(flag);
                ArrayList<Long> preMinAttr = (ArrayList<Long>) ois.readObject();
                Double preMinLowerBound = (Double) ois.readObject();
                Double preMinUpperBound = (Double) ois.readObject();
                flag = (String) ois.readObject();
                System.out.println(flag);
                ArrayList<Long> preMaxAttr = (ArrayList<Long>) ois.readObject();
                Double preMaxLowerBound = (Double) ois.readObject();
                Double preMaxUpperBound = (Double) ois.readObject();
                flag = (String) ois.readObject();
                System.out.println(flag);
                ArrayList<Long> preAvgAttr = (ArrayList<Long>) ois.readObject();
                Double preAvgLowerBound = (Double) ois.readObject();
                Double preAvgUpperBound = (Double) ois.readObject();
                flag = (String) ois.readObject();
                System.out.println(flag);
                ArrayList<Long> preVarAttr = (ArrayList<Long>) ois.readObject();
                Double preVarLowerBound = (Double) ois.readObject();
                Double preVarUpperBound = (Double) ois.readObject();
                flag = (String) ois.readObject();
                System.out.println(flag);
                ArrayList<Long> preSumAttr = (ArrayList<Long>) ois.readObject();
                Double preSumLowerBound = (Double) ois.readObject();
                Double preSumUpperBound = (Double) ois.readObject();
                flag = (String) ois.readObject();
                System.out.println(flag);
                Double preCountLowerBound = (Double) ois.readObject();
                Double preCountUpperBound = (Double) ois.readObject();
                if(repeat_debug){
                    System.out.println("MIN/MAX");
                    System.out.println(preMinAttr.equals(minAttr));
                    if(!preMinAttr.equals(minAttr)){
                        for(int i = 0; i < preMinAttr.size(); i++){
                            if(!preMinAttr.get(i).equals(minAttr.get(i))){
                                System.out.println(i + " " + preMinAttr.get(i) + " " + minAttr.get(i));
                            }
                        }
                    }
                    System.out.println(preMaxAttr.equals(maxAttr) + " lengt h" + preMaxAttr.size() + " " + maxAttr.size());
                    if(!preMaxAttr.equals(maxAttr)){
                        for(int i = 0; i < preMaxAttr.size(); i++){
                            if(!preMaxAttr.get(i).equals(maxAttr.get(i))){
                                System.out.println(i + " " + preMaxAttr.get(i) + " " + maxAttr.get(i));
                            }
                        }
                    }

                    System.out.println(preMinLowerBound.equals(minLowerBound));
                    System.out.println(preMinUpperBound.equals(minUpperBound));
                    System.out.println(preMaxLowerBound .equals( maxLowerBound));
                    System.out.println(preMaxUpperBound .equals( maxUpperBound));

                    System.out.println();
                    System.out.println("AVG");
                    if(!preAvgAttr.equals(avgAttr)){
                        for(int i = 0; i < preAvgAttr.size(); i++){
                            if(!preAvgAttr.get(i).equals(avgAttr.get(i))){
                                System.out.println(i + " " + preAvgAttr.get(i) + " " + avgAttr.get(i));
                            }
                        }
                    }
                    System.out.println(preAvgLowerBound.equals(avgLowerBound));
                    System.out.println(preAvgUpperBound.equals(avgUpperBound));

                    System.out.println();
                    System.out.println("SUM");
                    if(!preSumAttr.equals(sumAttr)){
                        for(int i = 0; i < preSumAttr.size(); i++){
                            if(!preSumAttr.get(i).equals(sumAttr.get(i))){
                                System.out.println(i + " " + preSumAttr.get(i) + " " + sumAttr.get(i));
                            }
                        }
                    }

                }

                //preMinAttr.equals(minAttr)
                if(preMinAttr.equals(minAttr) && preMaxAttr.equals(maxAttr) && preMinLowerBound.equals(minLowerBound) && preMinUpperBound.equals(minUpperBound) && preMaxLowerBound .equals( maxLowerBound) && preMaxUpperBound .equals( maxUpperBound)){
                    stateOfChange = 1;
                }else{
                    System.out.println("Different MIN/MAX");
                    differentConstraint = true;
                }
                if(!differentConstraint){
                    if(preVarAttr.equals(varAttr) && preVarLowerBound.equals(varLowerBound) && preVarUpperBound.equals(varUpperBound) && preAvgAttr.equals(avgAttr) && preAvgLowerBound.equals(avgLowerBound) && preAvgUpperBound.equals(avgUpperBound)){
                        stateOfChange = 2;
                    }else{
                        System.out.println("Different AVG/VAR");
                        differentConstraint = true;
                    }
                }
                if(!differentConstraint){
                    if(preSumAttr.equals(sumAttr) && preSumUpperBound.equals(sumUpperBound) && preSumLowerBound.equals(sumLowerBound) &&  preCountUpperBound.equals(countUpperBound) && preCountLowerBound.equals(countLowerBound)){
                        stateOfChange = 3;
                    }else{
                        System.out.println("Different SUM/COUNT");
                        differentConstraint = true;
                    }
                }


            }catch(EOFException e){//Previous result not found
                e.printStackTrace();
                differentConstraint = true;
                stateOfChange = 0;

            }catch(FileNotFoundException e) {
                e.printStackTrace();
                differentConstraint = true;
                stateOfChange = 0;
            }catch(Exception e){

                e.printStackTrace();
            }
        }
        Double readTime =  System.currentTimeMillis() / 1000.0;
        //if(var_debug)
        System.out.println("Time for reading the pre-results: " + (readTime - startTime));
        System.out.println("Different constraint check: " + differentConstraint + " " + stateOfChange);

        ObjectOutputStream oos = null;
        if(repeatQuery && !differentConstraint){
            try{
                //ObjectInputStream ois = null;
                ois = new ObjectInputStream(new FileInputStream(recordName + "/bestCollection.txt"));
                RegionCollectionWithVariance bc = (RegionCollectionWithVariance)ois.readObject();
                return bc;

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(repeatQuery && differentConstraint){

            try{
                File recordFolder = new File(recordName);
                if(!recordFolder.exists()){
                    recordFolder.mkdirs();
                }

                oos = new ObjectOutputStream(new FileOutputStream(recordName + "/constraints.txt"));
                oos.writeObject("MIN");
                oos.writeObject(minAttr);
                oos.writeObject(minLowerBound);
                oos.writeObject(minUpperBound);
                oos.writeObject("MAX");
                oos.writeObject(maxAttr);
                oos.writeObject(maxLowerBound);
                oos.writeObject(maxUpperBound);
                oos.writeObject("AVG");
                oos.writeObject(avgAttr);
                oos.writeObject(avgLowerBound);
                oos.writeObject(avgUpperBound);
                oos.writeObject("VAR");
                oos.writeObject(varAttr);
                oos.writeObject(varLowerBound);
                oos.writeObject(varUpperBound);
                oos.writeObject("SUM");
                oos.writeObject(sumAttr);
                oos.writeObject(sumLowerBound);
                oos.writeObject(sumUpperBound);
                oos.writeObject("COUNT");
                oos.writeObject(countLowerBound);
                oos.writeObject(countUpperBound);

            }catch(Exception e){
                e.printStackTrace();
            }
        }
        int max_p = 0;
        Double writeTime =  System.currentTimeMillis() / 1000.0;
        System.out.println("Time for writing the current constraints: " + (writeTime - readTime));
        RegionCollectionWithVariance bestCollection = null;
        double minStart = System.currentTimeMillis() / 1000.0;

        ArrayList<Integer> areas = new ArrayList<Integer>();
        areas.addAll(idList);
        int min_unAssigned = maxAttr.size();
        //First step: Filtering and seeding for MIN, MAX.
        Pair<int[], ArrayList<Integer>> minmaxResult = null;
        int[] minmaxlabels = null;
        ArrayList<Integer> minmaxseedAreas =null;
        if(repeatQuery && (differentConstraint && stateOfChange == 1)){
            try{
                ois = new ObjectInputStream(new FileInputStream(recordName + "/minmax.txt"));
                minmaxResult = (Pair<int[], ArrayList<Integer>>) ois.readObject();
                minmaxlabels = minmaxResult.getValue0();
                minmaxseedAreas = minmaxResult.getValue1();
                //minmaxseedAreas = (ArrayList<Integer>)ois.readObject();
            }catch(Exception e){
                e.printStackTrace();
            }

        }else if(repeatQuery && (differentConstraint && stateOfChange > 1)){}
        else{
            minmaxResult = filtering_and_seeding(areas, minAttr, minLowerBound, minUpperBound, maxAttr, maxLowerBound, maxUpperBound, sumAttr, sumUpperBound);
            if(repeatQuery){
                try{
                    oos = new ObjectOutputStream(new FileOutputStream(recordName + "/minmax.txt", true));
                    oos.writeObject(minmaxResult);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

            minmaxlabels = minmaxResult.getValue0();
            minmaxseedAreas = minmaxResult.getValue1();

        }



        double minEnd = System.currentTimeMillis() / 1000.0;
        minTime = minTime + (minEnd - minStart);
        ObjectInputStream varOis = null;
        ObjectInputStream avgOis = null;
        ObjectInputStream sumcountOis = null;
        //ObjectOutputStream sumOos = null;
        boolean hasVar = false;
        boolean hasAvg = false;
        if(!(varLowerBound.equals(-Double.POSITIVE_INFINITY) && varUpperBound.equals(Double.POSITIVE_INFINITY))){
            hasVar = true;
        }
        if(!(avgLowerBound.equals(-Double.POSITIVE_INFINITY) && avgUpperBound.equals(Double.POSITIVE_INFINITY))){
            hasAvg = true;
        }
        System.out.println("Has var: " + hasVar + " has avg " + hasAvg);
        if(hasVar && hasAvg){
            System.out.println("Please do not specify the variance and the average constraint at the same time");
            System.exit(12138);
        }
        try{
            if(repeatQuery){
                if(hasVar)
                    varOis =  new ObjectInputStream(new FileInputStream(recordName + "/var.txt"));
                if(hasAvg)
                    avgOis = new ObjectInputStream(new FileInputStream(recordName + "/avg.txt"));
                if(!differentConstraint)
                    sumcountOis = new ObjectInputStream(new FileInputStream(recordName + "/sumcount.txt"));
            }

            //sumOos = new ObjectOutputStream(new FileOutputStream(recordName + "/sumcount.txt", true));

        }catch (Exception e){
            e.printStackTrace();
        }
        //ObjectOutputStream sumOos = null;
        for (int it = 0; it < maxIt; it++) {
            double avgStart = System.currentTimeMillis() / 1000.0;
            int[] labels = null;
            ArrayList<Integer> seedAreas = null;
            if(!repeatQuery || differentConstraint && stateOfChange <= 1){
                labels = minmaxlabels.clone();
                seedAreas = new ArrayList<>(minmaxseedAreas);
                //System.out.println(seedAreas);
            }
            Map<Integer, RegionWithVariance> regionList = null;

            if(var_debug)
                System.out.println("Before initialiazation");

            if(hasVar){
                Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> varInitialization = null;

                if(repeatQuery &&(differentConstraint && stateOfChange == 2)){
                    try{
                        //ois = new ObjectInputStream(new FileInputStream(recordName + "/var.txt"));
                        varInitialization = ( Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>>)varOis.readObject();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    labels = varInitialization.getValue0();
                    regionList = varInitialization.getValue1();
                }else{
                    varInitialization = region_initialization_var(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, varAttr, varLowerBound, varUpperBound, sumAttr);
                    if(repeatQuery){
                        File f = new File(recordName + "/var.txt");
                        try{
                            if(!f.exists() || f.length() == 0){
                                oos = new ObjectOutputStream(new FileOutputStream(recordName + "/var.txt"));
                                oos.writeObject(varInitialization);
                            }else{
                                oos = new EMPObjectOutputStream(new FileOutputStream(recordName + "/var.txt", true));
                                oos.writeObject(varInitialization);
                            }
                        }catch(Exception e){
                            e.printStackTrace();
                        }


                    }

                    labels = varInitialization.getValue0();
                    regionList = varInitialization.getValue1();
                }
                //Second step: AVG and MIN MAX revisit
                //Quartet<int[], Map<Integer, Region>, List<Integer>, List<Integer>> avgInitialization = region_initialization(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, avgLowerBound, avgUpperBound, sumAttr);
                //Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> varInitialization = region_initialization_var(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, varAttr, varLowerBound, varUpperBound, sumAttr);
                if(var_debug)
                    System.out.println("After initialization");

            }else{
                Quartet<int[], Map<Integer, RegionWithVariance>, List<Integer>, List<Integer>> avgInitialization = null;
                if(repeatQuery &&(differentConstraint && stateOfChange == 2)){
                    try{
                        //ois = new ObjectInputStream(new FileInputStream(recordName + "/var.txt"));
                        avgInitialization =  (Quartet<int[], Map<Integer, RegionWithVariance>, List<Integer>, List<Integer>>) avgOis.readObject();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    labels = avgInitialization.getValue0();
                    regionList = avgInitialization.getValue1();
                }else if(repeatQuery &&(differentConstraint && stateOfChange > 2)){}
                else{
                    avgInitialization = region_initialization(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, avgLowerBound, avgUpperBound, varAttr, varLowerBound, varUpperBound, sumAttr);
                    try{
                        if(repeatQuery){
                            File f = new File(recordName + "/avg.txt");
                            if(!f.exists() || f.length() == 0){
                                oos = new ObjectOutputStream(new FileOutputStream(recordName + "/avg.txt"));
                                oos.writeObject(avgInitialization);
                            }else{
                                oos = new EMPObjectOutputStream(new FileOutputStream(recordName + "/avg.txt", true));
                                oos.writeObject(avgInitialization);
                            }
                        }

                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    labels = avgInitialization.getValue0();
                    regionList = avgInitialization.getValue1();
                }
                //Second step: AVG and MIN MAX revisit
                //Quartet<int[], Map<Integer, Region>, List<Integer>, List<Integer>> avgInitialization = region_initialization(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, avgLowerBound, avgUpperBound, sumAttr);
                //Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> varInitialization = region_initialization_var(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, varAttr, varLowerBound, varUpperBound, sumAttr);
                if(var_debug)
                    System.out.println("After initialization");

            }

            if(debug){
                checkLabels_var(labels, regionList);
            }
            double avgEnd = System.currentTimeMillis() / 1000.0;
            //avgTime += (avgEnd - minEnd);
            avgTime += (avgEnd - avgStart);
            //Start sum and count
            if (var_debug) {
                System.out.println("-------------");
                System.out.println("P after AVG: " + regionList.size());
                int unassignedCount = 0;
                for(int i = 0; i < labels.length; i++){
                    System.out.print(labels[i] + " ");
                    if(labels[i] < 1){
                        unassignedCount++;
                    }
                }

                System.out.println("No of unassigned areas after var" + unassignedCount);
                System.out.println("-------------");

            }
            //checkLabels_var(labels, regionList);
            /*if (var_debug) {
                for (Map.Entry<Integer, RegionWithVariance> regionEntry : regionList.entrySet()) {
                    // if (!regionEntry.getValue().satisfiable()) {
                    //idToBeRemoved.add(regionEntry.getValue().getId());
                    //regionList_MaxP_AVG.add(regionEntry.getKey());



                    System.out.println("Id to be removed:" + regionEntry.getValue().getId());
                    System.out.println("Min:" + regionEntry.getValue().getMin());
                    System.out.println("Max:" + regionEntry.getValue().getMax());
                    System.out.println("Avg:" + regionEntry.getValue().getAverage());
                    System.out.println("Var:" +  regionEntry.getValue().getVariance());
                    System.out.println("Sum:" + regionEntry.getValue().getSum());

                    System.out.println("Count:" + regionEntry.getValue().getCount());
                    System.out.println("Areas: " + regionEntry.getValue().getAreaList());
                    System.out.println("Satisfiable:" + regionEntry.getValue().satisfiable());
                }
                //System.out.print(regionEntry.getKey() + " ");

            }*/
            Pair<int[], Map<Integer, RegionWithVariance>> result = null;
            if(repeatQuery &&(!differentConstraint || stateOfChange > 2)){
                try{
                    //sumcountOis = new ObjectInputStream(new FileInputStream(recordName + "/sumcount.txt"));
                    result = ( Pair<int[], Map<Integer, RegionWithVariance>>)sumcountOis.readObject();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }else{
                result = sumcount_construction_var(labels, regionList, r, minAttr, maxAttr, avgAttr, varAttr, sumAttr, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
                try{

                    if(repeatQuery){
                        File f = new File(recordName + "/sumcount.txt");
                        if(!f.exists() || f.length() == 0){
                            oos = new ObjectOutputStream(new FileOutputStream(recordName + "/sumcount.txt"));
                            oos.writeObject(result);
                        }else{
                            oos = new EMPObjectOutputStream(new FileOutputStream(recordName + "/sumcount.txt", true));
                            oos.writeObject(result);
                        }
                    }

                    //oos = new ObjectOutputStream(new FileOutputStream(recordName + "/sumcount.txt", true));
                    //oos.writeObject(result);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            //Pair<int[], Map<Integer, RegionWithVariance>> result = sumcount_construction_var(labels, regionList, r, minAttr, maxAttr, avgAttr, varAttr, sumAttr, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
            labels = result.getValue0();
            regionList = result.getValue1();
            double sumEnd = System.currentTimeMillis() / 1000.0;
            sumTime += (sumEnd - avgEnd);
            //System.out.println("Finish SUM");
            /*if(repeatQuery){
                ObjectOutputStream oos = null;
                try{
                    oos = new ObjectOutputStream(new FileOutputStream("./prevResults.txt"));
                    oos.writeObject(recordName);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }*/
            if (var_debug) {
                for (Map.Entry<Integer, RegionWithVariance> regionEntry : regionList.entrySet()) {
                    // if (!regionEntry.getValue().satisfiable()) {
                    //idToBeRemoved.add(regionEntry.getValue().getId());
                    //regionList_MaxP_AVG.add(regionEntry.getKey());



                    System.out.println("Id to be removed:" + regionEntry.getValue().getId());
                    System.out.println("Min:" + regionEntry.getValue().getMin());
                    System.out.println("Max:" + regionEntry.getValue().getMax());
                    System.out.println("Avg:" + regionEntry.getValue().getAverage());
                    System.out.println("Var:" +  regionEntry.getValue().getVariance());
                    System.out.println("Sum:" + regionEntry.getValue().getSum());

                    System.out.println("Count:" + regionEntry.getValue().getCount());
                    System.out.println("Areas: " + regionEntry.getValue().getAreaList());
                    System.out.println("Satisfiable:" + regionEntry.getValue().satisfiable());
                }
                //System.out.print(regionEntry.getKey() + " ");

            }

            /*for(Map.Entry<Integer, RegionNew> entry: regionList.entrySet()){
                System.out.println( entry.getValue().getSum() + " " + entry.getValue().getCount());
            }*/

            int unAssignedCount = 0;
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] <= 0)
                    unAssignedCount++;
            }
            //System.out.println("Distance for this regionList" + calculateWithinRegionDistance(regionList, distanceMatrix));
            if (regionList.size() > max_p || (regionList.size() == max_p && unAssignedCount < min_unAssigned)) {
                max_p = regionList.size();
                min_unAssigned = unAssignedCount;
                bestCollection = new RegionCollectionWithVariance(regionList.size(), labels, regionList);
            }

            if (debug) {
                Map<Integer, RegionWithVariance> rcn = bestCollection.getRegionMap();
                for (RegionWithVariance rn : rcn.values()) {
                    if (!rn.satisfiable()) {
                        System.out.println("Region " + rn.getId() + " not satisfiable!");
                        System.exit(125);
                    }
                }
            }


        }
        try{
            if(repeatQuery){
                oos = new ObjectOutputStream(new FileOutputStream(recordName + "/bestCollection.txt"));
                oos.writeObject(bestCollection);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return bestCollection;
    }

    public static RegionCollectionWithVariance localReconstruction (int[] areaChanged,
                                            ArrayList<Integer> idList,
                                            RegionCollectionWithVariance rc,
                                            SpatialGrid r,
                                            ArrayList<Long> distAttr,
                                            ArrayList<Long> minAttr,
                                            Double minLowerBound,
                                            Double minUpperBound,

                                            ArrayList<Long> maxAttr,
                                            Double maxLowerBound,
                                            Double maxUpperBound,

                                            ArrayList<Long> avgAttr,
                                            Double avgLowerBound,
                                            Double avgUpperBound,

                                            ArrayList<Long> varAttr,
                                            Double varLowerBound,
                                            Double varUpperBound,

                                            ArrayList<Long> sumAttr,
                                            Double sumLowerBound,
                                            Double sumUpperBound,

                                            Double countLowerBound,
                                            Double countUpperBound, boolean repeatQuery, String recordName,
                                            boolean greedy){
        //System.out.println("Enter local reconstruct");

        Set<Integer> affectedRegions = new HashSet<Integer>();
        int[] labels = rc.getLabels().clone();
        if(localrec_debug){
            System.out.println("    Area affected: " + areaChanged[0] + " belongs to region " + labels[areaChanged[0]]);
        }
        //
        for(int i = 0; i < areaChanged.length; i++){
            affectedRegions.add(labels[areaChanged[i]]);
        }
        Map<Integer, RegionWithVariance> regionMap = rc.getRegionMap();
        boolean onehop = false;
        Integer centerRegion = -1;
        for(Integer regionId: affectedRegions){
            RegionWithVariance rv = regionMap.get(regionId);
            Set<Integer> neighborRegions = rv.getRegionNeighborSet(labels);
            neighborRegions.add(regionId);
            if(neighborRegions.containsAll(affectedRegions)){
                onehop = true;
                centerRegion = regionId;
                break;
            }
        }

        if(onehop){
            RegionWithVariance rv = regionMap.get(centerRegion);
            Set<Integer> deconstructRegions = rv.getRegionNeighborSet(labels);
            deconstructRegions.add(centerRegion);
            if(localrec_debug){
                System.out.println("Affected regions is within one hop of " + centerRegion);

                System.out.println("Regions to be deconstructed " + deconstructRegions);
                //System.out.println(affectedRegions);
            }
            Set<Integer> deconstructAreas = new HashSet<Integer>();
            for(Integer dr: deconstructRegions){
                deconstructAreas.addAll(regionMap.get(dr).getAreaList());
            }
            if(localrec_debug) {

                System.out.println();
                System.out.println("Affected areas " + deconstructAreas);
                System.out.println("No of affected areas " + deconstructAreas.size());
                System.out.println();
            }
            reconstructedAreas += deconstructAreas.size();
            RegionCollectionWithVariance nrc = construction_phase_LocalReconstruct(idList, distAttr, r,
                    minAttr,
                    minLowerBound,
                    minUpperBound,

                    maxAttr,
                    maxLowerBound,
                    maxUpperBound,

                    avgAttr,
                    avgLowerBound,
                    avgUpperBound,

                    varAttr,
                    varLowerBound,
                    varUpperBound,

                    sumAttr,
                    sumLowerBound,
                    sumUpperBound,
                    countLowerBound, countUpperBound, false, deconstructAreas, recordName);
            int noOfDec = deconstructRegions.size();
            int noOfRec = nrc.getMax_p();
            System.out.println("Region deconstructed " + noOfDec + " " + deconstructRegions);
            System.out.println("Region reconstructed " + noOfRec + " " + nrc.getRegionMap().keySet());
            if(greedy){
                if(noOfDec > noOfRec){
                    return rc;
                }
            }
            //System.out.println("Area 1 check " +rc.getLabels()[0]);
            for(Integer area: deconstructAreas){
                System.out.print(nrc.getLabels()[area] + " ");
            }
            System.out.println();
            if(localrec_debug){
                System.out.println("Check region feasibility afte deconstruction");
                for (Map.Entry<Integer, RegionWithVariance> regionEntry : nrc.getRegionMap().entrySet()) {
                    if (!regionEntry.getValue().satisfiable()) {
                        System.out.println("Region " + regionEntry.getKey() + " not satisfiable");
                    }
                }

                int newUnassigned = 0;
                for(int i : deconstructAreas){
                    if(nrc.getLabels()[i] < 1){
                        newUnassigned += 1;
                    }
                }
                System.out.println("No of new unassigned areas: " + newUnassigned);

            }
            Map<Integer, RegionWithVariance> oldRegionMap = new HashMap<>();
            oldRegionMap.putAll(rc.getRegionMap());
            int[] oldLabels = rc.getLabels().clone();
            for(Integer deconstructedRegion: deconstructRegions){
                oldRegionMap.remove(deconstructedRegion);
            }
            Map<Integer, RegionWithVariance> recRegionMap = nrc.getRegionMap();
            int largestId = 0;
            for(Integer i: regionMap.keySet()){
                if(i > largestId){
                    largestId = i;
                }
            }
            for (Map.Entry<Integer, RegionWithVariance> regionEntry :recRegionMap.entrySet()) {
                if (regionEntry.getValue().satisfiable()) {
                    regionEntry.getValue().updateId(regionEntry.getKey() + largestId, oldLabels);
                    oldRegionMap.put(regionEntry.getKey() + largestId, regionEntry.getValue());
                }
            }
            //checkLabels_var(oldLabels, oldRegionMap);
            checkLabels_var(oldLabels, oldRegionMap);
            return new RegionCollectionWithVariance(oldRegionMap.keySet().size(), oldLabels, oldRegionMap);
            /*ArrayList<Long> localMin = new ArrayList<>();
            ArrayList<Long> localMax = new ArrayList<>();
            ArrayList<Long> localAvg = new ArrayList<>();
            ArrayList<Long> localVar = new ArrayList<>();
            ArrayList<Long> localSum = new ArrayList<>();
            for(int i = 0);*/
            //int length = labels.length;

        }else{
            if(localrec_debug){
                System.out.println("Affected regions is NOT within one hop");
                System.out.println(affectedRegions);
            }
            Set<Integer> deconstructRegions = new HashSet<>();
            for(Integer rwr: affectedRegions){
                RegionWithVariance rv = regionMap.get(rwr);
                Set<Integer> neighborRegions = rv.getRegionNeighborSet(labels);
                deconstructRegions.add(rwr);
                deconstructRegions.addAll(neighborRegions);
            }
            System.out.println(deconstructRegions.toString());
            Set<Integer> deconstructAreas = new HashSet<Integer>();
            for(Integer dr: deconstructRegions){
                deconstructAreas.addAll(regionMap.get(dr).getAreaList());
            }
            if(localrec_debug){
                System.out.println();
                System.out.println("Affected areas " + deconstructAreas);
                System.out.println("No of affected areas " + deconstructAreas.size());
                System.out.println();
            }

            reconstructedAreas += deconstructAreas.size();
            RegionCollectionWithVariance nrc = construction_phase_LocalReconstruct(idList, distAttr, r,
                    minAttr,
                    minLowerBound,
                    minUpperBound,

                    maxAttr,
                    maxLowerBound,
                    maxUpperBound,

                    avgAttr,
                    avgLowerBound,
                    avgUpperBound,

                    varAttr,
                    varLowerBound,
                    varUpperBound,

                    sumAttr,
                    sumLowerBound,
                    sumUpperBound,
                    countLowerBound, countUpperBound, false, deconstructAreas, recordName);
            int noOfDec = deconstructRegions.size();
            int noOfRec = nrc.getMax_p();
            System.out.println("Region deconstructed " + noOfDec + " " + deconstructRegions);
            System.out.println("Region reconstructed " + noOfRec + " " + nrc.getRegionMap().keySet());
            if(greedy){
                if(noOfDec > noOfRec){
                    return rc;
                }
            }
            //System.out.println("Area 1 check " +rc.getLabels()[0]);
            for(Integer area: deconstructAreas){
                System.out.print(nrc.getLabels()[area] + " ");
            }
            System.out.println();
            if(localrec_debug){
                System.out.println("Check region feasibility afte deconstruction");
                for (Map.Entry<Integer, RegionWithVariance> regionEntry : nrc.getRegionMap().entrySet()) {
                    if (!regionEntry.getValue().satisfiable()) {
                        System.out.println("Region " + regionEntry.getKey() + " not satisfiable");
                    }
                }

                int newUnassigned = 0;
                for(int i : deconstructAreas){
                    if(nrc.getLabels()[i] < 1){
                        newUnassigned += 1;
                    }
                }
                System.out.println("No of new unassigned areas: " + newUnassigned);

            }
            Map<Integer, RegionWithVariance> oldRegionMap = new HashMap<>();
            oldRegionMap.putAll(rc.getRegionMap());
            int[] oldLabels = rc.getLabels().clone();
            for(Integer deconstructedRegion: deconstructRegions){
                oldRegionMap.remove(deconstructedRegion);
            }
            Map<Integer, RegionWithVariance> recRegionMap = nrc.getRegionMap();
            int largestId = 0;
            for(Integer i: regionMap.keySet()){
                if(i > largestId){
                    largestId = i;
                }
            }
            for (Map.Entry<Integer, RegionWithVariance> regionEntry :recRegionMap.entrySet()) {
                if (regionEntry.getValue().satisfiable()) {
                    regionEntry.getValue().updateId(regionEntry.getKey() + largestId, oldLabels);
                    oldRegionMap.put(regionEntry.getKey() + largestId, regionEntry.getValue());
                }
            }
            //checkLabels_var(oldLabels, oldRegionMap);
            return new RegionCollectionWithVariance(oldRegionMap.keySet().size(), oldLabels, oldRegionMap);
        }




    }

    public static void main(String args[]) throws Exception {
        //String normalDataset = "data/LACounty/La_county_noisland.shp";
        String normalDataset = "data/LACounty/La_county_noisland.shp";
        /*set_input_minmax_var(normalDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "unemployed",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "employed",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "households",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "pop2010",
                200000.0,
                Double.POSITIVE_INFINITY,
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "households",
                false);*/
        /*System.out.println("Local reconstruct");
        System.out.println();
        set_input_localReconstruct(normalDataset,
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
                "pop2010",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "households",
                false

        );*/

        System.out.println("Iterated greedy");
        System.out.println();
        set_input_IteratedGreedy(normalDataset,
                "pop_16up",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "unemployed",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "employed",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "households",
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "pop2010",
                20000.0,
                Double.POSITIVE_INFINITY,
                -Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                "households",
                false,
                1,
                100);


        System.out.println("No of areas reconstructed: " + reconstructedAreas);

    }

    public static void  set_input_localReconstruct(String fileName,
                                             String minAttrName,
                                             Double minAttrLow,
                                             Double minAttrHigh,
                                             String maxAttrName,
                                             Double maxAttrLow,
                                             Double maxAttrHigh,
                                             String avgAttrName,
                                             Double avgAttrLow,
                                             Double avgAttrHigh,
                                             String varAttrName,
                                             Double varAttrLow,
                                             Double varAttrHigh,
                                             String sumAttrName,
                                             Double sumAttrLow,
                                             Double sumAttrHigh,
                                             Double countLow,
                                             Double countHigh,
                                             String distAttrName,
                                             boolean repeatQuery

    ) throws Exception {
        double startTime = System.currentTimeMillis()/ 1000.0;
        File file = new File(fileName);
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source =
                dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
        ArrayList<Long> minAttr = new ArrayList<>();
        ArrayList<Long> maxAttr = new ArrayList<>();
        ArrayList<Long> avgAttr = new ArrayList<>();
        ArrayList<Long> varAttr = new ArrayList<>();
        ArrayList<Long> sumAttr = new ArrayList<>();
        ArrayList<Long> distAttr = new ArrayList<>();

        ArrayList<SimpleFeature> fList = new ArrayList<>();
        ArrayList<Integer> idList = new ArrayList<>();
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = - Double.POSITIVE_INFINITY, maxY = -Double.POSITIVE_INFINITY;
        Double minAttrMin = Double.POSITIVE_INFINITY;
        Double minAttrMax = -Double.POSITIVE_INFINITY;
        Double maxAttrMax = -Double.POSITIVE_INFINITY;
        Double maxAttrMin = Double.POSITIVE_INFINITY;
        double sumMin = Double.POSITIVE_INFINITY;
        int count = 0;
        Double avgTotal = 0.0;
        Double sumTotal = 0.0;
        ArrayList<Geometry> geometryList = new ArrayList<>();
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                //System.out.print(feature.getID());
                //System.out.print(": ");

                minAttr.add(Long.parseLong(feature.getAttribute(minAttrName).toString()));
                maxAttr.add(Long.parseLong(feature.getAttribute(maxAttrName).toString()));
                avgAttr.add(Long.parseLong(feature.getAttribute(avgAttrName).toString()));
                varAttr.add(Long.parseLong(feature.getAttribute(varAttrName).toString()));
                sumAttr.add(Long.parseLong(feature.getAttribute(sumAttrName).toString()));
                distAttr.add(Long.parseLong(feature.getAttribute(distAttrName).toString()));
                fList.add(feature);
                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < sumMin){
                    sumMin = Long.parseLong(feature.getAttribute(sumAttrName).toString());
                }
                if (Long.parseLong(feature.getAttribute(avgAttrName).toString()) < 0){
                    System.out.println("AVG attribute contains negative value(s)");
                    return;
                }
                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < 0){
                    System.out.println("SUM attribute contains negative value(s)");
                    return;
                }
                if (Long.parseLong(feature.getAttribute(minAttrName).toString()) < minAttrMin){
                    minAttrMin = Double.parseDouble(feature.getAttribute(minAttrName).toString());
                }
                if (Long.parseLong(feature.getAttribute(minAttrName).toString()) > minAttrMax){
                    minAttrMax = Double.parseDouble(feature.getAttribute(minAttrName).toString());
                }
                if(Long.parseLong(feature.getAttribute(maxAttrName).toString()) > maxAttrMax){
                    maxAttrMax = Double.parseDouble(feature.getAttribute(maxAttrName).toString());
                }
                if(Long.parseLong(feature.getAttribute(maxAttrName).toString()) < maxAttrMin){
                    maxAttrMin = Double.parseDouble(feature.getAttribute(maxAttrName).toString());
                }
                count ++;
                avgTotal += Double.parseDouble(feature.getAttribute(avgAttrName).toString());
                sumTotal += Double.parseDouble(feature.getAttribute(sumAttrName).toString());
                //System.out.println(feature.getID());
                idList.add(Integer.parseInt(feature.getID().split("\\.")[1]) - 1);
                //System.out.print(feature.getID());
                //System.out.print(": ");
                //fList.add(feature);
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                geometryList.add(geometry);
                double cminx = geometry.getEnvelope().getCoordinates()[0].getX();
                double cminy = geometry.getEnvelope().getCoordinates()[0].getY();
                double cmaxx = geometry.getEnvelope().getCoordinates()[2].getX();
                double cmaxy = geometry.getEnvelope().getCoordinates()[2].getY();
                if (minX > cminx){
                    minX = cminx;
                }
                if (minY > cminy){
                    minY = cminy;
                }
                if (maxX < cmaxx){
                    maxX = cmaxx;
                }
                if (maxY < cmaxy){
                    maxY = cmaxy;
                }

                //idList.add(Integer.parseInt(feature.getID().split("\\.")[1]) - 1);
            }
            features.close();

            //sg.printIndex();
        }
        dataStore.dispose();
        avgTotal = avgTotal / count;


        //Feasibility checking
        // (1)The situation for AVG will change after removing infeasible areas
        // (2) Even when avgTotal does not lie with in avgAttrMin and avgAttrMax, the algorithm will
        if(minAttrMin > minAttrHigh|| minAttrMax < minAttrLow|| maxAttrMin > maxAttrHigh || maxAttrMax < maxAttrLow||  sumMin > sumAttrHigh || sumTotal < sumAttrLow || count < countLow){
            System.out.println("The constraint settings are infeasible. The program will terminate immediately.");
            System.exit(1);
        }
        if(varAttrLow > varAttrHigh){
            System.out.println("The constraint settings are infeasible. The program will terminate immediately.");
            System.exit(1);
        }
        if(minAttrMin > minAttrHigh){
            System.out.println("There is no area satisfying the MIN <=. The program will terminate immediately.");
            System.exit(1);
        }else if(minAttrMax < minAttrLow){
            System.out.println("There is no area satisfying the MIN >=. The program will terminate immediately.");
            System.exit(1);
        }

        double rookstartTime = System.currentTimeMillis()/ 1000.0;

        SpatialGrid sg = new SpatialGrid(minX, minY, maxX, maxY);

        HashMap<Integer, Set<Integer>> neighborMap = calculateNeighbors(geometryList);
        sg.setNeighbors(neighborMap);
        double rookendTime = System.currentTimeMillis()/ 1000.0;
        System.out.println("Rook time: " + (rookendTime - rookstartTime));

        double dataLoadTime = System.currentTimeMillis()/ 1000.0;
        System.out.println("Input size: " + distAttr.size());
        long [][] distanceMatrix = EMPTabu.pdist(distAttr);
        Date t = new Date();

        String fileNameSplit[] = fileName.split("/");
        String mapName = fileNameSplit[fileNameSplit.length-1].split("\\.")[0];
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

        String timeStamp = df.format(t);
        String folderName = "data/AlgorithmTesting/FaCT_" + mapName + "_MIN-" + minAttrLow + "-" + minAttrHigh + "_AVG-" + avgAttrLow + "-" + avgAttrHigh + "_SUM-" +sumAttrLow + "-" + sumAttrHigh + "-" + timeStamp;
        File folder = new File(folderName);
        folder.mkdirs();
        File settingFile = new File(folderName + "/Settings.csv");
        if(!settingFile.exists()){
            settingFile.createNewFile();
        }
        Writer settingWriter = new FileWriter(settingFile);
        settingWriter.write("Constraint, Attribute Name, Lower Bound, Upper Bound\n");
        settingWriter.write("Min, " + minAttrName + ", " + minAttrLow + ", " + minAttrHigh + "\n");
        settingWriter.write("Max, " + maxAttrName + ", " + maxAttrLow + ", " + maxAttrHigh + "\n");
        settingWriter.write("Avg, " + avgAttrName + ", " + avgAttrLow + ", " + avgAttrHigh + "\n");
        settingWriter.write("Var, " + varAttrName + ", " + varAttrLow + ", " + varAttrHigh + "\n");
        settingWriter.write("Sum, " + sumAttrName + ", " + sumAttrLow + ", " + sumAttrHigh + "\n");
        settingWriter.write("Count, "  + ", " + countLow + ", " + countHigh + "\n");
        settingWriter.write("Rand," + randFlag[0] + "," + randFlag[1] + "\n");
        settingWriter.close();
        File csvFile = new File(folderName + "/Result_" + mapName+"_"+ timeStamp + ".csv");
        //System.out.println(csvFile);
        if(!csvFile.exists()){
            csvFile.createNewFile();
        }
        Writer csvWriter = new FileWriter(csvFile);
        csvWriter.write("Iteration, Max P, Construction Time, Heuristic Time, Construction + Heuristic, Score Before Heuristic, Score After Heuristic, Score Difference，Unassigned areas\n");
        //RegionCollection rc = construction_phase_gene(population, income, 1, sg, idList,4000,Double.POSITIVE_INFINITY);

        String recordName = "data/emp_record/emp_" + mapName + "_" + minAttrName +  minAttrLow + "-" + minAttrHigh + "_" + maxAttrName +maxAttrLow + "-" + maxAttrHigh + "_"+ avgAttrName + avgAttrLow + "-" + avgAttrHigh + "_" +varAttrName + varAttrLow + "-" + varAttrHigh + "_" + sumAttrName +sumAttrLow + "-" + sumAttrHigh + "_" + countLow + "-" + countHigh;
        varLowerbound = varAttrLow;
        varUpprbound = varAttrHigh;
        for(int i = 0; i < numOfIts; i++){
            double constructionStart = System.currentTimeMillis() / 1000.0;
            RegionCollectionWithVariance rc = construction_phase_changeOfAttribute(idList, distAttr, sg,
                    minAttr,
                    minAttrLow,
                    minAttrHigh,

                    maxAttr,
                    maxAttrLow,
                    maxAttrHigh,

                    avgAttr,
                    avgAttrLow,
                    avgAttrHigh,

                    varAttr,
                    varAttrLow,
                    varAttrHigh,

                    sumAttr,
                    sumAttrLow,
                    sumAttrHigh,
                    countLow, countHigh, repeatQuery, recordName);
            int[] labelsCons = rc.getLabels();
            int unassingedConstruct = 0;
            for(int k = 0; k < labelsCons.length; k++){
                if(labelsCons[k] < 1){
                    unassingedConstruct += 1;
                }
            }
            System.out.println("Unassigned after construction: " + unassingedConstruct);
            double constructionEnd = System.currentTimeMillis() / 1000.0;
            double constructionDuration = constructionEnd - constructionStart;
            System.out.println("Construction time: " + constructionDuration);
            int totalReconstruct = 10;
            ArrayList<Integer> deconAreas = new ArrayList<>();
            for(int k = 0; k < totalReconstruct; k++){
                int areaDec = minAttr.size() / totalReconstruct * k;
                deconAreas.add(areaDec);
                //sumAttr.set(areaDec, 0L);
                double reConstructionStart = System.currentTimeMillis() / 1000.0;
                localReconstruction((int[])deconAreas.stream().mapToInt(j -> j).toArray(),
                        //rc.getLabels(),
                        idList,
                        rc,
                        sg,
                        distAttr,
                        minAttr,
                        minAttrLow,
                        minAttrHigh,

                        maxAttr,
                        maxAttrLow,
                        maxAttrHigh,

                        avgAttr,
                        avgAttrLow,
                        avgAttrHigh,

                        varAttr,
                        varAttrLow,
                        varAttrHigh,

                        sumAttr,
                        sumAttrLow,
                        sumAttrHigh,
                        countLow, countHigh,
                        true,
                        "localReconstruct", false
                );
                double reConstructionEnd = System.currentTimeMillis() / 1000.0;
                double reConstructDuration   = reConstructionEnd - reConstructionStart;
                //System.out.println("Construction time: " + constructionDuration);
                System.out.println("Re-Construction time: " + reConstructDuration);
            }


            //System.out.println("Time for construction phase:\n" + (constructionTime - rookendTime));

            int max_p = rc.getMax_p();
            //System.out.println("MaxP: " + max_p);
            //Map<Integer, Integer> regionSpatialAttr = rc.getRegionSpatialAttr();
        /*System.out.println("regionSpatialAttr after construction_phase:");
        for(Map.Entry<Integer, Integer> entry: regionSpatialAttr.entrySet()){
            Integer rid = entry.getKey();
            Integer rval = entry.getValue();
            System.out.print(rid + ": ");
            System.out.print(rval + " ");
            //System.out.println();
        }*/

            long totalWDS = EMPTabu.calculateWithinRegionDistance_var(rc.getRegionMap(), distanceMatrix);
            //System.out.println("totalWithinRegionDistance before tabu: \n" + totalWDS);
            int tabuLength = 10;
            int max_no_move = distAttr.size();
            //checkLabels(rc.getLabels(), rc.getRegionList());

            //System.out.println("Start tabu");
            if(debug){
                System.out.println(Arrays.toString(rc.getLabels()));
                System.out.println(Arrays.toString(rc.getLabels()));
                System.out.println(rc.getRegionMap().keySet());
                checkLabels_var(rc.getLabels(), rc.getRegionMap());
            }
            //TabuReturn tr = EMPTabu.performTabu_var(rc.getLabels(), rc.getRegionMap(), sg, EMPTabu.pdist((distAttr)), tabuLength, max_no_move, minAttr, maxAttr, varAttr, sumAttr, avgAttr);
            //int[] labels = tr.labels;
            //System.out.println(labels.length);
            //long WDSDifference = totalWDS - tr.WDS;
            //int[] labels = SimulatedAnnealing.performSimulatedAnnealing(rc.getLabels(), rc.getRegionList(), sg, pdist((distAttr)), minAttr, maxAttr, sumAttr, avgAttr);
            double endTime = System.currentTimeMillis()/ 1000.0;
            //System.out.println("MaxP: " + max_p);
            double heuristicDuration = endTime - constructionEnd;
            //System.out.println("Time for tabu(s): \n" + (endTime - constructionTime));
            // System.out.println("total time: \n" +(endTime - startTime));
            File f = new File(folderName +"/" + i + ".txt");
            if(!f.exists()){
                f.createNewFile();
            }
            int unassignedCount = 0;
            int[] labels = rc.getLabels();
            Writer w = new FileWriter(f);
            for( int j = 0; j < labels.length; j++){
                w.write(labels[j] + "\n");
                if(labels[j] < 1){
                    unassignedCount++;
                }
            }
            w.close();
            //System.out.println("minTime: " + minTime);
            //System.out.println("avgTime: " + avgTime);
            //System.out.println("sumTime: " + sumTime);

            System.out.println("Iteration: " + i);
            System.out.println("p: "+ max_p);
            System.out.println("Construction time: " + constructionDuration);
            System.out.println("Tabu search time: " + heuristicDuration);
            System.out.println("Heterogeneity score before Tabu: "  + totalWDS);
            System.out.println("Heterogeneity score after Tabu: " + totalWDS);
            System.out.println("Number of unassigned areas: " + unassignedCount + "\n");

            csvWriter.write(i + ", " + max_p + ", " + constructionDuration + ", " + heuristicDuration + ", " + (constructionDuration+heuristicDuration) + ", " + totalWDS + ", " + totalWDS + ", " + 0 + "," + unassignedCount +"," + minTime + "," + avgTime + "," + sumTime + "\n");
            csvWriter.flush();
            minTime = 0;
            avgTime = 0;
            sumTime = 0;

        }
        csvWriter.close();

        if(debug)
            System.out.println("End of setipnput");

    }

    public static RegionCollectionWithVariance construction_phase_LocalReconstruct(ArrayList<Integer> idList,
                                                                                    ArrayList<Long> disAttr,
                                                                                    SpatialGrid r,
                                                                                    ArrayList<Long> minAttr,
                                                                                    Double minLowerBound,
                                                                                    Double minUpperBound,

                                                                                    ArrayList<Long> maxAttr,
                                                                                    Double maxLowerBound,
                                                                                    Double maxUpperBound,

                                                                                    ArrayList<Long> avgAttr,
                                                                                    Double avgLowerBound,
                                                                                    Double avgUpperBound,

                                                                                    ArrayList<Long> varAttr,
                                                                                    Double varLowerBound,
                                                                                    Double varUpperBound,

                                                                                    ArrayList<Long> sumAttr,
                                                                                    Double sumLowerBound,
                                                                                    Double sumUpperBound,

                                                                                    Double countLowerBound,
                                                                                    Double countUpperBound, boolean repeatQuery, Set<Integer> reconstructAreas, String recordName) {
        int maxIt = 100;
        RegionWithVariance.setRange(minLowerBound, minUpperBound, maxLowerBound, maxUpperBound, avgLowerBound, avgUpperBound, varLowerBound, varUpperBound, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);



        //boolean var_debug = true;

        //RegionWithVariance.setRange(minLowerBound, minUpperBound, maxLowerBound, maxUpperBound, avgLowerBound, avgUpperBound, varianceLoweBound, varianceUpperBound, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
        ObjectInputStream ois = null;
        boolean differentConstraint = false;
        int stateOfChange = 0;
        if(repeatQuery){
            try{
                ois = new ObjectInputStream(new FileInputStream(recordName + "/constraints.txt"));
                ArrayList<Long> preMinAttr = (ArrayList<Long>) ois.readObject();
                Double preMinLowerBound = (Double) ois.readObject();
                Double preMinUpperBound = (Double) ois.readObject();
                ArrayList<Long> preMaxAttr = (ArrayList<Long>) ois.readObject();
                Double preMaxLowerBound = (Double) ois.readObject();
                Double preMaxUpperBound = (Double) ois.readObject();
                ArrayList<Long> preAvgAttr = (ArrayList<Long>) ois.readObject();
                Double preAvgLowerBound = (Double) ois.readObject();
                Double preAvgUpperBound = (Double) ois.readObject();
                ArrayList<Long> preVarAttr = (ArrayList<Long>) ois.readObject();
                Double preVarLowerBound = (Double) ois.readObject();
                Double preVarUpperBound = (Double) ois.readObject();
                ArrayList<Long> preSumAttr = (ArrayList<Long>) ois.readObject();
                Double preSumLowerBound = (Double) ois.readObject();
                Double preSumUpperBound = (Double) ois.readObject();
                Double preCountLowerBound = (Double) ois.readObject();
                Double preCountUpperBound = (Double) ois.readObject();
                if(preMinAttr.equals(minAttr) && preMaxAttr.equals(maxAttr) && preMinLowerBound.equals(minLowerBound) && preMinUpperBound.equals(minUpperBound) && preMaxLowerBound .equals( maxLowerBound) && preMaxUpperBound .equals( maxUpperBound)){
                    stateOfChange = 1;
                }else{
                    System.out.println("Different MIN/MAX");
                    differentConstraint = true;
                }
                if(!differentConstraint){
                    if(preVarAttr.equals(varAttr) && preVarLowerBound.equals(varLowerBound) && preVarUpperBound.equals(varUpperBound)){
                        stateOfChange = 2;
                    }else{
                        System.out.println("Different VAR");
                        differentConstraint = true;
                    }
                }
                if(!differentConstraint){
                    if(preSumAttr.equals(sumAttr) && preSumUpperBound.equals(sumUpperBound) && preSumLowerBound.equals(sumLowerBound) &&  preCountUpperBound.equals(countUpperBound) && preCountLowerBound.equals(countLowerBound)){
                        stateOfChange = 3;
                    }else{
                        System.out.println("Different SUM/COUNT");
                        differentConstraint = true;
                    }
                }


            }catch(EOFException e){//Previous result not found
                e.printStackTrace();
                differentConstraint = true;
                stateOfChange = 0;

            }catch(FileNotFoundException e) {
                e.printStackTrace();
                differentConstraint = true;
                stateOfChange = 0;
            }catch(Exception e){

                e.printStackTrace();
            }
        }

        if(var_debug)
            System.out.println("Different constraint check: " + differentConstraint + " " + stateOfChange);

        ObjectOutputStream oos = null;
        if(repeatQuery && !differentConstraint){
            try{
                //ObjectInputStream ois = null;
                ois = new ObjectInputStream(new FileInputStream(recordName + "/bestCollection.txt"));
                RegionCollectionWithVariance bc = (RegionCollectionWithVariance)ois.readObject();
                return bc;

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(repeatQuery && differentConstraint){

            try{
                File recordFolder = new File(recordName);
                if(!recordFolder.exists()){
                    recordFolder.mkdirs();
                }

                oos = new ObjectOutputStream(new FileOutputStream(recordName + "/constraints.txt"));
                oos.writeObject(minAttr);
                oos.writeObject(minLowerBound);
                oos.writeObject(minUpperBound);
                oos.writeObject(maxAttr);
                oos.writeObject(maxLowerBound);
                oos.writeObject(maxUpperBound);
                oos.writeObject(avgAttr);
                oos.writeObject(avgLowerBound);
                oos.writeObject(avgUpperBound);
                oos.writeObject(varAttr);
                oos.writeObject(varLowerBound);
                oos.writeObject(varUpperBound);
                oos.writeObject(sumAttr);
                oos.writeObject(sumLowerBound);
                oos.writeObject(sumUpperBound);
                oos.writeObject(countLowerBound);
                oos.writeObject(countUpperBound);

            }catch(Exception e){
                e.printStackTrace();
            }
        }
        int max_p = 0;

        RegionCollectionWithVariance bestCollection = null;
        double minStart = System.currentTimeMillis() / 1000.0;

        ArrayList<Integer> areas = new ArrayList<Integer>();
        areas.addAll(idList);
        int min_unAssigned = maxAttr.size();
        //First step: Filtering and seeding for MIN, MAX.
        Pair<int[], ArrayList<Integer>> minmaxResult = null;
        int[] minmaxlabels = null;
        ArrayList<Integer> minmaxseedAreas =null;
        if(repeatQuery && (differentConstraint && stateOfChange == 1)){
            try{
                ois = new ObjectInputStream(new FileInputStream(recordName + "/minmax.txt"));
                minmaxResult = (Pair<int[], ArrayList<Integer>>) ois.readObject();
                minmaxlabels = minmaxResult.getValue0();
                minmaxseedAreas = minmaxResult.getValue1();
                //minmaxseedAreas = (ArrayList<Integer>)ois.readObject();
            }catch(Exception e){
                e.printStackTrace();
            }

        }else if(repeatQuery && (differentConstraint && stateOfChange > 1)){}
        else{
            minmaxResult = filtering_and_seeding(areas, minAttr, minLowerBound, minUpperBound, maxAttr, maxLowerBound, maxUpperBound, sumAttr, sumUpperBound);
            if(repeatQuery){
                try{
                    oos = new ObjectOutputStream(new FileOutputStream(recordName + "/minmax.txt", true));
                    oos.writeObject(minmaxResult);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

            minmaxlabels = minmaxResult.getValue0();
            minmaxseedAreas = minmaxResult.getValue1();

        }

        for(int i = 0; i < minmaxlabels.length; i++){
            if(!reconstructAreas.contains(i)){
                minmaxlabels[i] = -5; //Regions untouched
            }
        }
        ArrayList<Integer> seedsNotConsidered = new ArrayList<Integer>();
        for(Integer seed: minmaxseedAreas){
            if(!reconstructAreas.contains(seed)){
                seedsNotConsidered.add(seed);
            }
        }
        minmaxseedAreas.removeAll(seedsNotConsidered);

        //System.out.println("Limited seeds " + minmaxlabels[0]);


        double minEnd = System.currentTimeMillis() / 1000.0;
        minTime = minTime + (minEnd - minStart);
        ObjectInputStream varOis = null;
        ObjectInputStream avgOis = null;
        ObjectInputStream sumcountOis = null;
        //ObjectOutputStream sumOos = null;
        boolean hasVar = false;
        boolean hasAvg = false;
        if(!(varLowerBound.equals(-Double.POSITIVE_INFINITY) && varUpperBound.equals(Double.POSITIVE_INFINITY))){
            hasVar = true;
        }
        if(!(avgLowerBound.equals(-Double.POSITIVE_INFINITY) && avgUpperBound.equals(Double.POSITIVE_INFINITY))){
            hasAvg = true;
        }
        if(var_debug)
            System.out.println("Has var: " + hasVar + " has avg " + hasAvg);
        if(hasVar && hasAvg){
            System.out.println("Please do not specify the variance and the average constraint at the same time");
            System.exit(12138);
        }
        try{
            if(repeatQuery){
                if(hasVar)
                    varOis =  new ObjectInputStream(new FileInputStream(recordName + "/var.txt"));
                if(hasAvg)
                    avgOis = new ObjectInputStream(new FileInputStream(recordName + "/avg.txt"));
                if(!differentConstraint)
                    sumcountOis = new ObjectInputStream(new FileInputStream(recordName + "/sumcount.txt"));
            }

            //sumOos = new ObjectOutputStream(new FileOutputStream(recordName + "/sumcount.txt", true));

        }catch (Exception e){
            e.printStackTrace();
        }
        //ObjectOutputStream sumOos = null;
        for (int it = 0; it < maxIt; it++) {

            int[] labels = null;
            ArrayList<Integer> seedAreas = null;
            if(!repeatQuery || differentConstraint && stateOfChange <= 1){
                labels = minmaxlabels.clone();
                seedAreas = new ArrayList<>(minmaxseedAreas);
                //System.out.println(seedAreas);
            }
            Map<Integer, RegionWithVariance> regionList = null;

            if(var_debug)
                System.out.println("Before initialiazation");

            if(hasVar){
                Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> varInitialization = null;

                if(repeatQuery &&(differentConstraint && stateOfChange == 2)){
                    try{
                        //ois = new ObjectInputStream(new FileInputStream(recordName + "/var.txt"));
                        varInitialization = ( Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>>)varOis.readObject();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    labels = varInitialization.getValue0();
                    regionList = varInitialization.getValue1();
                }else{
                    varInitialization = region_initialization_var(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, varAttr, varLowerBound, varUpperBound, sumAttr);
                    try{
                        oos = new ObjectOutputStream(new FileOutputStream(recordName + "/var.txt", true));
                        oos.writeObject(varInitialization);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    labels = varInitialization.getValue0();
                    regionList = varInitialization.getValue1();
                }
                //Second step: AVG and MIN MAX revisit
                //Quartet<int[], Map<Integer, Region>, List<Integer>, List<Integer>> avgInitialization = region_initialization(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, avgLowerBound, avgUpperBound, sumAttr);
                //Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> varInitialization = region_initialization_var(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, varAttr, varLowerBound, varUpperBound, sumAttr);
                if(var_debug)
                    System.out.println("After initialization");

            }else{
                Quartet<int[], Map<Integer, RegionWithVariance>, List<Integer>, List<Integer>> avgInitialization = null;
                if(repeatQuery &&(differentConstraint && stateOfChange == 2)){
                    try{
                        //ois = new ObjectInputStream(new FileInputStream(recordName + "/var.txt"));
                        avgInitialization =  (Quartet<int[], Map<Integer, RegionWithVariance>, List<Integer>, List<Integer>>) avgOis.readObject();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    labels = avgInitialization.getValue0();
                    regionList = avgInitialization.getValue1();
                }else if(repeatQuery &&(differentConstraint && stateOfChange > 2)){}
                else{
                    avgInitialization = region_initialization(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, avgLowerBound, avgUpperBound, varAttr, varLowerBound, varUpperBound, sumAttr);
                    try{
                        if(repeatQuery){
                            File f = new File(recordName + "/avg.txt");
                            if(!f.exists() || f.length() == 0){
                                oos = new ObjectOutputStream(new FileOutputStream(recordName + "/avg.txt"));
                                oos.writeObject(avgInitialization);
                            }else{
                                oos = new EMPObjectOutputStream(new FileOutputStream(recordName + "/avg.txt", true));
                                oos.writeObject(avgInitialization);
                            }
                        }

                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    labels = avgInitialization.getValue0();
                    regionList = avgInitialization.getValue1();
                }
                //Second step: AVG and MIN MAX revisit
                //Quartet<int[], Map<Integer, Region>, List<Integer>, List<Integer>> avgInitialization = region_initialization(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, avgLowerBound, avgUpperBound, sumAttr);
                //Triplet<int[], Map<Integer, RegionWithVariance>, List<Integer>> varInitialization = region_initialization_var(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, varAttr, varLowerBound, varUpperBound, sumAttr);
                if(var_debug)
                    System.out.println("After initialization");

            }

            if(debug){
                checkLabels_var(labels, regionList);
            }
            double avgEnd = System.currentTimeMillis() / 1000.0;
            avgTime += (avgEnd - minEnd);
            //Start sum and count
            if (var_debug) {
                System.out.println("-------------");
                System.out.println("P after AVG: " + regionList.size());
                int unassignedCount = 0;
                for(int i = 0; i < labels.length; i++){
                    System.out.print(labels[i] + " ");
                    if(labels[i] < 1){
                        unassignedCount++;
                    }
                }

                System.out.println("No of unassigned areas after var" + unassignedCount);
                System.out.println("-------------");

            }
            /*if (var_debug) {
                for (Map.Entry<Integer, RegionWithVariance> regionEntry : regionList.entrySet()) {
                    // if (!regionEntry.getValue().satisfiable()) {
                    //idToBeRemoved.add(regionEntry.getValue().getId());
                    //regionList_MaxP_AVG.add(regionEntry.getKey());



                    System.out.println("Id to be removed:" + regionEntry.getValue().getId());
                    System.out.println("Min:" + regionEntry.getValue().getMin());
                    System.out.println("Max:" + regionEntry.getValue().getMax());
                    System.out.println("Avg:" + regionEntry.getValue().getAverage());
                    System.out.println("Var:" +  regionEntry.getValue().getVariance());
                    System.out.println("Sum:" + regionEntry.getValue().getSum());

                    System.out.println("Count:" + regionEntry.getValue().getCount());
                    System.out.println("Areas: " + regionEntry.getValue().getAreaList());
                    System.out.println("Satisfiable:" + regionEntry.getValue().satisfiable());
                }
                //System.out.print(regionEntry.getKey() + " ");

            }*/
            Pair<int[], Map<Integer, RegionWithVariance>> result = null;
            if(repeatQuery &&(!differentConstraint || stateOfChange > 2)){
                try{
                    //sumcountOis = new ObjectInputStream(new FileInputStream(recordName + "/sumcount.txt"));
                    result = ( Pair<int[], Map<Integer, RegionWithVariance>>)sumcountOis.readObject();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }else{
                result = sumcount_construction_var(labels, regionList, r, minAttr, maxAttr, avgAttr, varAttr, sumAttr, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
                try{

                    if(repeatQuery){
                        File f = new File(recordName + "/sumcount.txt");
                        if(!f.exists() || f.length() == 0){
                            oos = new ObjectOutputStream(new FileOutputStream(recordName + "/sumcount.txt"));
                            oos.writeObject(result);
                        }else{
                            oos = new EMPObjectOutputStream(new FileOutputStream(recordName + "/sumcount.txt", true));
                            oos.writeObject(result);
                        }
                    }

                    //oos = new ObjectOutputStream(new FileOutputStream(recordName + "/sumcount.txt", true));
                    //oos.writeObject(result);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            //Pair<int[], Map<Integer, RegionWithVariance>> result = sumcount_construction_var(labels, regionList, r, minAttr, maxAttr, avgAttr, varAttr, sumAttr, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
            labels = result.getValue0();
            regionList = result.getValue1();
            double sumEnd = System.currentTimeMillis() / 1000.0;
            sumTime += (sumEnd - avgEnd);
            //System.out.println("Finish SUM");
            /*if(repeatQuery){
                ObjectOutputStream oos = null;
                try{
                    oos = new ObjectOutputStream(new FileOutputStream("./prevResults.txt"));
                    oos.writeObject(recordName);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }*/
            if (var_debug) {
                for (Map.Entry<Integer, RegionWithVariance> regionEntry : regionList.entrySet()) {
                    // if (!regionEntry.getValue().satisfiable()) {
                    //idToBeRemoved.add(regionEntry.getValue().getId());
                    //regionList_MaxP_AVG.add(regionEntry.getKey());



                    System.out.println("Id to be removed:" + regionEntry.getValue().getId());
                    System.out.println("Min:" + regionEntry.getValue().getMin());
                    System.out.println("Max:" + regionEntry.getValue().getMax());
                    System.out.println("Avg:" + regionEntry.getValue().getAverage());
                    System.out.println("Var:" +  regionEntry.getValue().getVariance());
                    System.out.println("Sum:" + regionEntry.getValue().getSum());

                    System.out.println("Count:" + regionEntry.getValue().getCount());
                    System.out.println("Areas: " + regionEntry.getValue().getAreaList());
                    System.out.println("Satisfiable:" + regionEntry.getValue().satisfiable());
                }
                //System.out.print(regionEntry.getKey() + " ");

            }

            /*for(Map.Entry<Integer, RegionNew> entry: regionList.entrySet()){
                System.out.println( entry.getValue().getSum() + " " + entry.getValue().getCount());
            }*/

            int unAssignedCount = 0;
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] <= 0)
                    unAssignedCount++;
            }
            //System.out.println("Distance for this regionList" + calculateWithinRegionDistance(regionList, distanceMatrix));
            if (regionList.size() > max_p || (regionList.size() == max_p && unAssignedCount < min_unAssigned)) {
                max_p = regionList.size();
                min_unAssigned = unAssignedCount;
                bestCollection = new RegionCollectionWithVariance(regionList.size(), labels, regionList);
            }

            if (debug) {
                Map<Integer, RegionWithVariance> rcn = bestCollection.getRegionMap();
                for (RegionWithVariance rn : rcn.values()) {
                    if (!rn.satisfiable()) {
                        System.out.println("Region " + rn.getId() + " not satisfiable!");
                        System.exit(125);
                    }
                }
            }


        }
        try{
            if(repeatQuery){
                oos = new ObjectOutputStream(new FileOutputStream(recordName + "/bestCollection.txt"));
                oos.writeObject(bestCollection);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return bestCollection;
    }
    public static void  set_input_IteratedGreedy(String fileName,
                                                   String minAttrName,
                                                   Double minAttrLow,
                                                   Double minAttrHigh,
                                                   String maxAttrName,
                                                   Double maxAttrLow,
                                                   Double maxAttrHigh,
                                                   String avgAttrName,
                                                   Double avgAttrLow,
                                                   Double avgAttrHigh,
                                                   String varAttrName,
                                                   Double varAttrLow,
                                                   Double varAttrHigh,
                                                   String sumAttrName,
                                                   Double sumAttrLow,
                                                   Double sumAttrHigh,
                                                   Double countLow,
                                                   Double countHigh,
                                                   String distAttrName,
                                                   boolean repeatQuery,
                                                 int deconStrength,
                                                 int deconItrs

    ) throws Exception {
        double startTime = System.currentTimeMillis()/ 1000.0;
        File file = new File(fileName);
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source =
                dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
        ArrayList<Long> minAttr = new ArrayList<>();
        ArrayList<Long> maxAttr = new ArrayList<>();
        ArrayList<Long> avgAttr = new ArrayList<>();
        ArrayList<Long> varAttr = new ArrayList<>();
        ArrayList<Long> sumAttr = new ArrayList<>();
        ArrayList<Long> distAttr = new ArrayList<>();

        ArrayList<SimpleFeature> fList = new ArrayList<>();
        ArrayList<Integer> idList = new ArrayList<>();
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = - Double.POSITIVE_INFINITY, maxY = -Double.POSITIVE_INFINITY;
        Double minAttrMin = Double.POSITIVE_INFINITY;
        Double minAttrMax = -Double.POSITIVE_INFINITY;
        Double maxAttrMax = -Double.POSITIVE_INFINITY;
        Double maxAttrMin = Double.POSITIVE_INFINITY;
        double sumMin = Double.POSITIVE_INFINITY;
        int count = 0;
        Double avgTotal = 0.0;
        Double sumTotal = 0.0;
        ArrayList<Geometry> geometryList = new ArrayList<>();
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                //System.out.print(feature.getID());
                //System.out.print(": ");

                minAttr.add(Long.parseLong(feature.getAttribute(minAttrName).toString()));
                maxAttr.add(Long.parseLong(feature.getAttribute(maxAttrName).toString()));
                avgAttr.add(Long.parseLong(feature.getAttribute(avgAttrName).toString()));
                varAttr.add(Long.parseLong(feature.getAttribute(varAttrName).toString()));
                sumAttr.add(Long.parseLong(feature.getAttribute(sumAttrName).toString()));
                distAttr.add(Long.parseLong(feature.getAttribute(distAttrName).toString()));
                fList.add(feature);
                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < sumMin){
                    sumMin = Long.parseLong(feature.getAttribute(sumAttrName).toString());
                }
                if (Long.parseLong(feature.getAttribute(avgAttrName).toString()) < 0){
                    System.out.println("AVG attribute contains negative value(s)");
                    return;
                }
                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < 0){
                    System.out.println("SUM attribute contains negative value(s)");
                    return;
                }
                if (Long.parseLong(feature.getAttribute(minAttrName).toString()) < minAttrMin){
                    minAttrMin = Double.parseDouble(feature.getAttribute(minAttrName).toString());
                }
                if (Long.parseLong(feature.getAttribute(minAttrName).toString()) > minAttrMax){
                    minAttrMax = Double.parseDouble(feature.getAttribute(minAttrName).toString());
                }
                if(Long.parseLong(feature.getAttribute(maxAttrName).toString()) > maxAttrMax){
                    maxAttrMax = Double.parseDouble(feature.getAttribute(maxAttrName).toString());
                }
                if(Long.parseLong(feature.getAttribute(maxAttrName).toString()) < maxAttrMin){
                    maxAttrMin = Double.parseDouble(feature.getAttribute(maxAttrName).toString());
                }
                count ++;
                avgTotal += Double.parseDouble(feature.getAttribute(avgAttrName).toString());
                sumTotal += Double.parseDouble(feature.getAttribute(sumAttrName).toString());
                //System.out.println(feature.getID());
                idList.add(Integer.parseInt(feature.getID().split("\\.")[1]) - 1);
                //System.out.print(feature.getID());
                //System.out.print(": ");
                //fList.add(feature);
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                geometryList.add(geometry);
                double cminx = geometry.getEnvelope().getCoordinates()[0].getX();
                double cminy = geometry.getEnvelope().getCoordinates()[0].getY();
                double cmaxx = geometry.getEnvelope().getCoordinates()[2].getX();
                double cmaxy = geometry.getEnvelope().getCoordinates()[2].getY();
                if (minX > cminx){
                    minX = cminx;
                }
                if (minY > cminy){
                    minY = cminy;
                }
                if (maxX < cmaxx){
                    maxX = cmaxx;
                }
                if (maxY < cmaxy){
                    maxY = cmaxy;
                }

                //idList.add(Integer.parseInt(feature.getID().split("\\.")[1]) - 1);
            }
            features.close();

            //sg.printIndex();
        }
        dataStore.dispose();
        avgTotal = avgTotal / count;


        //Feasibility checking
        // (1)The situation for AVG will change after removing infeasible areas
        // (2) Even when avgTotal does not lie with in avgAttrMin and avgAttrMax, the algorithm will
        if(minAttrMin > minAttrHigh|| minAttrMax < minAttrLow|| maxAttrMin > maxAttrHigh || maxAttrMax < maxAttrLow||  sumMin > sumAttrHigh || sumTotal < sumAttrLow || count < countLow){
            System.out.println("The constraint settings are infeasible. The program will terminate immediately.");
            System.exit(1);
        }
        if(varAttrLow > varAttrHigh){
            System.out.println("The constraint settings are infeasible. The program will terminate immediately.");
            System.exit(1);
        }
        if(minAttrMin > minAttrHigh){
            System.out.println("There is no area satisfying the MIN <=. The program will terminate immediately.");
            System.exit(1);
        }else if(minAttrMax < minAttrLow){
            System.out.println("There is no area satisfying the MIN >=. The program will terminate immediately.");
            System.exit(1);
        }

        double rookstartTime = System.currentTimeMillis()/ 1000.0;

        SpatialGrid sg = new SpatialGrid(minX, minY, maxX, maxY);

        HashMap<Integer, Set<Integer>> neighborMap = calculateNeighbors(geometryList);
        sg.setNeighbors(neighborMap);
        double rookendTime = System.currentTimeMillis()/ 1000.0;
        System.out.println("Rook time: " + (rookendTime - rookstartTime));

        double dataLoadTime = System.currentTimeMillis()/ 1000.0;
        System.out.println("Input size: " + distAttr.size());
        long [][] distanceMatrix = EMPTabu.pdist(distAttr);
        Date t = new Date();

        String fileNameSplit[] = fileName.split("/");
        String mapName = fileNameSplit[fileNameSplit.length-1].split("\\.")[0];
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

        String timeStamp = df.format(t);
        String folderName = "data/AlgorithmTesting/FaCT_" + mapName + "_MIN-" + minAttrLow + "-" + minAttrHigh + "_AVG-" + avgAttrLow + "-" + avgAttrHigh + "_SUM-" +sumAttrLow + "-" + sumAttrHigh + "-" + timeStamp;
        File folder = new File(folderName);
        folder.mkdirs();
        File settingFile = new File(folderName + "/Settings.csv");
        if(!settingFile.exists()){
            settingFile.createNewFile();
        }
        Writer settingWriter = new FileWriter(settingFile);
        settingWriter.write("Constraint, Attribute Name, Lower Bound, Upper Bound\n");
        settingWriter.write("Min, " + minAttrName + ", " + minAttrLow + ", " + minAttrHigh + "\n");
        settingWriter.write("Max, " + maxAttrName + ", " + maxAttrLow + ", " + maxAttrHigh + "\n");
        settingWriter.write("Avg, " + avgAttrName + ", " + avgAttrLow + ", " + avgAttrHigh + "\n");
        settingWriter.write("Var, " + varAttrName + ", " + varAttrLow + ", " + varAttrHigh + "\n");
        settingWriter.write("Sum, " + sumAttrName + ", " + sumAttrLow + ", " + sumAttrHigh + "\n");
        settingWriter.write("Count, "  + ", " + countLow + ", " + countHigh + "\n");
        settingWriter.write("Rand," + randFlag[0] + "," + randFlag[1] + "\n");
        settingWriter.close();
        File csvFile = new File(folderName + "/Result_" + mapName+"_"+ timeStamp + ".csv");
        //System.out.println(csvFile);
        if(!csvFile.exists()){
            csvFile.createNewFile();
        }
        Writer csvWriter = new FileWriter(csvFile);
        csvWriter.write("Iteration, Max P, Construction Time, Heuristic Time, Construction + Heuristic, Score Before Heuristic, Score After Heuristic, Score Difference，Unassigned areas\n");
        //RegionCollection rc = construction_phase_gene(population, income, 1, sg, idList,4000,Double.POSITIVE_INFINITY);

        String recordName = "data/emp_record/emp_" + mapName + "_" + minAttrName +  minAttrLow + "-" + minAttrHigh + "_" + maxAttrName +maxAttrLow + "-" + maxAttrHigh + "_"+ avgAttrName + avgAttrLow + "-" + avgAttrHigh + "_" +varAttrName + varAttrLow + "-" + varAttrHigh + "_" + sumAttrName +sumAttrLow + "-" + sumAttrHigh + "_" + countLow + "-" + countHigh;
        varLowerbound = varAttrLow;
        varUpprbound = varAttrHigh;
        for(int i = 0; i < numOfIts; i++){
            double constructionStart = System.currentTimeMillis() / 1000.0;
            RegionCollectionWithVariance rc = construction_phase_changeOfAttribute(idList, distAttr, sg,
                    minAttr,
                    minAttrLow,
                    minAttrHigh,

                    maxAttr,
                    maxAttrLow,
                    maxAttrHigh,

                    avgAttr,
                    avgAttrLow,
                    avgAttrHigh,

                    varAttr,
                    varAttrLow,
                    varAttrHigh,

                    sumAttr,
                    sumAttrLow,
                    sumAttrHigh,
                    countLow, countHigh, repeatQuery, recordName);
            int[] labelsCons = rc.getLabels();
            int unassingedConstruct = 0;
            for(int k = 0; k < labelsCons.length; k++){
                if(labelsCons[k] < 1){
                    unassingedConstruct += 1;
                }
            }
            System.out.println("Unassigned after construction: " + unassingedConstruct);
            System.out.println("P after construction: " + rc.getMax_p());
            double constructionEnd = System.currentTimeMillis() / 1000.0;
            double constructionDuration = constructionEnd - constructionStart;
            System.out.println("Construction time: " + constructionDuration);
            int totalReconstruct = deconItrs;


            Random rand = new Random();
            double reconstructionTotal = 0;
            int upperBound = minAttr.size();
            for(int k = 0; k < totalReconstruct; k++){
                ArrayList<Integer> deconAreas = new ArrayList<>();
                for(int j = 0; j < deconStrength;j++){
                    int areaDec = rand.nextInt(upperBound);
                    deconAreas.add(areaDec);
                }
                //sumAttr.set(areaDec, 0L);
                double reConstructionStart = System.currentTimeMillis() / 1000.0;
                rc = localReconstruction((int[])deconAreas.stream().mapToInt(j -> j).toArray(),
                //int[] deconAreaArray = {areaDec};
                //rc = localReconstruction(deconAreaArray,
                        //rc.getLabels(),
                        idList,
                        rc,
                        sg,
                        distAttr,
                        minAttr,
                        minAttrLow,
                        minAttrHigh,

                        maxAttr,
                        maxAttrLow,
                        maxAttrHigh,

                        avgAttr,
                        avgAttrLow,
                        avgAttrHigh,

                        varAttr,
                        varAttrLow,
                        varAttrHigh,

                        sumAttr,
                        sumAttrLow,
                        sumAttrHigh,
                        countLow, countHigh,
                        true,
                        "localReconstruct", true
                );
                double reConstructionEnd = System.currentTimeMillis() / 1000.0;
                double reConstructDuration   = reConstructionEnd - reConstructionStart;
                reconstructionTotal += reConstructDuration;
                //System.out.println("Construction time: " + constructionDuration);
                System.out.println("Re-Construction time: " + reConstructDuration);
            }


            //System.out.println("Time for construction phase:\n" + (constructionTime - rookendTime));

            int max_p = rc.getMax_p();
            //System.out.println("MaxP: " + max_p);
            //Map<Integer, Integer> regionSpatialAttr = rc.getRegionSpatialAttr();
        /*System.out.println("regionSpatialAttr after construction_phase:");
        for(Map.Entry<Integer, Integer> entry: regionSpatialAttr.entrySet()){
            Integer rid = entry.getKey();
            Integer rval = entry.getValue();
            System.out.print(rid + ": ");
            System.out.print(rval + " ");
            //System.out.println();
        }*/

            long totalWDS = EMPTabu.calculateWithinRegionDistance_var(rc.getRegionMap(), distanceMatrix);
            //System.out.println("totalWithinRegionDistance before tabu: \n" + totalWDS);
            int tabuLength = 10;
            int max_no_move = distAttr.size();
            //checkLabels(rc.getLabels(), rc.getRegionList());

            //System.out.println("Start tabu");
            if(debug){
                System.out.println(Arrays.toString(rc.getLabels()));
                System.out.println(Arrays.toString(rc.getLabels()));
                System.out.println(rc.getRegionMap().keySet());
                checkLabels_var(rc.getLabels(), rc.getRegionMap());
            }
            //TabuReturn tr = EMPTabu.performTabu_var(rc.getLabels(), rc.getRegionMap(), sg, EMPTabu.pdist((distAttr)), tabuLength, max_no_move, minAttr, maxAttr, varAttr, sumAttr, avgAttr);
            //int[] labels = tr.labels;
            //System.out.println(labels.length);
            //long WDSDifference = totalWDS - tr.WDS;
            //int[] labels = SimulatedAnnealing.performSimulatedAnnealing(rc.getLabels(), rc.getRegionList(), sg, pdist((distAttr)), minAttr, maxAttr, sumAttr, avgAttr);
            double endTime = System.currentTimeMillis()/ 1000.0;
            //System.out.println("MaxP: " + max_p);
            double heuristicDuration = endTime - constructionEnd;
            //System.out.println("Time for tabu(s): \n" + (endTime - constructionTime));
            // System.out.println("total time: \n" +(endTime - startTime));
            File f = new File(folderName +"/" + i + ".txt");
            if(!f.exists()){
                f.createNewFile();
            }
            int unassignedCount = 0;
            int[] labels = rc.getLabels();
            Writer w = new FileWriter(f);
            for( int j = 0; j < labels.length; j++){
                w.write(labels[j] + "\n");
                if(labels[j] < 1){
                    unassignedCount++;
                }
            }
            w.close();
            //System.out.println("minTime: " + minTime);
            //System.out.println("avgTime: " + avgTime);
            //System.out.println("sumTime: " + sumTime);

            System.out.println("Iteration: " + i);
            System.out.println("p: "+ max_p);
            System.out.println("Construction time: " + constructionDuration);
            System.out.println("Tabu search time: " + heuristicDuration);
            System.out.println("Reconstruction total time: " + reconstructionTotal);
            System.out.println("Heterogeneity score before Tabu: "  + totalWDS);
            System.out.println("Heterogeneity score after Tabu: " + totalWDS);
            System.out.println("Number of unassigned areas: " + unassignedCount + "\n");

            csvWriter.write(i + ", " + max_p + ", " + constructionDuration + ", " + heuristicDuration + ", " + (constructionDuration+heuristicDuration) + ", " + totalWDS + ", " + totalWDS + ", " + 0 + "," + unassignedCount +"," + minTime + "," + avgTime + "," + sumTime + "\n");
            csvWriter.flush();
            minTime = 0;
            avgTime = 0;
            sumTime = 0;

        }
        csvWriter.close();

        if(debug)
            System.out.println("End of setipnput");

    }

}


