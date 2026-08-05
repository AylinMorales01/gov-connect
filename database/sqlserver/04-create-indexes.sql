CREATE INDEX IDX_CONTRACT_STATUS
    ON contracts(status);

CREATE INDEX IDX_CONTRACT_END_DATE
    ON contracts(end_date);

CREATE INDEX IDX_COLLECTION_DATE
    ON collections(collection_date);

CREATE INDEX IDX_COLLECTION_DEPARTMENT
    ON collections(department_id);