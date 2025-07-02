package edu.ucr.cs.pyneapple.regionalization;

import edu.ucr.cs.pyneapple.utils.EMPUtils.EMPTabu;
import edu.ucr.cs.pyneapple.utils.EMPUtils.RegionWithVariance;
import edu.ucr.cs.pyneapple.utils.SpatialGrid;
import org.javatuples.Pair;
import org.geotools.data.*;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.locationtech.jts.geom.Geometry;

import java.io.File;
import java.util.*;

public class GreedyBaseline {
    public static void set_input_minmax_var_baseline(String fileName,
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
                                                     double maxP) throws Exception {

        System.out.println("Loading shapefile and reading features...");

        File file = new File(fileName);
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source =
                dataStore.getFeatureSource(typeName);
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
        Double minAttrMin = Double.POSITIVE_INFINITY;
        Double minAttrMax = -Double.POSITIVE_INFINITY;
        Double maxAttrMax = -Double.POSITIVE_INFINITY;
        Double maxAttrMin = Double.POSITIVE_INFINITY;
        double sumMin = Double.POSITIVE_INFINITY;

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

                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < sumMin) {
                    sumMin = Long.parseLong(feature.getAttribute(sumAttrName).toString());
                }
                if (Long.parseLong(feature.getAttribute(avgAttrName).toString()) < 0) {
                    System.out.println("AVG attribute contains negative value(s)");
                    return;
                }
                if (Long.parseLong(feature.getAttribute(sumAttrName).toString()) < 0) {
                    System.out.println("SUM attribute contains negative value(s)");
                    return;
                }
                if (Long.parseLong(feature.getAttribute(minAttrName).toString()) < minAttrMin) {
                    minAttrMin = Double.parseDouble(feature.getAttribute(minAttrName).toString());
                }
                if (Long.parseLong(feature.getAttribute(minAttrName).toString()) > minAttrMax) {
                    minAttrMax = Double.parseDouble(feature.getAttribute(minAttrName).toString());
                }
                if (Long.parseLong(feature.getAttribute(maxAttrName).toString()) > maxAttrMax) {
                    maxAttrMax = Double.parseDouble(feature.getAttribute(maxAttrName).toString());
                }
                if (Long.parseLong(feature.getAttribute(maxAttrName).toString()) < maxAttrMin) {
                    maxAttrMin = Double.parseDouble(feature.getAttribute(maxAttrName).toString());
                }

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
        System.out.println("Finished reading " + idList.size() + " features.");
        dataStore.dispose();

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
            attributeTable.put(idList.get(i), attr);
        }
        long startTime = System.currentTimeMillis();
        System.out.println("Running region growing baseline with maxP = " + maxP + "...");
        List<RegionWithVariance> regions = run(maxP, attributeTable, allAreaIds, Double.POSITIVE_INFINITY, sg,
                minAttrLow, minAttrHigh,
                maxAttrLow, maxAttrHigh,
                avgAttrLow, avgAttrHigh,
                varAttrLow, varAttrHigh,
                sumAttrLow, sumAttrHigh,
                countLow, countHigh);

        long[][] distanceMatrix = EMPTabu.pdist(distAttr);
        System.out.println("Computing Heterogeneity Score (WDS)...");
        double hScore = computeHScore(regions, distanceMatrix);
        long endTime = System.currentTimeMillis();
        System.out.println("Region construction took " + (endTime - startTime) + " ms.");
<<<<<<< Updated upstream
        System.out.println("Generated " + regions.size() + " regions using RandomBaseline.");
=======
        System.out.println("Generated " + regions.size() + " regions using GreedyBaseline.");
