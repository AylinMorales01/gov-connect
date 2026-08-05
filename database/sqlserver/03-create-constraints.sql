ALTER TABLE budgets
    ADD CONSTRAINT FK_BUDGET_DEPARTMENT
        FOREIGN KEY (department_id)
            REFERENCES departments(id);

ALTER TABLE contracts
    ADD CONSTRAINT FK_CONTRACT_DEPARTMENT
        FOREIGN KEY (department_id)
            REFERENCES departments(id);

ALTER TABLE collections
    ADD CONSTRAINT FK_COLLECTION_DEPARTMENT
        FOREIGN KEY (department_id)
            REFERENCES departments(id);

ALTER TABLE automation_logs
    ADD CONSTRAINT FK_LOG_USER
        FOREIGN KEY (user_id)
            REFERENCES users(id);