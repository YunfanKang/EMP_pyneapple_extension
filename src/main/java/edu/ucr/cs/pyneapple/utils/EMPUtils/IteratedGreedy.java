package edu.ucr.cs.pyneapple.utils.EMPUtils;

import edu.ucr.cs.pyneapple.utils.SpatialGrid;

import java.util.*;

//import edu.ucr.cs.pineapple.regionalization.EMPUtils.GMaxPTabu;

public class IteratedGreedy {
    public static boolean debug = true;
    public static TabuReturn  performIteratedGreedy(int[] initLabels,
                                                    Map<Integer, Region> initRegionList,
                                                    SpatialGrid r,
                                                    long[][] distanceMatrix,
                                                    int max_no_move,
                                                    ArrayList<Long> minAttr,
                                                    ArrayList<Long> maxAttr,
                                                    ArrayList<Long> avgAttr,
                                                    ArrayList<Long> sumAttr){
        int[] labels = Arrays.copyOf(initLabels, initLabels.length);
        int[] bestLabels = Arrays.copyOf(initLabels, initLabels.length);
        Map<Integer, Region> regionList = initRegionList;
        //Map<Integer, Integer> regionSpatialAttrs = initRegionSpatialAttr;
        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            indexList.add(i);
        }
        double perturbStrength = 0.1;
        //System.out.println("Before deconstruction");
        //DeconstructionInerNode(labels, indexList, regionList, perturbStrength, r, distanceMatrix, minAttr, maxAttr, avgAttr, sumAttr);
        DeconstructionBoundaryNode(labels, indexList, regionList, perturbStrength, r, distanceMatrix, minAttr, maxAttr, avgAttr, sumAttr);

        long withinRegionDistance = EMPTabu.calculateWithinRegionDistance(initRegionList, distanceMatrix);
        long bestWDS = withinRegionDistance;



