package com.pragatix.dto;

import java.util.Map;

public class DepartmentPerformanceResponse {
    private String departmentName;
    private double overallAverage;
    private long totalStudents;
    private Map<String, Double> yearWiseAverage;

    public DepartmentPerformanceResponse() {
    }

    public DepartmentPerformanceResponse(String departmentName, double overallAverage, long totalStudents,
            Map<String, Double> yearWiseAverage) {
        this.departmentName = departmentName;
        this.overallAverage = overallAverage;
        this.totalStudents = totalStudents;
        this.yearWiseAverage = yearWiseAverage;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public double getOverallAverage() {
        return overallAverage;
    }

    public void setOverallAverage(double overallAverage) {
        this.overallAverage = overallAverage;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public Map<String, Double> getYearWiseAverage() {
        return yearWiseAverage;
    }

    public void setYearWiseAverage(Map<String, Double> yearWiseAverage) {
        this.yearWiseAverage = yearWiseAverage;
    }
}
