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
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;

public class EMP_IterativeGreedy implements RegionalizationMethod {
    static boolean debug = false;
    static boolean check_p_afterAVG = false;
    static boolean labelCheck = true;
    static int numOfIts = 3;
    static int randFlag[] = {1,1};
    static int rand[] = {0, 1, 2};
    static String rands[] = {"S", "R", "B"};
    static int mergeLimit = 3;
    static String testName = "RandWithMergeLimit";

    static double minTime = 0;
    static double avgTime = 0;
    static double sumTime = 0;

    RegionCollection constructionPartition;
    TabuReturn finalPartition;

    @Override
    public int getP() {
        return constructionPartition.getMax_p();
    }

    @Override
    public int[] getRegionLabels() {
        return finalPartition.labels;
    }
    @Override
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
        constructionPartition = construction_phase_breakdown(idList, disAttr, sg, sumAttr, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, sumAttr, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, sumAttr, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, sumAttr, threshold, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        finalPartition = EMPTabu.performTabu(constructionPartition.getLabels(), constructionPartition.getRegionMap(), sg, EMPTabu.pdist((disAttr)), tabuLength, max_no_move, sumAttr, sumAttr, sumAttr, sumAttr);

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
        constructionPartition = construction_phase_breakdown(idList, disAttr, sg, minAttr, minLowerBound, minUpperBound, maxAttr, maxLowerBound, maxUpperBound, avgAttr, avgLowerBound, avgUpperBound, sumAttr, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
        finalPartition = EMPTabu.performTabu(constructionPartition.getLabels(), constructionPartition.getRegionMap(), sg, EMPTabu.pdist((disAttr)), tabuLength, max_no_move, minAttr, maxAttr, sumAttr, avgAttr);

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
    public static Quartet<int[], Map<Integer, Region>, List<Integer>, List<Integer>>  region_initialization(int[] labels,
                                                                                                            ArrayList<Integer> seedAreas,
                                                                                                            SpatialGrid r,
                                                                                                            ArrayList<Long> minAttr,
                                                                                                            Double minUpperBound,
                                                                                                            ArrayList<Long> maxAttr,
                                                                                                            Double maxLowerBound,
                                                                                                            ArrayList<Long> avgAttr,
                                                                                                            Double avgLowerBound,
                                                                                                            Double avgUpperBound,
                                                                                                            ArrayList<Long> sumAttr
    ){
        int cId; //Counter for numbering the regions
        Map<Integer, Region> regionList = new HashMap<Integer, Region>();
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
                Region newRegion = new Region(cId);
                newRegion.addArea(arr_index, minAttr.get(arr_index), maxAttr.get(arr_index), avgAttr.get(arr_index), sumAttr.get(arr_index), r);
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
                Region tr = new Region(-1);
                removedLow.add(lowArea);
                labels[lowArea] = -1;
                tr.addArea(lowArea, minAttr.get(lowArea), maxAttr.get(lowArea), avgAttr.get(lowArea), sumAttr.get(lowArea), r);
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
                                tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), sumAttr.get(i), r);
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
                                tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), sumAttr.get(i), r);
                                removedLow.add(i);
                                labels[i] = -1;
                                updated = true;
                                break;
                            }
                        }
                    } else {
                        feasible = true;
                        regionChange = true;
                        if(debug){
                            System.out.println("Region Change");}
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
        //May be the condition for the while loop can be only for low or high? The order for low and high may also matter.
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
                Region tr = new Region(-1);
                removedHigh.add(HighArea);
                labels[HighArea] = -1;
                tr.addArea(HighArea, minAttr.get(HighArea), maxAttr.get(HighArea), avgAttr.get(HighArea), sumAttr.get(HighArea), r);
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
                                tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), sumAttr.get(i), r);
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
                                tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), sumAttr.get(i), r);
                                removedLow.add(i);
                                labels[i] = -1;
                                updated = true;
                                break;
                            }
                        }
                    } else {
                        feasible = true;
                        regionChange = true;
                        if(debug){
                            System.out.println("Region Change");}
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
                    regionList.get(labels[neighborArea]).addArea(avgArea, minAttr.get(avgArea), maxAttr.get(avgArea), avgAttr.get(avgArea), sumAttr.get(avgArea), r);
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
                        regionList.get(labels[neighborArea]).addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), sumAttr.get(lowarea), r);
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
                    regionList.get(lowestRegion).addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), sumAttr.get(lowarea), r);
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
                        regionList.get(labels[neighborArea]).addArea(higharea, minAttr.get(higharea), maxAttr.get(higharea), avgAttr.get(higharea), sumAttr.get(higharea), r);
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
                    regionList.get(highestRegion).addArea(higharea, minAttr.get(higharea), maxAttr.get(higharea), avgAttr.get(higharea), sumAttr.get(higharea), r);
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
                List<Region> tmpRegionList = new ArrayList<Region>();
                Region tryR = new Region(-1);
                Double lowestAcceptLow = Double.POSITIVE_INFINITY;
                //System.out.println(r.getNeighbors(lowarea));
                List<Integer> neighborList = new ArrayList<>(r.getNeighbors(lowarea));
                //Collections.shuffle(neighborList);
                for (Integer neighborArea : neighborList) {
                    //System.out.print(labels[neighborArea] + " ");
                    if (labels[neighborArea] > 0) {
                        Region tmpR = regionList.get(labels[neighborArea]);
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
                    tryR.addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), sumAttr.get(lowarea), r);
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
                    Region expandR = new Region(-1);

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
                    tryR = tryR.mergeWith(expandR, minAttr, maxAttr, avgAttr, sumAttr, r);
                    if (avgAttr.get(lowarea) >= tryR.getAcceptLow()) {
                        merged = true;
                        //System.out.println("Merged!");
                        tryR.addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), sumAttr.get(lowarea), r);
                        labels[lowarea] = expandR.getId();
                        labels = tryR.updateId(expandR.getId(), labels);
                        interUnassignedLow.remove();
                        for (Region tr : tmpRegionList) {

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
            List<Region> tmpRegionList = new ArrayList<Region>();
            Region tryR = new Region(-1);
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
                tryR.addArea(highArea, minAttr.get(highArea), maxAttr.get(highArea), avgAttr.get(highArea), sumAttr.get(highArea), r);
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
                Region expandR = new Region(-1);
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
                tryR = tryR.mergeWith(expandR, minAttr, maxAttr, avgAttr, sumAttr, r);
                if (avgAttr.get(highArea) <= tryR.getAcceptHigh()) {
                    tryR.addArea(highArea, minAttr.get(highArea), maxAttr.get(highArea), avgAttr.get(highArea), sumAttr.get(highArea), r);
                    labels[highArea] = expandR.getId();
                    labels = tryR.updateId(expandR.getId(), labels);
                    iterUnassignedHigh.remove();
                    for (Region tr : tmpRegionList) {

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
                    regionList.get(labels[neighborArea]).addArea(avgArea, minAttr.get(avgArea), maxAttr.get(avgArea), avgAttr.get(avgArea), sumAttr.get(avgArea), r);
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
            for (Map.Entry<Integer, Region> e : regionList.entrySet()) {

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
        for (Map.Entry<Integer, Region> regionEntry : regionList.entrySet()) {
            Region region = regionEntry.getValue();
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
                Region notMinRegion = regionList.get(notMinId);
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
                Region notMaxRegion = regionList.get(notMaxId);
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
                List<Integer> regionNeighbor = new ArrayList<>(neighborSet);

                //System.out.println(regionNeighbor.size());

                if(randFlag[0] >= 1){
                    Collections.shuffle(regionNeighbor);
                }

                for(Integer neighbor:regionNeighbor){
                    if(notMax.contains(neighbor)){
                        minMaxMerged = true;
                        Region mergedRegion = regionList.get(notMinRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, sumAttr, r);
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
                List<Integer> regionNeighbor = new ArrayList<>(neighborSet);

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
                        Region mergedRegion = regionList.get(notMinRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, sumAttr, r);
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
                List<Integer> regionNeighbor = new ArrayList<>(neighborSet);
                if(randFlag[0] >= 1){
                    Collections.shuffle(regionNeighbor);
                }
                for(Integer neighbor:regionNeighbor){
                    if(!notMax.contains(neighbor)){
                        minMaxMerged = true;
                        Region mergedRegion = regionList.get(notMaxRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, sumAttr, r);
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
                Region notMinRegion = regionList.get(notMinId);
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
                Region notMaxRegion = regionList.get(notMaxId);
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
        Quartet<int[], Map<Integer, Region>, List<Integer>, List<Integer>> avgInitialization = new Quartet<>(labels, regionList, unassignedLow, unassignedHigh);
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
            List<Map.Entry<Integer, Region>> tmpList2 = new ArrayList<Map.Entry<Integer, Region>>(regionList.entrySet());
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


    public static RegionCollection construction_phase_breakdown(ArrayList<Integer> idList,
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

                                                                ArrayList<Long> sumAttr,
                                                                Double sumLowerBound,
                                                                Double sumUpperBound,

                                                                Double countLowerBound,
                                                                Double countUpperBound) {
        int maxIt = 100;


        Region.setRange(minLowerBound, minUpperBound, maxLowerBound, maxUpperBound, avgLowerBound, avgUpperBound, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);

        int max_p = 0;

        RegionCollection bestCollection = null;
        for (int it = 0; it < maxIt  ; it++) {
            //System.out.println("i: " + i + " max_p: " + max_p);
            double minStart = System.currentTimeMillis()/ 1000.0;
            ArrayList<Integer> areas = new ArrayList<Integer>();
            areas.addAll(idList);
            int min_unAssigned = maxAttr.size();
            int[] labels = new int[disAttr.size()];//The size of the attribute sets should be the same.
            if (minLowerBound != -Double.POSITIVE_INFINITY || maxUpperBound != Double.POSITIVE_INFINITY || sumUpperBound != Double.POSITIVE_INFINITY) {
                Iterator<Integer> idIterator = areas.iterator();
                while (idIterator.hasNext()) {
                    Integer id = idIterator.next();
                    if (minAttr.get(id) < minLowerBound || maxAttr.get(id) > maxUpperBound || sumAttr.get(id) > sumUpperBound) {
                        idIterator.remove();
                        labels[id] = -2;
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
            double minEnd = System.currentTimeMillis()/ 1000.0;
            minTime = minTime +(minEnd-minStart);
            int cId;
            //List<Integer> enclave = new ArrayList<Integer>();
            //Map<Integer, List<Integer>> regionList = new HashMap<Integer, List<Integer>>();
            //Map<Integer, Integer> regionSpatialAttr = new HashMap<Integer, Integer>();
            Map<Integer, Region> regionList = new HashMap<Integer, Region>();
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
                    Region newRegion = new Region(cId);
                    newRegion.addArea(arr_index, minAttr.get(arr_index), maxAttr.get(arr_index), avgAttr.get(arr_index), sumAttr.get(arr_index), r);
                    regionList.put(cId, newRegion);
                    labels[arr_index] = cId;
                }
            }

            /*for(Map.Entry<Integer, RegionNew> mapEntry: regionList.entrySet()){
                System.out.println(mapEntry.getKey() + " " + mapEntry.getValue().getId());
            }*/
            if(debug){
                System.out.println("Step 1:");
                System.out.println("UnassignedLow: " + unassignedLow.size());
                System.out.println("UnassignedHigh: " + unassignedHigh.size());
                System.out.println("Num of regions: " + regionList.size());
            }

                /*for(Map.Entry<Integer, Region> e: regionList.entrySet()){
                    System.out.println(e.getKey() + " " + e.getValue().getId() + " " + e.getValue().getRegionNeighborSet(labels));
                }*/
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
                    Region tr = new Region(-1);
                    removedLow.add(lowArea);
                    labels[lowArea] = -1;
                    tr.addArea(lowArea, minAttr.get(lowArea), maxAttr.get(lowArea), avgAttr.get(lowArea), sumAttr.get(lowArea), r);
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
                                    tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), sumAttr.get(i), r);
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
                                    tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), sumAttr.get(i), r);
                                    removedLow.add(i);
                                    labels[i] = -1;
                                    updated = true;
                                    break;
                                }
                            }
                        } else {
                            feasible = true;
                            regionChange = true;
                            if(debug){
                                System.out.println("Region Change");}
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
            //May be the condition for the while loop can be only for low or high? The order for low and high may also matter.
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
                    Region tr = new Region(-1);
                    removedHigh.add(HighArea);
                    labels[HighArea] = -1;
                    tr.addArea(HighArea, minAttr.get(HighArea), maxAttr.get(HighArea), avgAttr.get(HighArea), sumAttr.get(HighArea), r);
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
                                    tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), sumAttr.get(i), r);
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
                                    tr.addArea(i, minAttr.get(i), maxAttr.get(i), avgAttr.get(i), sumAttr.get(i), r);
                                    removedLow.add(i);
                                    labels[i] = -1;
                                    updated = true;
                                    break;
                                }
                            }
                        } else {
                            feasible = true;
                            regionChange = true;
                            if(debug){
                                System.out.println("Region Change");}
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
            }
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
                        regionList.get(labels[neighborArea]).addArea(avgArea, minAttr.get(avgArea), maxAttr.get(avgArea), avgAttr.get(avgArea), sumAttr.get(avgArea), r);
                        labels[avgArea] = regionList.get(labels[neighborArea]).getId();
                        iteratorAvg.remove();
                        break;
                    }
                }
            }
            Iterator<Integer> iteratorLow = unassignedLow.iterator();
            while (iteratorLow.hasNext()) {
                Integer lowarea = iteratorLow.next();
                //Set<Integer> neighborRegionSet = new HashSet<Integer>();
                List<Integer> neighborList = new ArrayList<>(r.getNeighbors(lowarea));
                if(randFlag[0] <= 1){
                    if(randFlag[0] == 1){
                        Collections.shuffle(neighborList);
                    }

                    for (Integer neighborArea : neighborList) {
                        //Set.add(labels[neighborArea]);
                        if (labels[neighborArea] > 0 && regionList.get(labels[neighborArea]).getAcceptLow() <= avgAttr.get(lowarea)) {
                            if (debug) {
                                System.out.println("Add low area " + lowarea + " to region " + labels[neighborArea]);
                            }
                            regionList.get(labels[neighborArea]).addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), sumAttr.get(lowarea), r);
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
                        regionList.get(lowestRegion).addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), sumAttr.get(lowarea), r);
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
                            regionList.get(labels[neighborArea]).addArea(higharea, minAttr.get(higharea), maxAttr.get(higharea), avgAttr.get(higharea), sumAttr.get(higharea), r);
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
                        regionList.get(highestRegion).addArea(higharea, minAttr.get(higharea), maxAttr.get(higharea), avgAttr.get(higharea), sumAttr.get(higharea), r);
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
                    List<Region> tmpRegionList = new ArrayList<Region>();
                    Region tryR = new Region(-1);
                    Double lowestAcceptLow = Double.POSITIVE_INFINITY;
                    //System.out.println(r.getNeighbors(lowarea));
                    List<Integer> neighborList = new ArrayList<>(r.getNeighbors(lowarea));
                    //Collections.shuffle(neighborList);
                    for (Integer neighborArea : neighborList) {
                        //System.out.print(labels[neighborArea] + " ");
                        if (labels[neighborArea] > 0) {
                            Region tmpR = regionList.get(labels[neighborArea]);
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
                        tryR.addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), sumAttr.get(lowarea), r);
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
                        Region expandR = new Region(-1);

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
                        tryR = tryR.mergeWith(expandR, minAttr, maxAttr, avgAttr, sumAttr, r);
                        if (avgAttr.get(lowarea) >= tryR.getAcceptLow()) {
                            merged = true;
                            //System.out.println("Merged!");
                            tryR.addArea(lowarea, minAttr.get(lowarea), maxAttr.get(lowarea), avgAttr.get(lowarea), sumAttr.get(lowarea), r);
                            labels[lowarea] = expandR.getId();
                            labels = tryR.updateId(expandR.getId(), labels);
                            interUnassignedLow.remove();
                            for (Region tr : tmpRegionList) {

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
                List<Region> tmpRegionList = new ArrayList<Region>();
                Region tryR = new Region(-1);
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
                    tryR.addArea(highArea, minAttr.get(highArea), maxAttr.get(highArea), avgAttr.get(highArea), sumAttr.get(highArea), r);
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
                    Region expandR = new Region(-1);
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
                    tryR = tryR.mergeWith(expandR, minAttr, maxAttr, avgAttr, sumAttr, r);
                    if (avgAttr.get(highArea) <= tryR.getAcceptHigh()) {
                        tryR.addArea(highArea, minAttr.get(highArea), maxAttr.get(highArea), avgAttr.get(highArea), sumAttr.get(highArea), r);
                        labels[highArea] = expandR.getId();
                        labels = tryR.updateId(expandR.getId(), labels);
                        iterUnassignedHigh.remove();
                        for (Region tr : tmpRegionList) {

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
                        regionList.get(labels[neighborArea]).addArea(avgArea, minAttr.get(avgArea), maxAttr.get(avgArea), avgAttr.get(avgArea), sumAttr.get(avgArea), r);
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
                for (Map.Entry<Integer, Region> e : regionList.entrySet()) {

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
            for (Map.Entry<Integer, Region> regionEntry : regionList.entrySet()) {
                Region region = regionEntry.getValue();
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
                    Region notMinRegion = regionList.get(notMinId);
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
                    Region notMaxRegion = regionList.get(notMaxId);
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
                    List<Integer> regionNeighbor = new ArrayList<>(neighborSet);

                    //System.out.println(regionNeighbor.size());

                    if(randFlag[0] >= 1){
                        Collections.shuffle(regionNeighbor);
                    }

                    for(Integer neighbor:regionNeighbor){
                        if(notMax.contains(neighbor)){
                            minMaxMerged = true;
                            Region mergedRegion = regionList.get(notMinRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, sumAttr, r);
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
                    List<Integer> regionNeighbor = new ArrayList<>(neighborSet);

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
                            Region mergedRegion = regionList.get(notMinRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, sumAttr, r);
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
                    List<Integer> regionNeighbor = new ArrayList<>(neighborSet);
                    if(randFlag[0] >= 1){
                        Collections.shuffle(regionNeighbor);
                    }
                    for(Integer neighbor:regionNeighbor){
                        if(!notMax.contains(neighbor)){
                            minMaxMerged = true;
                            Region mergedRegion = regionList.get(notMaxRegion).mergeWith(regionList.get(neighbor), minAttr, maxAttr, avgAttr, sumAttr, r);
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
                    Region notMinRegion = regionList.get(notMinId);
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
                    Region notMaxRegion = regionList.get(notMaxId);
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
            double avgEnd = System.currentTimeMillis()/ 1000.0;
            avgTime += (avgEnd - minEnd);
            //Start sum and count
            if(debug){
                System.out.println("-------------");
                System.out.println("P after AVG: " +regionList.size());
                System.out.println("-------------");
            }
            boolean updated = true;
            while (updated) {
                //checkLabels(labels, regionList);
                updated = false;
                List<Map.Entry<Integer, Region>> tmpList2 = new ArrayList<Map.Entry<Integer, Region>>(regionList.entrySet());
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
            double sumEnd = System.currentTimeMillis()/ 1000.0;
            sumTime += (sumEnd - avgEnd);


            /*for(Map.Entry<Integer, RegionNew> entry: regionList.entrySet()){
                System.out.println( entry.getValue().getSum() + " " + entry.getValue().getCount());
            }*/

            int unAssignedCount = 0;
            for(int i = 0; i < labels.length; i++){
                if (labels[i] <= 0)
                    unAssignedCount ++;
            }
            //System.out.println("Distance for this regionList" + calculateWithinRegionDistance(regionList, distanceMatrix));
            if(regionList.size() > max_p || (regionList.size() == max_p && unAssignedCount < min_unAssigned)){
                max_p = regionList.size();
                min_unAssigned = unAssignedCount;
                bestCollection = new RegionCollection(regionList.size(), labels, regionList);
            }

        }

        if(debug){
            Map<Integer, Region> rcn = bestCollection.getRegionMap();
            for(Region rn: rcn.values()){
                if(!rn.satisfiable()){
                    System.out.println("Region " + rn.getId() + " not satisfiable!");
                    System.exit(125);
                }
            }
        }

        return bestCollection;
    }
    public static RegionCollection construction_phase_breakdown_minmaxNoRepeat(ArrayList<Integer> idList,
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

                                                                               ArrayList<Long> sumAttr,
                                                                               Double sumLowerBound,
                                                                               Double sumUpperBound,

                                                                               Double countLowerBound,
                                                                               Double countUpperBound) {
        int maxIt = 100;


        Region.setRange(minLowerBound, minUpperBound, maxLowerBound, maxUpperBound, avgLowerBound, avgUpperBound, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);

        int max_p = 0;

        RegionCollection bestCollection = null;
        double minStart = System.currentTimeMillis() / 1000.0;

        ArrayList<Integer> areas = new ArrayList<Integer>();
        areas.addAll(idList);
        int min_unAssigned = maxAttr.size();
        //First step: Filtering and seeding for MIN, MAX.
        Pair<int[], ArrayList<Integer>> minmaxResult = filtering_and_seeding(areas, minAttr, minLowerBound, minUpperBound, maxAttr, maxLowerBound, maxUpperBound, sumAttr, sumUpperBound);
        int[] minmaxlabels = minmaxResult.getValue0();
        ArrayList<Integer> minmaxseedAreas = minmaxResult.getValue1();

        double minEnd = System.currentTimeMillis() / 1000.0;
        minTime = minTime + (minEnd - minStart);

        for (int it = 0; it < maxIt; it++) {

            int[] labels = minmaxlabels.clone();
            ArrayList<Integer> seedAreas = new ArrayList<>(minmaxseedAreas);

            //Second step: AVG and MIN MAX revisit
            Quartet<int[], Map<Integer, Region>, List<Integer>, List<Integer>> avgInitialization = region_initialization(labels, seedAreas, r, minAttr, minUpperBound, maxAttr, maxLowerBound, avgAttr, avgLowerBound, avgUpperBound, sumAttr);

            labels = avgInitialization.getValue0();
            Map<Integer, Region> regionList = avgInitialization.getValue1();

            double avgEnd = System.currentTimeMillis() / 1000.0;
            avgTime += (avgEnd - minEnd);
            //Start sum and count
            if (check_p_afterAVG) {
                System.out.println("-------------");
                System.out.println("P after AVG: " + regionList.size());
                System.out.println("-------------");
            }
            Pair<int[], Map<Integer, Region>> result = sumcount_construction(labels, regionList, r, minAttr, maxAttr, avgAttr, sumAttr, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound);
            labels = result.getValue0();
            regionList = result.getValue1();
            double sumEnd = System.currentTimeMillis() / 1000.0;
            sumTime += (sumEnd - avgEnd);


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
                bestCollection = new RegionCollection(regionList.size(), labels, regionList);
            }

            if (debug) {
                Map<Integer, Region> rcn = bestCollection.getRegionMap();
                for (Region rn : rcn.values()) {
                    if (!rn.satisfiable()) {
                        System.out.println("Region " + rn.getId() + " not satisfiable!");
                        System.exit(125);
                    }
                }
            }


        }
        return bestCollection;
    }


    public static void checkLabels(int[] labels, Map<Integer, Region> regionList){
        boolean consistent = true;
        for(int i = 0; i < labels.length; i++){
            if (labels[i] > 0){
                if(!regionList.get(labels[i]).getAreaList().contains(i)){
                    System.out.println("Area " + i + " not in region " + labels[i]);
                    consistent = false;
                }
            }
        }
        for(Map.Entry<Integer, Region> mapEntry: regionList.entrySet()){
            Region region = mapEntry.getValue();
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
    public static void  set_input_construct(String fileName,
                                            String minAttrName,
                                            Double minAttrLow,
                                            Double minAttrHigh,
                                            String maxAttrName,
                                            Double maxAttrLow,
                                            Double maxAttrHigh,
                                            String avgAttrName,
                                            Double avgAttrLow,
                                            Double avgAttrHigh,
                                            String sumAttrName,
                                            Double sumAttrLow,
                                            Double sumAttrHigh,
                                            Double countLow,
                                            Double countHigh,
                                            String distAttrName

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
                sumAttr.add(Long.parseLong(feature.getAttribute(sumAttrName).toString()));
                distAttr.add(Long.parseLong(feature.getAttribute(distAttrName).toString()));
                fList.add(feature);
                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < sumMin){
                    sumMin = Long.parseLong(feature.getAttribute(sumAttrName).toString());
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
        if(minAttrMin > minAttrHigh){
            System.out.println("There is no area satisfying the MIN <=. The program will terminate immediately.");
            System.exit(1);
        }else if(minAttrMax < minAttrLow){
            System.out.println("There is no area satisfying the MIN >=. The program will terminate immediately.");
            System.exit(1);
        }

        //System.out.println(minX + " " + minY + ", " + maxX + " " + maxY);
        double rookstartTime = System.currentTimeMillis()/ 1000.0;
        //System.out.println("Time for reading the file: " + (rookstartTime - startTime));
        SpatialGrid sg = new SpatialGrid(minX, minY, maxX, maxY);
        //sg.createIndex(45, fList);
        //sg.calculateContiguity(fList);
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
        for(int i = 0; i < numOfIts; i++){
            double constructionStart = System.currentTimeMillis() / 1000.0;
            RegionCollection rc = construction_phase_breakdown(idList, distAttr, sg,
                    minAttr,
                    minAttrLow,
                    minAttrHigh,

                    maxAttr,
                    maxAttrLow,
                    maxAttrHigh,

                    avgAttr,
                    avgAttrLow,
                    avgAttrHigh,

                    sumAttr,
                    sumAttrLow,
                    sumAttrHigh,
                    countLow, countHigh);
            double constructionEnd = System.currentTimeMillis() / 1000.0;
            double constructionDuration = constructionEnd - constructionStart;
            //System.out.println("Time for construction phase:\n" + (constructionTime - rookendTime));
            System.out.println("Construction time: " + constructionDuration);
            int max_p = rc.getMax_p();
            //System.out.println("MaxP: " + max_p);
            //Map<Integer, Integer> regionSpatialAttr = rc.getRegionSpatialAttr();
            int[] labels = rc.getLabels();
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
            //System.out.println("minTime: " + minTime);
            //System.out.println("avgTime: " + avgTime);
            //System.out.println("sumTime: " + sumTime);

            System.out.println("Iteration: " + i);
            System.out.println("p: "+ max_p);
            System.out.println("Construction time: " + constructionDuration);
            //System.out.println("Tabu search time: " + heuristicDuration);
            //System.out.println("Heterogeneity score before Tabu: "  + totalWDS);
            //System.out.println("Heterogeneity score after Tabu: " + tr.WDS);
            System.out.println("Number of unassigned areas: " + unassignedCount + "\n");

            //csvWriter.write(i + ", " + max_p + ", " + constructionDuration + ", " + heuristicDuration + ", " + (constructionDuration+heuristicDuration) + ", " + totalWDS + ", " + tr.WDS + ", " + WDSDifference + "," + unassignedCount +"," + minTime + "," + avgTime + "," + sumTime + "\n");
            csvWriter.write(i + ", " + max_p + ", " + constructionDuration + ", " +  "," + unassignedCount +"," + minTime + "," + avgTime + "," + sumTime + "\n");
            csvWriter.flush();
            minTime = 0;
            avgTime = 0;
            sumTime = 0;

        }
        csvWriter.close();
        //System.out.println("End of setipnput");

    }
    public static RegionCollection  set_shapefile_input(String fileName,
                                                        String minAttrName,
                                                        Double minAttrLow,
                                                        Double minAttrHigh,
                                                        String maxAttrName,
                                                        Double maxAttrLow,
                                                        Double maxAttrHigh,
                                                        String avgAttrName,
                                                        Double avgAttrLow,
                                                        Double avgAttrHigh,
                                                        String sumAttrName,
                                                        Double sumAttrLow,
                                                        Double sumAttrHigh,
                                                        Double countLow,
                                                        Double countHigh,
                                                        String distAttrName

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
                sumAttr.add(Long.parseLong(feature.getAttribute(sumAttrName).toString()));
                distAttr.add(Long.parseLong(feature.getAttribute(distAttrName).toString()));
                fList.add(feature);
                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < sumMin){
                    sumMin = Long.parseLong(feature.getAttribute(sumAttrName).toString());
                }
                /*if (Long.parseLong(feature.getAttribute(avgAttrName).toString()) < 0){
                    System.out.println("AVG attribute contains negative value(s)");
                    return;
                }
                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < 0){
                    System.out.println("SUM attribute contains negative value(s)");
                    return;
                }*/
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
        if(minAttrMin > minAttrHigh){
            System.out.println("There is no area satisfying the MIN <=. The program will terminate immediately.");
            System.exit(1);
        }else if(minAttrMax < minAttrLow){
            System.out.println("There is no area satisfying the MIN >=. The program will terminate immediately.");
            System.exit(1);
        }

        //System.out.println(minX + " " + minY + ", " + maxX + " " + maxY);
        double rookstartTime = System.currentTimeMillis()/ 1000.0;
        //System.out.println("Time for reading the file: " + (rookstartTime - startTime));
        SpatialGrid sg = new SpatialGrid(minX, minY, maxX, maxY);
        //sg.createIndex(45, fList);
        //sg.calculateContiguity(fList);
        HashMap<Integer, Set<Integer>> neighborMap = calculateNeighbors(geometryList);
        sg.setNeighbors(neighborMap);
        double rookendTime = System.currentTimeMillis()/ 1000.0;
        System.out.println("Rook time: " + (rookendTime - rookstartTime));

        double dataLoadTime = System.currentTimeMillis()/ 1000.0;
        System.out.println("Input size: " + distAttr.size());
        long [][] distanceMatrix = EMPTabu.pdist(distAttr);
        Date t = new Date();


        double constructionStart = System.currentTimeMillis() / 1000.0;
        RegionCollection rc = construction_phase_breakdown(idList, distAttr, sg,
                minAttr,
                minAttrLow,
                minAttrHigh,

                maxAttr,
                maxAttrLow,
                maxAttrHigh,

                avgAttr,
                avgAttrLow,
                avgAttrHigh,

                sumAttr,
                sumAttrLow,
                sumAttrHigh,
                countLow, countHigh);
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

        long totalWDS = EMPTabu.calculateWithinRegionDistance(rc.getRegionMap(), distanceMatrix);
        //System.out.println("totalWithinRegionDistance before tabu: \n" + totalWDS);
        int tabuLength = 10;
        int max_no_move = distAttr.size();
        //checkLabels(rc.getLabels(), rc.getRegionList());

        //System.out.println("Start tabu");

        TabuReturn tr = EMPTabu.performTabu(rc.getLabels(), rc.getRegionMap(), sg, EMPTabu.pdist((distAttr)), tabuLength, max_no_move, minAttr, maxAttr, sumAttr, avgAttr);
        int[] labels = tr.labels;
        //System.out.println(labels.length);
        long WDSDifference = totalWDS - tr.WDS;
        //int[] labels = SimulatedAnnealing.performSimulatedAnnealing(rc.getLabels(), rc.getRegionList(), sg, pdist((distAttr)), minAttr, maxAttr, sumAttr, avgAttr);
        double endTime = System.currentTimeMillis()/ 1000.0;
        //System.out.println("MaxP: " + max_p);
        double heuristicDuration = endTime - constructionEnd;
        //System.out.println("Time for tabu(s): \n" + (endTime - constructionTime));
        // System.out.println("total time: \n" +(endTime - startTime));
        int unassignedCount = 0;
        for( int j = 0; j < labels.length; j++){
            if(labels[j] < 1){
                unassignedCount++;
            }
        }
        //System.out.println("minTime: " + minTime);
        //System.out.println("avgTime: " + avgTime);
        //System.out.println("sumTime: " + sumTime);

        RegionCollection finalRC = new RegionCollection();
        finalRC.setMax_p(rc.getMax_p());
        finalRC.setLabels(tr.labels);
        finalRC.setUnassignedCount(unassignedCount);
        return finalRC;

    }
    public static void  set_input(String fileName,
                                  String minAttrName,
                                  Double minAttrLow,
                                  Double minAttrHigh,
                                  String maxAttrName,
                                  Double maxAttrLow,
                                  Double maxAttrHigh,
                                  String avgAttrName,
                                  Double avgAttrLow,
                                  Double avgAttrHigh,
                                  String sumAttrName,
                                  Double sumAttrLow,
                                  Double sumAttrHigh,
                                  Double countLow,
                                  Double countHigh,
                                  String distAttrName

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
        for(int i = 0; i < numOfIts; i++){
            double constructionStart = System.currentTimeMillis() / 1000.0;
            RegionCollection rc = construction_phase_breakdown(idList, distAttr, sg,
                    minAttr,
                    minAttrLow,
                    minAttrHigh,

                    maxAttr,
                    maxAttrLow,
                    maxAttrHigh,

                    avgAttr,
                    avgAttrLow,
                    avgAttrHigh,

                    sumAttr,
                    sumAttrLow,
                    sumAttrHigh,
                    countLow, countHigh);
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

            long totalWDS = EMPTabu.calculateWithinRegionDistance(rc.getRegionMap(), distanceMatrix);
            //System.out.println("totalWithinRegionDistance before tabu: \n" + totalWDS);
            int tabuLength = 10;
            int max_no_move = distAttr.size();
            //checkLabels(rc.getLabels(), rc.getRegionList());

            //System.out.println("Start tabu");

            TabuReturn tr = IteratedGreedy.performIteratedGreedy(rc.getLabels(), rc.getRegionMap(), sg, EMPTabu.pdist((distAttr)), max_no_move, minAttr, maxAttr, sumAttr, avgAttr);
            int[] labels = tr.labels;
            //System.out.println(labels.length);
            long WDSDifference = totalWDS - tr.WDS;
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
            System.out.println("Heterogeneity score after Tabu: " + tr.WDS);
            System.out.println("Number of unassigned areas: " + unassignedCount + "\n");

            csvWriter.write(i + ", " + max_p + ", " + constructionDuration + ", " + heuristicDuration + ", " + (constructionDuration+heuristicDuration) + ", " + totalWDS + ", " + tr.WDS + ", " + WDSDifference + "," + unassignedCount +"," + minTime + "," + avgTime + "," + sumTime + "\n");
            csvWriter.flush();
            minTime = 0;
            avgTime = 0;
            sumTime = 0;

        }
        csvWriter.close();

        if(debug)
            System.out.println("End of setipnput");

    }
    public static void  set_input_minmax_norepeat(String fileName,
                                                  String minAttrName,
                                                  Double minAttrLow,
                                                  Double minAttrHigh,
                                                  String maxAttrName,
                                                  Double maxAttrLow,
                                                  Double maxAttrHigh,
                                                  String avgAttrName,
                                                  Double avgAttrLow,
                                                  Double avgAttrHigh,
                                                  String sumAttrName,
                                                  Double sumAttrLow,
                                                  Double sumAttrHigh,
                                                  Double countLow,
                                                  Double countHigh,
                                                  String distAttrName

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
        for(int i = 0; i < numOfIts; i++){
            double constructionStart = System.currentTimeMillis() / 1000.0;
            RegionCollection rc = construction_phase_breakdown_minmaxNoRepeat(idList, distAttr, sg,
                    minAttr,
                    minAttrLow,
                    minAttrHigh,

                    maxAttr,
                    maxAttrLow,
                    maxAttrHigh,

                    avgAttr,
                    avgAttrLow,
                    avgAttrHigh,

                    sumAttr,
                    sumAttrLow,
                    sumAttrHigh,
                    countLow, countHigh);
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

            long totalWDS = EMPTabu.calculateWithinRegionDistance(rc.getRegionMap(), distanceMatrix);
            //System.out.println("totalWithinRegionDistance before tabu: \n" + totalWDS);
            int tabuLength = 10;
            int max_no_move = distAttr.size();
            //checkLabels(rc.getLabels(), rc.getRegionList());

            //System.out.println("Start tabu");

            TabuReturn tr = EMPTabu.performTabu(rc.getLabels(), rc.getRegionMap(), sg, EMPTabu.pdist((distAttr)), tabuLength, max_no_move, minAttr, maxAttr, sumAttr, avgAttr);
            int[] labels = tr.labels;
            //System.out.println(labels.length);
            long WDSDifference = totalWDS - tr.WDS;
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
            System.out.println("Heterogeneity score after Tabu: " + tr.WDS);
            System.out.println("Number of unassigned areas: " + unassignedCount + "\n");

            csvWriter.write(i + ", " + max_p + ", " + constructionDuration + ", " + heuristicDuration + ", " + (constructionDuration+heuristicDuration) + ", " + totalWDS + ", " + tr.WDS + ", " + WDSDifference + "," + unassignedCount +"," + minTime + "," + avgTime + "," + sumTime + "\n");
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


}
