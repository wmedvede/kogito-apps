ALTER TABLE job_details
    ADD COLUMN created TIMESTAMPTZ;

CREATE INDEX job_details_created_idx
    ON job_details (created);

UPDATE job_details
SET created = fire_time
WHERE created is null;