        TabuReturn tr = new TabuReturn();
        tr.labels = bestLabels;
        tr.WDS = bestWDS;
        return tr;
    }
    private static void DeconstructionInerNode(int[] initLabels,
                                       List<Integer> indexList,
                                       Map<Integer, Region> initRegionList,
                                       double perturbStrength,
                                       SpatialGrid r,
                                       long[][] distanceMatrix,
                                       ArrayList<Long> minAttr,
                                       ArrayList<Long> maxAttr,
                                       ArrayList<Long> avgAttr,
                                       ArrayList<Long> sumAttr){
        int noOfAreas = initLabels.length;

        int[] labels = Arrays.copyOf(initLabels, initLabels.length);
        Map<Integer, Region> regionList = initRegionList;
        int maxRegionId = Collections.max(regionList.keySet());
        int noOfPerturbAreas = (int) (perturbStrength * initLabels.length);
        //int[] array = new Random().ints(noOfPerturbAreas, 0, initLables.length).toArray()
        Collections.shuffle(indexList);
        List<Integer> randomIndices = indexList.subList(0, noOfPerturbAreas);
        //System.out.println(randomIndices);
        //List<Integer> potentialAreas = Tabu.pickMoveAreaNew(labels, regionList, r, distanceMatrix, minAttr, maxAttr, avgAttr, sumAttr);
        //System.out.println(potentialAreas.size());
        //System.out.println(randomIndices.size());
        List<Integer> deconstructedAreas = new ArrayList<Integer>();
        for (Integer index : randomIndices) {
            int regionId = labels[index];
            if (regionId <= 0)
                continue;

            Region sourceRegion = regionList.get(labels[index]);
            List<List<Integer>> ccs = sourceRegion.connectedComponentsAfterRemoval(index, r);
            if (ccs.size() == 1) {
                if (debug) {
                    System.out.println("Remove area " + index + " from region " + regionId);
                }
                sourceRegion.removeArea(index, minAttr, maxAttr, avgAttr, sumAttr, r);
                labels[index] = -7;
                deconstructedAreas.add(index);
            }else if(ccs.size() == 0){
                if(debug){
                    System.out.println("Region " + regionId + " contains only area  " + index + " " + regionList.get(regionId).areaList);
                }
                regionList.remove(regionId);
                labels[index] = -7;
                deconstructedAreas.add(index);
            }else{
                if(debug){
                    System.out.println("Removing area " + index + " from region " + regionId + " splits the region");
                }
                int noOfRegionsAfterSplit = ccs.size();
                regionList.remove(labels[index]);
                labels[index] = -7;
                deconstructedAreas.add(index);
                for(int i = 0; i < noOfRegionsAfterSplit; i++){
                    maxRegionId += 1;
                    Region tmpRegion = new Region(-1 * maxRegionId);
                    for(int j = 0; j < ccs.get(i).size();j++){
                        int area = ccs.get(i).get(j);
                        labels[area] = -1 * maxRegionId;
                        tmpRegion.addArea(area, minAttr.get(area), maxAttr.get(area), avgAttr.get(area), sumAttr.get(area), r);
                    }
                    regionList.put(-1 * maxRegionId, tmpRegion);
                    if(debug){
                        System.out.println("Add new region " + -1 * maxRegionId);
                    }
                }
            }
        }

        if(debug){
            List<Integer> infeasibleRegions = new ArrayList<>();
            System.out.println("No of regions: " + regionList.size());
            int infeasibleRegionCount = 0;
            for(Map.Entry<Integer, Region> entry: regionList.entrySet()){
                if(!entry.getValue().satisfiable()){
                    infeasibleRegionCount += 1;
                    infeasibleRegions.add(entry.getKey());
                }
            }
            System.out.println("Infeasible region count: " + infeasibleRegionCount + " " + infeasibleRegions);
            for(Integer region: infeasibleRegions){
                System.out.print(regionList.get(region).getSum() + " ");
            }
            System.out.println();
        }

        //Reconstruction
        for(int i = 0; i < deconstructedAreas.size(); i++){//Select a deconstructed area
            int area = deconstructedAreas.get(i);
            Set<Integer> neighborAreas = r.getNeighbors(area);
            Set<Integer> neighborRegions = new TreeSet<Integer>();
            boolean allTrue = true;
            for(Integer neighborArea: neighborAreas){
                neighborRegions.add(labels[neighborArea]);
                if(labels[neighborArea] > 0 || labels[neighborArea] < -7){
                    System.out.print(labels[neighborArea] + " " + regionList.get(labels[neighborArea]).satisfiable() + " ");
                    allTrue = allTrue & regionList.get(labels[neighborArea]).satisfiable();
                }

            }
            System.out.println(" " + allTrue);
            System.out.println(area + " " +neighborRegions);
        }
    }
    public static List<Integer> pickBoundaryArea(int[] initLabels, Map<Integer, Region> initRegionList, SpatialGrid r){
        List<Integer> boundaryAreas = new ArrayList<>();
        for(Map.Entry<Integer, Region> e: initRegionList.entrySet()){
            boundaryAreas.addAll(e.getValue().getBoundaryAreas(r, initLabels));
        }
        return boundaryAreas;
    }
    public static List<Integer> pickRemovableBoundaryArea(int[] initLabels, Map<Integer, Region> initRegionList, SpatialGrid r, ArrayList<Long> minAttr,
                                                          ArrayList<Long> maxAttr,
                                                          ArrayList<Long> avgAttr,
                                                          ArrayList<Long> sumAttr){
        List<Integer> boundaryAreas = new ArrayList<>();
        for(Map.Entry<Integer, Region> e: initRegionList.entrySet()){
            List<Integer> allBoundAreas = e.getValue().getBoundaryAreas(r, initLabels);
            for (Integer a:  allBoundAreas){
                if (e.getValue().removable(a, minAttr, maxAttr, avgAttr, sumAttr, r)){
                    boundaryAreas.add(a);
                }
                }
            }

        return boundaryAreas;
    }
    private static void DeconstructionBoundaryNode(int[] initLabels,
                                               List<Integer> indexList,
                                               Map<Integer, Region> initRegionList,
                                               double perturbStrength,
                                               SpatialGrid r,
                                               long[][] distanceMatrix,
                                               ArrayList<Long> minAttr,
                                               ArrayList<Long> maxAttr,
                                               ArrayList<Long> avgAttr,
                                               ArrayList<Long> sumAttr){
        int noOfAreas = initLabels.length;
        int[] labels = Arrays.copyOf(initLabels, initLabels.length);
        Map<Integer, Region> regionList = initRegionList;
        //int noOfPerturbAreas = (int) (perturbStrength * initLabels.length);
        //int[] array = new Random().ints(noOfPerturbAreas, 0, initLables.length).toArray()
        //Collections.shuffle(indexList);
        //List<Integer> randomIndices = indexList.subList(0, noOfPerturbAreas);
       // System.out.println(randomIndices);
        //List<Integer> potentialAreas = Tabu.pickMoveAreaNew(labels, regionList, r, distanceMatrix, minAttr, maxAttr, avgAttr, sumAttr);
        List<Integer> potentialAreas = pickBoundaryArea(labels, regionList, r);
        List<Integer> potentialRemoveAbleAreas = pickRemovableBoundaryArea(labels, regionList, r, minAttr, maxAttr, avgAttr, sumAttr);
        Collections.shuffle(potentialAreas);

        int noOfPerturbAreas = (int) (perturbStrength * potentialAreas.size());
        List<Integer> randomIndices = potentialAreas.subList(0, noOfPerturbAreas);

        System.out.println("All boundary " + potentialAreas.size());
        System.out.println("All removable boundary " + potentialRemoveAbleAreas.size());
        //System.out.println(randomIndices.size());
        int maxRegionId = Collections.max(regionList.keySet());

        List<Integer> deconstructedAreas = new ArrayList<Integer>();
        //for (Integer index : potentialAreas){
        for (Integer index : randomIndices){
            int regionId = labels[index];
            if (regionId <= 0)
                continue;

            Region sourceRegion = regionList.get(labels[index]);
            List<List<Integer>> ccs = sourceRegion.connectedComponentsAfterRemoval(index, r);
            if (ccs.size() == 1) {
                if (debug) {
                    System.out.println("Remove area " + index + " from region " + regionId);
                }
                sourceRegion.removeArea(index, minAttr, maxAttr, avgAttr, sumAttr, r);
                labels[index] = -7;
                deconstructedAreas.add(index);
            }else if(ccs.size() == 0){
                if(debug){
                    System.out.println("Region " + regionId + " contains only area  " + index + " " + regionList.get(regionId).areaList);
                }
                regionList.remove(regionId);
                labels[index] = -7;
                deconstructedAreas.add(index);
            }else{
                if(debug){
                    System.out.println("Removing area " + index + " from region " + regionId + " splits the region");
                }
                int noOfRegionsAfterSplit = ccs.size();
                regionList.remove(labels[index]);
                labels[index] = -7;
                deconstructedAreas.add(index);
                for(int i = 0; i < noOfRegionsAfterSplit; i++){
                    maxRegionId += 1;
                    Region tmpRegion = new Region(-1 * maxRegionId);
                    for(int j = 0; j < ccs.get(i).size();j++){
                        int area = ccs.get(i).get(j);
                        labels[area] = -1 * maxRegionId;
                        tmpRegion.addArea(area, minAttr.get(area), maxAttr.get(area), avgAttr.get(area), sumAttr.get(area), r);
                    }
                    regionList.put(-1 * maxRegionId, tmpRegion);
                    if(debug){
                        System.out.println("Add new region " + -1 * maxRegionId);
                    }
                }
            }
        }
        //Reconstruction
        List<Integer> infeasibleRegions = new ArrayList<>();
        //System.out.println("No of regions: " + regionList.size());
        int infeasibleRegionCount = 0;
        for(Map.Entry<Integer, Region> entry: regionList.entrySet()){
            if(!entry.getValue().satisfiable()){
                infeasibleRegionCount += 1;
                infeasibleRegions.add(entry.getKey());
            }
        }
        for(Integer infRegion: infeasibleRegions){

        }
        for(Integer dArea: deconstructedAreas){

        }
        if(debug){
            //List<Integer> infeasibleRegions = new ArrayList<>();
            System.out.println("No of regions: " + regionList.size());
            //int infeasibleRegionCount = 0;
            for(Map.Entry<Integer, Region> entry: regionList.entrySet()){
                if(!entry.getValue().satisfiable()){
                    infeasibleRegionCount += 1;
                    infeasibleRegions.add(entry.getKey());
                }
            }
            System.out.println("Infeasible region count: " + infeasibleRegionCount + " " + infeasibleRegions);
            for(Integer region: infeasibleRegions){
                System.out.print(regionList.get(region).getSum() + " ");
            }
            System.out.println();




        }

    }
}
