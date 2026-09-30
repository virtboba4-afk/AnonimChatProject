CREATE TABLE profiles (
                          id UUID PRIMARY KEY,
                          nickname VARCHAR(100) NOT NULL UNIQUE,
                          age INTEGER NOT NULL,
                          preferred_language VARCHAR(10) NOT NULL,
                          matching_score DOUBLE PRECISION NOT NULL DEFAULT 100.0,
                          can_search BOOLEAN NOT NULL DEFAULT TRUE,
                          version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE reports (
                         id UUID PRIMARY KEY,
                         reporter_id UUID NOT NULL REFERENCES profiles(id),
                         reported_id UUID NOT NULL REFERENCES profiles(id),
                         reason VARCHAR(500) NOT NULL,
                         status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_reports_reporter_id ON reports(reporter_id);
CREATE INDEX idx_reports_reported_id ON reports(reported_id);