CREATE TABLE activity_completion_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    team_id BIGINT DEFAULT NULL,
    activity_id BIGINT NOT NULL,
    cc_id BIGINT DEFAULT NULL,
    proof_url VARCHAR(500),
    reason TEXT,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME,
    approved_at DATETIME,
    approved_by VARCHAR(100),
    rejected_reason TEXT,
    updated_at DATETIME,
    CONSTRAINT fk_acr_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_acr_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_acr_activity FOREIGN KEY (activity_id) REFERENCES activities(id),
    CONSTRAINT fk_acr_cc FOREIGN KEY (cc_id) REFERENCES users(id)
);
