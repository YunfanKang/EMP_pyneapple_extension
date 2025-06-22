package edu.ucr.cs.pyneapple.regionalization;

import edu.ucr.cs.pyneapple.utils.EMPUtils.EMPTabu;
import edu.ucr.cs.pyneapple.utils.EMPUtils.RegionWithVariance;
import edu.ucr.cs.pyneapple.utils.SpatialGrid;
import org.geotools.data.*;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.locationtech.jts.geom.Geometry;
import org.javatuples.Pair;

import java.io.File;
import java.util.*;

public class RandomPartitionBaseline {
    public static void set_input_minmax_var_partition(String fileName,
                                                      String minAttrName, Double minAttrLow, Double minAttrHigh,
                                                      String maxAttrName, Double maxAttrLow, Double maxAttrHigh,
                                                      String avgAttrName, Double avgAttrLow, Double avgAttrHigh,
                                                      String varAttrName, Double varAttrLow, Double varAttrHigh,
                                                      String sumAttrName, Double sumAttrLow, Double sumAttrHigh,
                                                      Double countLow, Double countHigh,
                                                      String distAttrName, int targetRegionCount) throws Exception {

        System.out.println("Loading shapefile and reading features...");

        File file = new File(fileName);
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE;
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);

        ArrayList<Long> minAttr = new ArrayList<>();
        ArrayList<Long> maxAttr = new ArrayList<>();
        ArrayList<Long> avgAttr = new ArrayList<>();
        ArrayList<Long> varAttr = new ArrayList<>();
        ArrayList<Long> sumAttr = new ArrayList<>();
        ArrayList<Long> distAttr = new ArrayList<>();
        ArrayList<Integer> idList = new ArrayList<>();
        ArrayList<Geometry> geometryList = new ArrayList<>();

        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                minAttr.add(Long.parseLong(feature.getAttribute(minAttrName).toString()));
                maxAttr.add(Long.parseLong(feature.getAttribute(maxAttrName).toString()));
                avgAttr.add(Long.parseLong(feature.getAttribute(avgAttrName).toString()));
                varAttr.add(Long.parseLong(feature.getAttribute(varAttrName).toString()));
                sumAttr.add(Long.parseLong(feature.getAttribute(sumAttrName).toString()));
                distAttr.add(Long.parseLong(feature.getAttribute(distAttrName).toString()));
                idList.add(Integer.parseInt(feature.getID().split("\\.")[1]) - 1);

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                geometryList.add(geometry);

