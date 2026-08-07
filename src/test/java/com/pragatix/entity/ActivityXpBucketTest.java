package com.pragatix.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Activity XP bucket allocation rules.
 *
 * Business Rule:
 *   XP bucket is determined EXCLUSIVELY by the Activity Subgroup category.
 *   Mode (Individual / Group) determines HOW the activity is performed,
 *   NOT which XP bucket receives the points.
 *
 *   Must (Individual) / Must (Group)  →  Must XP ONLY
 *   Individual                        →  Individual XP ONLY
 *   Group                             →  Group XP ONLY
 */
public class ActivityXpBucketTest {

    private Activity makeActivity(String subgroupCategory, String modeType) {
        Activity activity = new Activity();
        activity.setXpType("Reward");
        activity.setModeType(modeType);

        if (subgroupCategory != null) {
            ActivitySubgroup subgroup = new ActivitySubgroup();
            subgroup.setCategory(subgroupCategory);
            subgroup.setName(subgroupCategory.equals("must") ? "Must (Individual)" : subgroupCategory);
            activity.setSubgroup(subgroup);
        }
        return activity;
    }

    // ── Must (Individual) ────────────────────────────────────────────────────

    @Test
    void mustIndividual_onlyMustXpEligible() {
        Activity act = makeActivity("must", "Individual");
        assertTrue(act.isMustXpEligible(),       "Must activity must be Must XP eligible");
        assertFalse(act.isIndividualXpEligible(), "Must activity must NOT be Individual XP eligible");
        assertFalse(act.isGroupXpEligible(),      "Must activity must NOT be Group XP eligible");
    }

    // ── Must (Group) ─────────────────────────────────────────────────────────

    @Test
    void mustGroup_onlyMustXpEligible() {
        Activity act = makeActivity("must", "Group");
        assertTrue(act.isMustXpEligible(),       "Must Group activity must be Must XP eligible");
        assertFalse(act.isIndividualXpEligible(), "Must Group activity must NOT be Individual XP eligible");
        assertFalse(act.isGroupXpEligible(),      "Must Group activity must NOT be Group XP eligible");
    }

    // ── Individual ───────────────────────────────────────────────────────────

    @Test
    void individual_onlyIndividualXpEligible() {
        Activity act = makeActivity("individual", "Individual");
        assertFalse(act.isMustXpEligible(),      "Individual activity must NOT be Must XP eligible");
        assertTrue(act.isIndividualXpEligible(),  "Individual activity must be Individual XP eligible");
        assertFalse(act.isGroupXpEligible(),      "Individual activity must NOT be Group XP eligible");
    }

    // ── Group ────────────────────────────────────────────────────────────────

    @Test
    void group_onlyGroupXpEligible() {
        Activity act = makeActivity("group", "Group");
        assertFalse(act.isMustXpEligible(),      "Group activity must NOT be Must XP eligible");
        assertFalse(act.isIndividualXpEligible(), "Group activity must NOT be Individual XP eligible");
        assertTrue(act.isGroupXpEligible(),       "Group activity must be Group XP eligible");
    }

    // ── Total XP is always updated (independent of bucket) ───────────────────
    // The total XP update is done unconditionally in XpEngineService so no
    // test needed here — confirmed unchanged in XpEngineService.java line 175.

    // ── Penalty XP type must never enter any bucket ──────────────────────────

    @Test
    void penaltyXpType_noBuckets() {
        Activity act = makeActivity("must", "Individual");
        act.setXpType("Penalty");
        assertFalse(act.isMustXpEligible(),      "Penalty activity must NOT be Must XP eligible");
        assertFalse(act.isIndividualXpEligible(), "Penalty activity must NOT be Individual XP eligible");
        assertFalse(act.isGroupXpEligible(),      "Penalty activity must NOT be Group XP eligible");
    }

    // ── Must subgroup case-insensitivity ─────────────────────────────────────

    @Test
    void mustCategory_caseInsensitive() {
        Activity act = makeActivity("MUST", "Individual");
        assertTrue(act.isMustXpEligible(),       "MUST (uppercase) must be Must XP eligible");
        assertFalse(act.isIndividualXpEligible(), "MUST (uppercase) must NOT be Individual XP eligible");
    }
}
