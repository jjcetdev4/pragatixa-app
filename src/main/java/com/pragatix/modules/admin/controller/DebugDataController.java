package com.pragatix.modules.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DebugDataController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/api/debug/team_members")
    public List<Map<String, Object>> debugTeamMembers() {
        return jdbcTemplate.queryForList("DESCRIBE team_members");
    }

    @GetMapping("/api/debug/alter")
    public String debugAlter() {
        try {
            jdbcTemplate.execute("ALTER TABLE team_members MODIFY COLUMN reg_no VARCHAR(100) NULL DEFAULT NULL");
            return "SUCCESS";
        } catch (Exception e) {
            return "FAILED: " + e.getMessage();
        }
    }
}
