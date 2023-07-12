package edu.ucr.cs.pyneapple.utils.EMPUtils;

import edu.ucr.cs.pyneapple.utils.SpatialGrid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegionWithVariance implements Serializable {
    static boolean debug = false;
    static double minLowerBound, minUpperBound, maxLowerBound, maxUpperBound, avgLowerBound, avgUpperBound, sumLowerBound, sumUpperBound, countLowerBound, countUpperBound, varLowerBound, varUpperBound;
    List<Integer> areaList;
    int id;
    int numOfAreas;
    double average, max, min, sum, variance, varianceAverage, varianceSum, varianceSumSquare;
    double avgAcceptLow, avgAcceptHigh;
    Set<Integer> areaNeighborSet;
    public static void setRange(Double minLowerBound,
                                Double minUpperBound,
                                Double maxLowerBound,
                                Double maxUpperBound,
                                Double avgLowerBound,
                                Double avgUpperBound,
                                Double varianceLowerBound,
                                Double varianceUpperBound,
                                Double sumLowerBound,
                                Double sumUpperBound,
                                Double countLowerBound,
                                Double countUpperBound){
        RegionWithVariance.minLowerBound = minLowerBound;
        RegionWithVariance.minUpperBound = minUpperBound;
        RegionWithVariance.maxLowerBound = maxLowerBound;
        RegionWithVariance.maxUpperBound = maxUpperBound;
        RegionWithVariance.avgLowerBound = avgLowerBound;
        RegionWithVariance.avgUpperBound = avgUpperBound;
        RegionWithVariance.sumLowerBound = sumLowerBound;
        RegionWithVariance.sumUpperBound = sumUpperBound;
        RegionWithVariance.countLowerBound = countLowerBound;
        RegionWithVariance.countUpperBound = countUpperBound;
        RegionWithVariance.varLowerBound = varianceLowerBound;
        RegionWithVariance.varUpperBound = varianceUpperBound;
    }
    //Set<Integer>
    public RegionWithVariance(int id){
        super();
        numOfAreas = 0;
        average = 0;
        variance = 0;
        varianceAverage = 0;
        varianceSum = 0;
        varianceSumSquare = 0;
        this.id = id;
        this.avgAcceptLow = RegionWithVariance.avgLowerBound;
        this.avgAcceptHigh = RegionWithVariance.avgUpperBound;
        this.areaNeighborSet = new HashSet<Integer>();
        this.areaList = new ArrayList<Integer>();
        this.max = -Double.POSITIVE_INFINITY;
        this.min = Double.POSITIVE_INFINITY;
        this.sum = 0;
    }
    public boolean addArea(Integer id, long minAttrVal, long maxAttrVal, long avgAttrVal, long varAttrVal, long sumAttrVal, SpatialGrid sg){
        if (areaList.contains(id)){
            if(debug){
                System.out.println("Area " + id + " is already contained in the current region " + this.id + "!");
            }

            return false;
        }else{
            //System.out.println("Add " + id + " to " + this.getId() );
            this.areaList.add(id);
            areaNeighborSet.remove(id);
            for(Integer a: sg.getNeighbors(id)){
                if (!areaList.contains(a)){
                    areaNeighborSet.add(a);
                }
            }
            average = (average * numOfAreas + avgAttrVal) / (numOfAreas + 1);
            this.numOfAreas = this.numOfAreas + 1;
            avgAcceptLow = RegionWithVariance.avgLowerBound * (numOfAreas + 1) - average * numOfAreas;
            avgAcceptHigh = RegionWithVariance.avgUpperBound * (numOfAreas +1) - average * numOfAreas;
            //varianceAverage = (varianceAverage * numOfAreas + varAttrVal) / (numOfAreas + 1);
            varianceSum += varAttrVal;
            varianceSumSquare += Math.pow(varAttrVal, 2);
            //this.variance = 1 / (this.getCount() + 1.0) *  (this.variance * this.getCount() + this.getCount() / (this.getCount() + 1.0 ) * Math.pow((this.varianceAverage - varAttrVal), 2));
            this.variance = varianceSumSquare / this.getCount() - Math.pow(varianceSum, 2) / Math.pow(this.getCount(), 2);
            if(this.min > minAttrVal){
                this.min = minAttrVal;
            }
            if(this.max < maxAttrVal){
                this.max = maxAttrVal;
            }
            this.sum += sumAttrVal;

            if(debug){
                System.out.println("Area " + id + " added to region " +this.getId());
            }

            return true;
        }

    }
    public double getVariance(){
        return this.variance;
    }
    public double getVarianceAverage(){
        return this.varianceAverage;
    }
    public double getVarianceSum(){
        return this.varianceSum;
    }
    public double getVarianceSumSquare(){
        return this.varianceSumSquare;
    }
    public boolean removeArea(Integer id, ArrayList<Long> minAttr, ArrayList<Long> maxAttr, ArrayList<Long> avgAttr,  ArrayList<Long> varAttr, ArrayList<Long> sumAttr, SpatialGrid sg){
        if (!areaList.contains(id)){
            System.out.println("Area to be removed is not in the region: area Id " + id + " region Id " + this.getId());
            return false;
        }else{
            areaList.remove(id);
            areaNeighborSet.add(id);
            this.areaNeighborSet.clear();
            //System.out.println("Sum before removal： " + this.sum);
            this.sum = this.sum - sumAttr.get(id);
            //System.out.println("Area sum attr: " + sumAttr.get(id));
           // System.out.println("Sum after removal： " + this.sum);
            this.average = (this.average * numOfAreas - avgAttr.get(id)) / (numOfAreas - 1);
            //this.varianceAverage = (this.average * numOfAreas - avgAttr.get(id)) / (numOfAreas - 1);
            this.varianceSum -= varAttr.get(id);
            this.varianceSumSquare -= varAttr.get(id) * varAttr.get(id);
            this.variance = varianceSumSquare / this.getCount() - Math.pow(varianceSum, 2) / Math.pow(this.getCount(), 2);


            numOfAreas --;
            double oldMin = this.min;
            this.max = -Double.POSITIVE_INFINITY;
            this.min = Double.POSITIVE_INFINITY;
            for(Integer area: areaList){
                if(this.min > minAttr.get(area)){
                    this.min = minAttr.get(area);
                }
                if(this.max < maxAttr.get(area)){
                    this.max = maxAttr.get(area);
                }
                for(Integer neighborArea: sg.getNeighbors(area)){
                    if(!areaList.contains(neighborArea)){
                        areaNeighborSet.add(neighborArea);
                    }
                }
            }
            //System.out.println("Remove: min changes after removing " + minAttr.get(id) + " from " +oldMin + " to " +this.min);
            return true;
        }
    }
    public double getAverage(){
        return this.average;
    }
    public double getSum(){return this.sum;}
    public Set<Integer> getAreaNeighborSet(){
        return areaNeighborSet;
    }
    public Set<Integer> getRegionNeighborSet(int[] labels){
        Set <Integer> regionNeighborSet = new HashSet<Integer>();
        for(Integer a: this.areaNeighborSet){
            if(labels[a] <= 0)
                continue;
            regionNeighborSet.add(labels[a]);
        }
        return regionNeighborSet;
    }
    public int[] updateId(int newId, int[] labels){
        this.id = newId;
        for(Integer i: areaList){
            if(debug){
                System.out.println("Area " + i + " id changes from " + labels[i] + " to " + newId);
            }

            labels[i] = newId;
        }
        return labels;
    }
    public List<Integer> getAreaList(){
        return this.areaList;
    }
    public double getAcceptLow(){
        return this.avgAcceptLow;
    }
    public double getAcceptHigh(){
        return this.avgAcceptHigh;
    }

    public int getId() {
        return this.id;
    }
    public double getMin(){
        return this.min;
    }
    public double getMax(){
        return this.max;
    }
    public int getCount(){
        return this.numOfAreas;
    }
    public boolean acceptable(Integer area, ArrayList<Long> minAttr, ArrayList <Long> maxAttr, ArrayList<Long> avgAttr, ArrayList<Long> varAttr, ArrayList<Long> sumAttr){
        if(this.numOfAreas + 1 <= countUpperBound){
            if(this.sum + sumAttr.get(area) <= sumUpperBound){
                if((minAttr.get(area) < this.min && minAttr.get(area) <= minUpperBound && minAttr.get(area) >= minLowerBound) ||
                        minAttr.get(area) >= this.min){
                    if((maxAttr.get(area) > this.max && maxAttr.get(area) <= maxUpperBound && maxAttr.get(area) >= maxLowerBound) ||
                            maxAttr.get(area) <= this.max){
                        double tmpAvg = (this.numOfAreas * this.average + avgAttr.get(area)) / (this.numOfAreas + 1);
                        if(tmpAvg >= avgLowerBound && tmpAvg <= avgUpperBound){
                            double tmpVarSum = varianceSum + varAttr.get(area);
                            double tmpVarSumSquare = varianceSumSquare + varAttr.get(area) * varAttr.get(area);
                            double tmpVar = tmpVarSumSquare / (this.getCount() + 1.0) - Math.pow(tmpVarSum, 2) / Math.pow(this.getCount() + 1, 2);
                            if(tmpVar >= varLowerBound && tmpVar <= varUpperBound)
                                return true;
                            else{
                                if(debug){
                                    System.out.println("Adding area " +area + " to region " + this.id + " exceeds one of the var");
                                }
                            }
                        }else{
                            if(debug){
                                System.out.println("Adding area " +area + " to region " + this.id + " exceeds one of the avg");
                            }
                        }
                    }else{
                        if(debug){
                            System.out.println("Adding area " +area + " to region " + this.id + " exceeds one of the max");
                        }
                    }
                }else{
                    if(debug){
                        System.out.println("Adding area " +area + " to region " + this.id + " exceeds one of the Min");
                    }
                }
            }else{
                if(debug){
                    System.out.println("Adding area " +area + " to region " + this.id + " exceeds the sumUpperBound");
                }
            }
        }else{
            if(debug){
                System.out.println("Adding area " +area + " to region " + this.id + " exceeds the countUpperBound");
            }
        }
        return false;
    }

    public boolean removable(Integer area, ArrayList<Long> minAttr, ArrayList <Long> maxAttr, ArrayList<Long> avgAttr, ArrayList<Long> varAttr, ArrayList<Long> sumAttr, SpatialGrid sg){
        if(!areaList.contains(area)){
            System.out.println("Area " + area + " not removable because area not in the list of " + this.getId() + " " + areaList);
            return false;
        }
        if((this.numOfAreas - 1) >= RegionWithVariance.countLowerBound &&
                ((this.sum - sumAttr.get(area)) >= RegionWithVariance.sumLowerBound) &&
                ((this.average * numOfAreas - avgAttr.get(area)) / (numOfAreas - 1) >= RegionWithVariance.avgLowerBound) &&
                ((this.average * numOfAreas - avgAttr.get(area)) / (numOfAreas - 1) <= RegionWithVariance.avgUpperBound)){
            double tmpVarSum = varianceSum - varAttr.get(area);
            double tmpVarSumSquare = varianceSumSquare - varAttr.get(area) * varAttr.get(area);
            double tmpVar = tmpVarSumSquare / (this.getCount() - 1.0) - Math.pow(tmpVarSum, 2) / Math.pow(this.getCount() - 1, 2);
            if(!(tmpVar >= varLowerBound && tmpVar <= varUpperBound))
                return false;

            if(this.min == minAttr.get(area) || this.max == maxAttr.get(area)){
                Double tmpMin = Double.POSITIVE_INFINITY;
                Double tmpMax = -Double.POSITIVE_INFINITY;
                List<Integer> tmpList = new ArrayList<>();
                tmpList.addAll(this.areaList);
                tmpList.remove(area);
                for(Integer otherArea: tmpList){
                    //if(otherArea != area){
                        if(tmpMin > minAttr.get(otherArea)){
                            tmpMin = Double.valueOf(minAttr.get(otherArea));
                        }
                        if(tmpMax < maxAttr.get(otherArea)){
                            tmpMax = Double.valueOf(maxAttr.get(otherArea));
                        }
                   // }
                }
                //System.out.println("Able: New min after removing area with Min value " + minAttr.get(area) + " is " + tmpMin);
                if(tmpMin <= RegionWithVariance.minUpperBound && tmpMin >= RegionWithVariance.minLowerBound && tmpMax <= RegionWithVariance.maxUpperBound && tmpMax >= RegionWithVariance.maxLowerBound){
                    if(numOfAreas - 1 > countLowerBound && numOfAreas - 1 > 0){
                        return connectedAfterRemoval(area, sg);
                    }
                }


            }

        }

        return false;

    }
    public List<Integer> getBoundaryAreas(SpatialGrid r, int[] labels){
        List<Integer> boundaryArea = new ArrayList<>();
        for(Integer area: areaList){
            boolean neighborFromAnotherR = false;
            Set<Integer> pasin = r.getNeighbors(area);
            for(Integer a: pasin){   

                if(neighborFromAnotherR)
                    break;
                //if(labels[a] != -2 && labels[a] != labels[area])
                if(labels[a] != labels[area]){
                    neighborFromAnotherR = true;
                }
            }
            if(neighborFromAnotherR){
                boundaryArea.add(area);
            }
        }
        return boundaryArea;
    }
    public boolean connectedAfterRemoval(Integer area, SpatialGrid sg){
        List<Integer> leftAreas = new ArrayList<Integer>();
        for(int i = 0; i < this.areaList.size(); i++){
            leftAreas.add(this.areaList.get(i));
        }
        leftAreas.remove(area);
        List<Integer> connectedNeighbor = new ArrayList<Integer>();
        boolean[] visited =new boolean[leftAreas.size()];
        for(Integer i: sg.getNeighbors(leftAreas.get(0))){
            connectedNeighbor.add(i);
        }
        visited[0] = true;
        boolean grow = true;
        while (grow){
            grow = false;
            for(int i = 1; i < leftAreas.size(); i++){
                if(visited[i] == false && connectedNeighbor.contains(leftAreas.get(i))){
                    visited[i] = true;
                    for(Integer j: sg.getNeighbors(leftAreas.get(i))){
                        connectedNeighbor.add(j);
                    }
                    grow = true;
                }
            }
        }
        boolean onecc = true;
        List<Integer> cc1Areas = new ArrayList<>();
        for(int i = 0; i < visited.length; i++){
            if(visited[i] == false){
                onecc = false;
            }else{
                cc1Areas.add(leftAreas.get(i));
            }
        }
        return onecc;
    }
    public List<List<Integer>> connectedComponentsAfterRemoval(Integer area, SpatialGrid sg){
        List<Integer> leftAreas = new ArrayList<Integer>();
        for(int i = 0; i < this.areaList.size(); i++){
            leftAreas.add(this.areaList.get(i));
        }
        leftAreas.remove(area);

        List<List<Integer>> connectedComponents = new ArrayList<>();
        if(leftAreas.size() < 1){
            //System.out.println("Nothing remains after removal");
            return connectedComponents;
        }
        List<Integer> connectedNeighbor = new ArrayList<Integer>();
        boolean[] visited =new boolean[leftAreas.size()];
        boolean allVisited = false;
        while(!allVisited){
            List<Integer> cc = new ArrayList<>();
            int notVisitedIndex = 0;
            while(visited[notVisitedIndex] && notVisitedIndex < visited.length -1)
                notVisitedIndex += 1;
            if(notVisitedIndex == visited.length - 1 && visited[visited.length - 1] == true){
                allVisited = true;
                break;
            }
            for(Integer i: sg.getNeighbors(leftAreas.get(notVisitedIndex))){
                connectedNeighbor.add(i);
            }
            visited[notVisitedIndex] = true;
            cc.add(leftAreas.get(notVisitedIndex));
            boolean grow = true;
            while (grow){
                grow = false;
                for(int i = notVisitedIndex + 1; i < leftAreas.size(); i++){
                    if(visited[i] == false && connectedNeighbor.contains(leftAreas.get(i))){
                        visited[i] = true;
                        cc.add(leftAreas.get(i));
                        for(Integer j: sg.getNeighbors(leftAreas.get(i))){
                            connectedNeighbor.add(j);
                        }
                        grow = true;
                    }
                }
            }
            connectedComponents.add(cc);
        }

        return connectedComponents;
    }
    public RegionWithVariance mergeWith(RegionWithVariance expandR, ArrayList<Long> minAttr, ArrayList<Long> maxAttr, ArrayList<Long> avgAttr, ArrayList<Long> varAttr, ArrayList<Long> sumAttr, SpatialGrid sg) {
        if(expandR == null){
            System.out.println("Error, region to be merged is NULL! " + this.getId());
        }
        if(this.id == expandR.getId()){
            return this;
        }
        RegionWithVariance tmpR = new RegionWithVariance(-1);
        for(Integer a: this.areaList){
            tmpR.addArea(a, minAttr.get(a), maxAttr.get(a), avgAttr.get(a), varAttr.get(a), sumAttr.get(a),  sg);
        }
        for(Integer a: expandR.getAreaList()){
            tmpR.addArea(a, minAttr.get(a), maxAttr.get(a), avgAttr.get(a), varAttr.get(a), sumAttr.get(a),  sg);
        }
        //System.out.println("Merged region:" + tmpR.getCount());
        return tmpR;

    }
    public static double simpleVariance(int count, double varSum, double varSumSquare){
        double variance =  varSumSquare / count - Math.pow(varSum/count, 2);
        return variance;
    }
    public boolean satisfiable(){
        /*System.out.println("Min u "+ minUpperBound);
        System.out.println("Min l "+minLowerBound);
        System.out.println("Max u "+maxUpperBound);
        System.out.println("Max l "+maxLowerBound);
        System.out.println("avg u "+avgUpperBound);
        System.out.println("avg l "+avgLowerBound);
        System.out.println("var u "+varUpperBound);
        System.out.println("var l "+varLowerBound);
        System.out.println("sum u "+sumUpperBound);
        System.out.println("sum l "+sumLowerBound);
        System.out.println("count u" + countUpperBound);
        System.out.println("count l " +countLowerBound);*/
        return(this.min <= minUpperBound &&
                this.min >= minLowerBound &&
                this.max <= maxUpperBound &&
                this.max >= maxLowerBound &&
                this.average >= avgLowerBound &&
                this.average <= avgUpperBound &&
                this.variance >= varLowerBound &&
                this.variance <= varUpperBound &&
                this.sum >= sumLowerBound &&
                this.sum <= sumUpperBound &&
                this.numOfAreas >= countLowerBound &&
                this.numOfAreas <= countUpperBound);
    }
}