>>>>>>> Stashed changes
        System.out.println("Heterogeneity Score (WDS): " + hScore);

        int maxRegionSize = regions.stream()
                .mapToInt(r -> r.getAreaList().size())
                .max()
                .orElse(0);

        System.out.println("Max area count in a single region: " + maxRegionSize);


    }

    public static List<RegionWithVariance> run(double maxRegions, Map<Integer, Map<String, Double>> attributeTable,
                                               List<Integer> allAreaIds,
                                               double maxStepsPerRegion,
                                               SpatialGrid sg,
                                               double minLowerBound, double minUpperBound,
                                               double maxLowerBound, double maxUpperBound,
                                               double avgLowerBound, double avgUpperBound,
                                               double varianceLowerBound, double varianceUpperBound,
                                               double sumLowerBound, double sumUpperBound,
                                               double countLowerBound, double countUpperBound) {

<<<<<<< Updated upstream
=======
        // 设置约束范围
>>>>>>> Stashed changes
        RegionWithVariance.setRange(
                minLowerBound, minUpperBound,
                maxLowerBound, maxUpperBound,
                avgLowerBound, avgUpperBound,
                varianceLowerBound, varianceUpperBound,
                sumLowerBound, sumUpperBound,
                countLowerBound, countUpperBound
        );

<<<<<<< Updated upstream

        ArrayList<Long> minList = extractLongList(attributeTable, "min");
        ArrayList<Long> maxList = extractLongList(attributeTable, "max");
        ArrayList<Long> sumList = extractLongList(attributeTable, "sum");

=======
        // 提取属性值
        ArrayList<Long> minList = extractLongList(attributeTable, "min");
        ArrayList<Long> maxList = extractLongList(attributeTable, "max");
        ArrayList<Long> sumList = extractLongList(attributeTable, "sum");
        ArrayList<Long> avgList = extractLongList(attributeTable, "avg");
        ArrayList<Long> varList = extractLongList(attributeTable, "var");

        // 获取所有区域并筛选可作为种子的
>>>>>>> Stashed changes
        ArrayList<Integer> areaIdList = new ArrayList<>(allAreaIds);
        Pair<int[], ArrayList<Integer>> result = filtering_and_seeding(areaIdList,
                minList, minLowerBound, minUpperBound,
                maxList, maxLowerBound, maxUpperBound,
                sumList, sumUpperBound);

<<<<<<< Updated upstream

        int[] labels = result.getValue0();
        ArrayList<Integer> seedAreas = result.getValue1();
        Set<Integer> unassigned = new HashSet<>(areaIdList);
        List<RegionWithVariance> regions = new ArrayList<>();
        Random random = new Random();

        ArrayList<Long> avgList = extractLongList(attributeTable, "avg");
        ArrayList<Long> varList = extractLongList(attributeTable, "var");


        while (!unassigned.isEmpty() && regions.size() < maxRegions) {
            for (Integer seed : new HashSet<>(seedAreas)) {
                if (!unassigned.contains(seed)) continue;
                RegionWithVariance region = new RegionWithVariance(regions.size() + 1);
                boolean success = region.addArea(
                        seed,
                        minList.get(seed),
                        maxList.get(seed),
                        avgList.get(seed),
                        varList.get(seed),
                        sumList.get(seed),
                        sg
                );
                if (!success) continue;
                unassigned.remove(seed);


                Set<Integer> candidates = region.getAreaNeighborSet();
                List<Integer> candidateList = new ArrayList<>(candidates);
                Collections.shuffle(candidateList, random);
                for (Integer candidate : candidateList) {
                    if (!unassigned.contains(candidate)) continue;
=======
        ArrayList<Integer> seedAreas = result.getValue1();

        // 初始化未分配区域（不包含作为 seed 的区域）
        Set<Integer> unassigned = new HashSet<>(allAreaIds);
        seedAreas.forEach(unassigned::remove);

        List<RegionWithVariance> regions = new ArrayList<>();
        Set<Integer> failedAssignments = new HashSet<>();

        List<Integer> seedAreaCopy = new ArrayList<>(seedAreas);
        for (Integer seed : seedAreaCopy) {
            if (regions.size() >= maxRegions) break;
            if (!seedAreas.contains(seed)) continue;

            RegionWithVariance region = new RegionWithVariance(regions.size() + 1);
            boolean success = region.addArea(
                    seed,
                    minList.get(seed),
                    maxList.get(seed),
                    avgList.get(seed),
                    varList.get(seed),
                    sumList.get(seed),
                    sg
            );

            if (!success) {
                failedAssignments.add(seed);
                continue;
            }

            seedAreas.remove(seed);
            unassigned.remove(seed);

            // 动态扩展邻居直到无法继续
            Set<Integer> candidates = new HashSet<>(region.getAreaNeighborSet());
            Set<Integer> triedCandidates = new HashSet<>();

            while (!candidates.isEmpty()) {
                if (region.satisfiable()) break;
                List<Integer> candidateList = new ArrayList<>(candidates);
//                Collections.shuffle(candidateList, random);

                boolean addedAny = false;
                for (Integer candidate : candidateList) {
                    if (!unassigned.contains(candidate)) {
                        triedCandidates.add(candidate);
                        continue;
                    }

>>>>>>> Stashed changes
                    boolean addedSuccess = region.addArea(
                            candidate,
                            minList.get(candidate),
                            maxList.get(candidate),
                            avgList.get(candidate),
                            varList.get(candidate),
                            sumList.get(candidate),
                            sg
                    );
<<<<<<< Updated upstream
                    if (addedSuccess && region.satisfiable()) {
                        unassigned.remove(candidate);
=======

                    if (addedSuccess) {
                        seedAreas.remove(candidate);
                        triedCandidates.add(candidate);
                        addedAny = true;

                        // 更新邻居集合
                        Set<Integer> newNeighbors = region.getAreaNeighborSet();
                        newNeighbors.removeAll(triedCandidates);
                        newNeighbors.removeAll(region.getAreaList());
                        candidates.addAll(newNeighbors);
                        break; // 每轮只加一个，避免爆炸式增长
>>>>>>> Stashed changes
                    } else {
                        region.removeArea(
                                candidate,
                                minList,
                                maxList,
                                avgList,
                                varList,
                                sumList,
                                sg
                        );
<<<<<<< Updated upstream
                    }
                }
                regions.add(region);
            }
        }
        System.out.println("Finished region growing. Total regions formed: " + regions.size());
        System.out.println("Remaining unassigned areas: " + unassigned.size());
        return regions;
    }

=======
                        triedCandidates.add(candidate);
                        failedAssignments.add(candidate);
                    }
                }

                if (!addedAny) break; // 无法继续扩展
            }

            // 检查最终区域是否满足约束
            if (region.satisfiable()) {
                regions.add(region);
            } else {
                failedAssignments.add(seed);
            }
        }

        // 把失败的尝试合并进未分配集合
        unassigned.addAll(failedAssignments);

        System.out.println("Finished region growing. Total regions formed: " + regions.size());
        System.out.println("Remaining unassigned areas: " + unassigned.size());
        Set<Integer> allAssignedAreas = new HashSet<>();

        System.out.println("Region assignments:");
        for (RegionWithVariance region : regions) {
//            System.out.println("Region " + region.getId() + ": " + region.getAreaList());
            allAssignedAreas.addAll(region.getAreaList());
        }

        System.out.println(allAssignedAreas.size());


        // 全部区域集合
        Set<Integer> allAreas = new HashSet<>(allAreaIds);

