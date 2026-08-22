package jjcet.PragatiX.entity;

import java.time.LocalDateTime;

public interface SoftDeletable {
    boolean isDeleted();
    void setDeleted(boolean deleted);

    LocalDateTime getDeletedAt();
    void setDeletedAt(LocalDateTime deletedAt);

    LocalDateTime getPermanentDeleteAt();
    void setPermanentDeleteAt(LocalDateTime permanentDeleteAt);

    String getDeletedBy();
    void setDeletedBy(String deletedBy);
}