                double cminx = geometry.getEnvelope().getCoordinates()[0].getX();
                double cminy = geometry.getEnvelope().getCoordinates()[0].getY();
                double cmaxx = geometry.getEnvelope().getCoordinates()[2].getX();
                double cmaxy = geometry.getEnvelope().getCoordinates()[2].getY();
                minX = Math.min(minX, cminx);
                minY = Math.min(minY, cminy);
                maxX = Math.max(maxX, cmaxx);
                maxY = Math.max(maxY, cmaxy);
            }
        }
        dataStore.dispose();

        System.out.println("Finished reading " + idList.size() + " features.");

        SpatialGrid sg = new SpatialGrid(minX, minY, maxX, maxY);
        HashMap<Integer, Set<Integer>> neighborMap = calculateNeighbors(geometryList);
        sg.setNeighbors(neighborMap);
        System.out.println("Initialized spatial grid and neighborhood map.");

        List<Integer> allAreaIds = new ArrayList<>(idList);
        Map<Integer, Map<String, Double>> attributeTable = new HashMap<>();
        for (int i = 0; i < idList.size(); i++) {
            Map<String, Double> attr = new HashMap<>();
            attr.put("min", minAttr.get(i).doubleValue());
            attr.put("max", maxAttr.get(i).doubleValue());
            attr.put("avg", avgAttr.get(i).doubleValue());
            attr.put("var", varAttr.get(i).doubleValue());
            attr.put("sum", sumAttr.get(i).doubleValue());
            attr.put("dist", distAttr.get(i).doubleValue());
            attributeTable.put(idList.get(i), attr);
        }

        long startTime = System.currentTimeMillis();
        System.out.println("Running random partition baseline with target region count = " + targetRegionCount + "...");
        List<RegionWithVariance> regions = runRandomPartition(
                targetRegionCount, attributeTable, allAreaIds, sg,
                minAttrLow, minAttrHigh,
                maxAttrLow, maxAttrHigh,
                avgAttrLow, avgAttrHigh,
                varAttrLow, varAttrHigh,
                sumAttrLow, sumAttrHigh,
                countLow, countHigh,
                distAttr
        );

        long[][] distanceMatrix = edu.ucr.cs.pyneapple.utils.EMPUtils.EMPTabu.pdist(distAttr);
        System.out.println("Computing Heterogeneity Score (WDS)...");
        double hScore = computeHScore(regions, distanceMatrix);
        long endTime = System.currentTimeMillis();
        System.out.println("Partitioning took " + (endTime - startTime) + " ms.");
        System.out.println("Generated " + regions.size() + " regions using RandomPartitionBaseline.");
        System.out.println("Heterogeneity Score (WDS): " + hScore);
    }

    private static HashMap<Integer, Set<Integer>> calculateNeighbors(List<Geometry> geometryList) {
        HashMap<Integer, Set<Integer>> neighbors = new HashMap<>();
        for (int i = 0; i < geometryList.size(); i++) {
            for (int j = i + 1; j < geometryList.size(); j++) {
                if (geometryList.get(i).touches(geometryList.get(j))) {
                    neighbors.computeIfAbsent(i, k -> new HashSet<>()).add(j);
                    neighbors.computeIfAbsent(j, k -> new HashSet<>()).add(i);
                }
            }
        }
        return neighbors;
    }

    public static long computeHScore(List<RegionWithVariance> regions, long[][] distanceMatrix) {
        Map<Integer, RegionWithVariance> regionMap = new HashMap<>();
        for (RegionWithVariance region : regions) {
            regionMap.put(region.getId(), region);
        }
        return EMPTabu.calculateWithinRegionDistance_var(regionMap, distanceMatrix);
    }

    public static List<RegionWithVariance> runRandomPartition(
            int p,
            Map<Integer, Map<String, Double>> attributeTable,
            List<Integer> allAreaIds,
            SpatialGrid sg,
            double minLowerBound, double minUpperBound,
            double maxLowerBound, double maxUpperBound,
            double avgLowerBound, double avgUpperBound,
            double varianceLowerBound, double varianceUpperBound,
            double sumLowerBound, double sumUpperBound,
            double countLowerBound, double countUpperBound,
            ArrayList<Long> distList) {

        RegionWithVariance.setRange(minLowerBound, minUpperBound,
                maxLowerBound, maxUpperBound,
                avgLowerBound, avgUpperBound,
                varianceLowerBound, varianceUpperBound,
                sumLowerBound, sumUpperBound,
                countLowerBound, countUpperBound);

        ArrayList<Long> minList = extractLongList(attributeTable, "min");
        ArrayList<Long> maxList = extractLongList(attributeTable, "max");
        ArrayList<Long> avgList = extractLongList(attributeTable, "avg");
        ArrayList<Long> varList = extractLongList(attributeTable, "var");
        ArrayList<Long> sumList = extractLongList(attributeTable, "sum");

        Pair<int[], ArrayList<Integer>> result = filtering_and_seeding(
                new ArrayList<>(allAreaIds),
                minList, minLowerBound, minUpperBound,
                maxList, maxLowerBound, maxUpperBound,
                sumList, sumUpperBound
        );
        ArrayList<Integer> filteredAreas = result.getValue0() != null
                ? new ArrayList<>(allAreaIds) : new ArrayList<>();

        ArrayList<Integer> seedAreas = result.getValue1();


        while (true) {
            Random random = new Random();
            Collections.shuffle(seedAreas, random);
            List<Integer> seeds = seedAreas.subList(0, Math.min(p, seedAreas.size()));
            List<RegionWithVariance> regions = new ArrayList<>();
            Set<Integer> unassigned = new HashSet<>(filteredAreas);

            // 初始化 p 个种子区域
            for (int i = 0; i < seeds.size(); i++) {
                RegionWithVariance region = new RegionWithVariance(i + 1);
                Integer seed = seeds.get(i);
                if (region.addArea(seed, minList.get(seed), maxList.get(seed), avgList.get(seed), varList.get(seed), sumList.get(seed), sg)) {
                    regions.add(region);
                    unassigned.remove(seed);
                }
            }

            // 尝试扩展每个区域
            boolean anyAdded = true;
            while (!unassigned.isEmpty() && anyAdded) {
                anyAdded = false;
                for (RegionWithVariance region : regions) {
                    Set<Integer> neighbors = region.getAreaNeighborSet();
                    List<Integer> candidates = new ArrayList<>(neighbors);
                    Collections.shuffle(candidates, random);

                    for (Integer c : candidates) {
                        if (!unassigned.contains(c)) continue;
                        if (region.addArea(c, minList.get(c), maxList.get(c), avgList.get(c), varList.get(c), sumList.get(c), sg)) {
                            unassigned.remove(c);
                            anyAdded = true;
                            break;
                        }
                    }
                }
            }

            // ✅ 检查所有 region 是否满足条件
            boolean allSatisfied = true;
            for (RegionWithVariance region : regions) {
                if (!region.satisfiable()) {
                    allSatisfied = false;
                    break;
                }
            }

            if (allSatisfied && regions.size() == p) {
                System.out.println("[RandomPartitionBaseline] Success. Regions formed: " + regions.size());
                System.out.println("[RandomPartitionBaseline] Unassigned areas: " + unassigned.size());
                return regions;
            } else {
//                System.out.println("[RandomPartitionBaseline] Retry due to unsatisfiable region(s) or wrong region count.");
                // retry loop continues

            }
        }
    }


    private static ArrayList<Long> extractLongList(Map<Integer, Map<String, Double>> attrTable, String key) {
        int maxId = Collections.max(attrTable.keySet()) + 1;
        ArrayList<Long> list = new ArrayList<>(Collections.nCopies(maxId, 0L));
        for (Map.Entry<Integer, Map<String, Double>> entry : attrTable.entrySet()) {
            if (entry.getValue().containsKey(key)) {
                list.set(entry.getKey(), entry.getValue().get(key).longValue());
            }
        }
        return list;
    }

    public static Pair<int[], ArrayList<Integer>> filtering_and_seeding(ArrayList<Integer> areas,
                                                                        ArrayList<Long> minAttr,
                                                                        Double minLowerBound,
                                                                        Double minUpperBound,
                                                                        ArrayList<Long> maxAttr,
                                                                        Double maxLowerBound,
                                                                        Double maxUpperBound,
                                                                        ArrayList<Long> sumAttr,
                                                                        Double sumUpperBound) {
        int[] labels = new int[maxAttr.size()];
        Iterator<Integer> idIterator = areas.iterator();
        while (idIterator.hasNext()) {
            Integer id = idIterator.next();
            if (minAttr.get(id) < minLowerBound || maxAttr.get(id) > maxUpperBound || sumAttr.get(id) > sumUpperBound) {
                idIterator.remove();
                labels[id] = -2;
            }
        }

        ArrayList<Integer> seedAreas = new ArrayList<>();
        for (Integer id : areas) {
            if (minAttr.get(id) <= minUpperBound || maxAttr.get(id) >= maxLowerBound) {
                seedAreas.add(id);
            }
        }
        return new Pair<>(labels, seedAreas);
    }
}