// 所有被分配的区域
        Set<Integer> assignedAreas = new HashSet<>();
        for (RegionWithVariance region : regions) {
            for (Integer area : region.getAreaList()) {
                if (!assignedAreas.add(area)) {
                    System.err.println("⚠️ Duplicate assignment detected: area " + area);
                }
            }
        }

// 检查 assigned + unassigned 是否等于总数
        Set<Integer> combined = new HashSet<>(assignedAreas);
        combined.addAll(unassigned);

        if (combined.size() != allAreas.size()) {
            System.err.println("❌ assigned + unassigned != total areas");
            System.err.println("Assigned: " + assignedAreas.size());
            System.err.println("Unassigned: " + unassigned.size());
            System.err.println("Combined: " + combined.size());
            System.err.println("Expected total: " + allAreas.size());

            // 输出缺失区域
            Set<Integer> missing = new HashSet<>(allAreas);
            missing.removeAll(combined);
            System.err.println("Missing areas: " + missing);
        } else {
            System.out.println("✅ Area assignment consistent: all areas accounted for.");
        }




        return regions;
    }



>>>>>>> Stashed changes
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

        ArrayList<Integer> seedAreas = new ArrayList<>();
        if (minUpperBound != Double.POSITIVE_INFINITY || maxLowerBound != -Double.POSITIVE_INFINITY) {
            for (Integer id : areas) {
                if (minAttr.get(id) <= minUpperBound || maxAttr.get(id) >= maxLowerBound)
                    seedAreas.add(id);
            }
        } else {
            seedAreas.addAll(areas);
        }

        return new Pair<>(labels, seedAreas);
    }


}
