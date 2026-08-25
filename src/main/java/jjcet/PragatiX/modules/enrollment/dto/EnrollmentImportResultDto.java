package jjcet.PragatiX.modules.enrollment.dto;

import java.util.ArrayList;
import java.util.List;

public class EnrollmentImportResultDto {
    private int totalRows = 0;
    private int importedCount = 0;
    private int skippedCount = 0;
    private List<String> errors = new ArrayList<>();

    public EnrollmentImportResultDto() {}

    public EnrollmentImportResultDto(int totalRows, int importedCount, int skippedCount, List<String> errors) {
        this.totalRows = totalRows;
        this.importedCount = importedCount;
        this.skippedCount = skippedCount;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public void addError(String error) {
        this.errors.add(error);
        this.skippedCount++;
    }

    public void incrementImported() {
        this.importedCount++;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
