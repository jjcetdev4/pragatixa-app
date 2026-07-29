package com.pragatix.modules.leaderboard.dto.response;

import java.util.List;

public class FilterOptionsDto {
    private List<FilterItem> years;
    private List<FilterItem> departments;
    private List<FilterItem> sections;

    public FilterOptionsDto(List<FilterItem> years, List<FilterItem> departments, List<FilterItem> sections) {
        this.years = years;
        this.departments = departments;
        this.sections = sections;
    }

    public List<FilterItem> getYears() {
        return years;
    }

    public void setYears(List<FilterItem> years) {
        this.years = years;
    }

    public List<FilterItem> getDepartments() {
        return departments;
    }

    public void setDepartments(List<FilterItem> departments) {
        this.departments = departments;
    }

    public List<FilterItem> getSections() {
        return sections;
    }

    public void setSections(List<FilterItem> sections) {
        this.sections = sections;
    }

    public static class FilterItem {
        private String id;
        private String name;

        public FilterItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
