package jjcet.PragatiX.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DepartmentType {
    MAIN,
    SUB;

    @JsonCreator
    public static DepartmentType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MAIN;
        }
        for (DepartmentType type : DepartmentType.values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return MAIN;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
