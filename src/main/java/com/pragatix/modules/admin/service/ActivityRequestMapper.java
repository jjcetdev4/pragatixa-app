package com.pragatix.modules.admin.service;

import com.pragatix.entity.Activity;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ActivityRequestMapper {

    public void mapBasicFields(Activity activity, Map<String, Object> body) {
        String name = (String) body.get("name");
        activity.setName(name);
        activity.setActivityName(name);

        String desc = (String) body.get("description");
        activity.setDescription(desc);
        activity.setActivityDescription(desc);

        activity.setFrequency((String) body.get("frequency"));
        activity.setOwnerDepartment("");
        activity.setOwnerSubrole("");

        Object evidenceObj = body.get("evidence");
        if (evidenceObj instanceof List) {
            List<?> evList = (List<?>) evidenceObj;
            activity.setEvidence(evList.stream().map(Object::toString).collect(Collectors.joining(", ")));
        } else if (evidenceObj instanceof String) {
            activity.setEvidence((String) evidenceObj);
        }

        if (body.containsKey("manualEvidenceName")) {
            Object manualEvObj = body.get("manualEvidenceName");
            activity.setManualEvidenceName(manualEvObj != null ? manualEvObj.toString() : null);
        } else {
            activity.setManualEvidenceName(null);
        }

        activity.setXp((String) body.get("xp"));
        activity.setCap(body.get("cap"));
        activity.setType((String) body.get("type"));
        activity.setModeType(body.get("type") != null ? (String) body.get("type") : "Individual");
        activity.setJustification((String) body.get("justification"));

        String xpType = "Reward";
        if (body.containsKey("xpType") && body.get("xpType") != null) {
            xpType = body.get("xpType").toString().trim();
        }
        activity.setXpType(xpType);

        if (body.containsKey("allowStudentRequest") && body.get("allowStudentRequest") != null) {
            Object val = body.get("allowStudentRequest");
            if (val instanceof Boolean) {
                activity.setAllowStudentRequest((Boolean) val);
            } else if (val instanceof String) {
                activity.setAllowStudentRequest(Boolean.parseBoolean((String) val));
            }
        }

        if (body.containsKey("streakEnabled") && body.get("streakEnabled") != null) {
            Object val = body.get("streakEnabled");
            if (val instanceof Boolean) {
                activity.setStreakEnabled((Boolean) val);
            } else if (val instanceof String) {
                activity.setStreakEnabled(Boolean.parseBoolean((String) val));
            }
        } else if (activity.getId() == null) {
            activity.setStreakEnabled(false);
        }

        if (body.containsKey("attendanceEngineEnabled") && body.get("attendanceEngineEnabled") != null) {
            Object val = body.get("attendanceEngineEnabled");
            if (val instanceof Boolean) {
                activity.setAttendanceEngineEnabled((Boolean) val);
            } else if (val instanceof String) {
                activity.setAttendanceEngineEnabled(Boolean.parseBoolean((String) val));
            }
            System.out.println("FORENSIC: ActivityRequestMapper - Parsed attendanceEngineEnabled as TRUE/FALSE: " + activity.getAttendanceEngineEnabled());
        } else if (activity.getId() == null) {
            activity.setAttendanceEngineEnabled(false);
            System.out.println("FORENSIC: ActivityRequestMapper - Key not found or null for CREATE. Set to FALSE.");
        }

        if (body.containsKey("attendanceRule") && body.get("attendanceRule") != null) {
            activity.setAttendanceRule(body.get("attendanceRule").toString().trim());
        } else if (activity.getId() == null) {
            activity.setAttendanceRule(null);
        }
    }

    public String extractXpCategory(Map<String, Object> body) {
        return (String) body.get("xpCategory");
    }

    public Object[] parseAwardConfiguration(Map<String, Object> body, Activity existingActivity) {
        Boolean awardEnabled = false;
        if (body.containsKey("awardEnabled")) {
            Object val = body.get("awardEnabled");
            if (val instanceof Boolean)
                awardEnabled = (Boolean) val;
            else if (val instanceof String)
                awardEnabled = Boolean.parseBoolean((String) val);
        }
        Integer awardXp = 0;
        if (body.containsKey("awardXp")) {
            Object val = body.get("awardXp");
            if (val instanceof Number)
                awardXp = ((Number) val).intValue();
            else if (val instanceof String) {
                try {
                    awardXp = Integer.parseInt((String) val);
                } catch (Exception ignored) {
                }
            }
        } else if (body.containsKey("xp")) {
            try {
                awardXp = Integer.parseInt(body.get("xp").toString());
            } catch (Exception ignored) {
            }
        }

        Boolean penaltyEnabled = false;
        if (body.containsKey("penaltyEnabled")) {
            Object val = body.get("penaltyEnabled");
            if (val instanceof Boolean)
                penaltyEnabled = (Boolean) val;
            else if (val instanceof String)
                penaltyEnabled = Boolean.parseBoolean((String) val);
        }
        Integer penaltyXp = 0;
        if (body.containsKey("penaltyXp")) {
            Object val = body.get("penaltyXp");
            if (val instanceof Number)
                penaltyXp = ((Number) val).intValue();
            else if (val instanceof String) {
                try {
                    penaltyXp = Integer.parseInt((String) val);
                } catch (Exception ignored) {
                }
            }
        }

        // Preservation logic for updates
        if (existingActivity != null && Boolean.TRUE.equals(existingActivity.getAttendanceEngineEnabled()) &&
                !body.containsKey("awardEnabled") && !body.containsKey("penaltyEnabled") &&
                !body.containsKey("awardXp") && !body.containsKey("penaltyXp")) {
            
            if (!body.containsKey("awardEnabled")) {
                awardEnabled = existingActivity.getAwardEnabled();
            }
            if (!body.containsKey("awardXp")) {
                awardXp = existingActivity.getAwardXp();
            }
            if (!body.containsKey("penaltyEnabled")) {
                penaltyEnabled = existingActivity.getPenaltyEnabled();
            }
            if (!body.containsKey("penaltyXp")) {
                penaltyXp = existingActivity.getPenaltyXp();
            }
        } else {
            // Backward compatibility
            if (!body.containsKey("awardEnabled") && !body.containsKey("penaltyEnabled")) {
                Integer passXp = 0;
                if (body.containsKey("passXp")) {
                    try {
                        passXp = Integer.parseInt(body.get("passXp").toString());
                    } catch (Exception ignored) {
                    }
                }
                Integer failXp = 0;
                if (body.containsKey("failXp")) {
                    try {
                        failXp = Integer.parseInt(body.get("failXp").toString());
                    } catch (Exception ignored) {
                    }
                }
                if (passXp > 0 || failXp > 0) {
                    awardEnabled = passXp > 0;
                    awardXp = passXp;
                    penaltyEnabled = failXp > 0;
                    penaltyXp = failXp;
                } else {
                    String reqXpType = body.containsKey("xpType") && body.get("xpType") != null
                            ? body.get("xpType").toString()
                            : "Reward";
                    if ("Penalty".equalsIgnoreCase(reqXpType) || "Discipline".equalsIgnoreCase(reqXpType)) {
                        penaltyEnabled = true;
                        penaltyXp = awardXp;
                        awardEnabled = false;
                        awardXp = 0;
                    } else if ("Mixed".equalsIgnoreCase(reqXpType)) {
                        awardEnabled = true;
                        penaltyEnabled = true;
                        penaltyXp = awardXp;
                    } else {
                        awardEnabled = true;
                        penaltyEnabled = false;
                        penaltyXp = 0;
                    }
                }
            }
        }

        return new Object[] { awardEnabled, awardXp, penaltyEnabled, penaltyXp };
    }

    public String parseAwardType(Map<String, Object> body) {
        String awardType = "Fixed XP";
        if (body.containsKey("awardType") && body.get("awardType") != null) {
            awardType = body.get("awardType").toString();
        }
        return awardType;
    }

    public String parseAwardFrequency(Map<String, Object> body) {
        String awardFrequency = "One Time";
        if (body.containsKey("awardFrequency") && body.get("awardFrequency") != null) {
            awardFrequency = body.get("awardFrequency").toString().trim();
        } else if (body.containsKey("resetPeriod") && body.get("resetPeriod") != null) {
            awardFrequency = body.get("resetPeriod").toString().trim();
        }
        return awardFrequency;
    }

    public Integer parseCap(Map<String, Object> body, String matchedFrequency) {
        Integer cap = 1;
        if (body.containsKey("cap") && body.get("cap") != null) {
            Object capVal = body.get("cap");
            if (capVal instanceof Number)
                cap = ((Number) capVal).intValue();
            else {
                try {
                    cap = Integer.parseInt(capVal.toString());
                } catch (Exception ignored) {
                }
            }
        } else if (body.containsKey("maximumAwards") && body.get("maximumAwards") != null) {
            Object maxA = body.get("maximumAwards");
            if (maxA instanceof Number)
                cap = ((Number) maxA).intValue();
            else {
                try {
                    cap = Integer.parseInt(maxA.toString());
                } catch (Exception ignored) {
                }
            }
        }
        if (matchedFrequency.equalsIgnoreCase("One Time") || matchedFrequency.equalsIgnoreCase("Manual")) {
            cap = 1;
        }
        return cap;
    }

    public List<String> parseAwardDays(Map<String, Object> body) {
        List<String> awardDays = null;
        if (body.containsKey("awardDays") && body.get("awardDays") != null) {
            Object adObj = body.get("awardDays");
            if (adObj instanceof List) {
                awardDays = ((List<?>) adObj).stream().map(Object::toString).collect(Collectors.toList());
            } else if (adObj instanceof String) {
                awardDays = List.of(((String) adObj).split(","));
            }
        }
        return awardDays;
    }

    public void mapRemainingConfiguration(Activity activity, Map<String, Object> body, String matchedCategory,
            boolean awardEnabled, Integer awardXp, boolean penaltyEnabled, Integer penaltyXp,
            String awardType, String matchedFrequency, Integer cap, List<String> awardDays) {

        activity.setXpCategory(matchedCategory);

        if (!awardEnabled)
            awardXp = 0;
        if (!penaltyEnabled)
            penaltyXp = 0;

        activity.setAwardEnabled(awardEnabled);
        activity.setAwardXp(awardXp);
        activity.setPenaltyEnabled(penaltyEnabled);
        activity.setPenaltyXp(penaltyXp);

        activity.setAwardType(awardType);
        activity.setAwardFrequency(matchedFrequency);
        activity.setResetPeriod(matchedFrequency);
        activity.setRepeatAllowed(!matchedFrequency.equalsIgnoreCase("One Time"));
        activity.setCap(cap);
        activity.setMaximumAwards(cap);

        if (awardDays != null && !awardDays.isEmpty()) {
            activity.setAwardDays(awardDays.stream().map(String::trim).collect(Collectors.joining(",")));
        } else if (body.containsKey("awardDays")) {
            activity.setAwardDays(null);
        }

        Boolean isMandatory = activity.getId() == null ? false : activity.isMandatory();
        if (body.containsKey("isMandatory")) {
            Object val = body.get("isMandatory");
            if (val instanceof Boolean)
                isMandatory = (Boolean) val;
            else if (val instanceof String)
                isMandatory = Boolean.parseBoolean((String) val);
        } else if (body.containsKey("mandatory")) {
            Object val = body.get("mandatory");
            if (val instanceof Boolean)
                isMandatory = (Boolean) val;
            else if (val instanceof String)
                isMandatory = Boolean.parseBoolean((String) val);
        }
        activity.setMandatory(isMandatory);

        Boolean evidenceRequired = true;
        if (body.containsKey("evidenceRequired")) {
            Object val = body.get("evidenceRequired");
            if (val instanceof Boolean)
                evidenceRequired = (Boolean) val;
            else if (val instanceof String)
                evidenceRequired = Boolean.parseBoolean((String) val);
        }
        activity.setEvidenceRequired(evidenceRequired);

        Integer displayOrder = 0;
        if (body.containsKey("displayOrder") && body.get("displayOrder") != null) {
            Object dispVal = body.get("displayOrder");
            if (dispVal instanceof Number)
                displayOrder = ((Number) dispVal).intValue();
            else {
                try {
                    displayOrder = Integer.parseInt(dispVal.toString());
                } catch (Exception ignored) {
                }
            }
        }
        activity.setDisplayOrder(displayOrder);

        String status = "ACTIVE";
        System.out.println("Mapper Trace -> Body Contains Status: " + body.containsKey("status"));
        System.out.println("Mapper Trace -> Body Status Value: " + body.get("status"));
        if (body.containsKey("status") && body.get("status") != null) {
            status = (String) body.get("status");
        }
        System.out.println("Mapper Trace -> Final Status Applied: " + status);
        activity.setStatus(status);
        activity.setMaxPoints(100);
    }
}
