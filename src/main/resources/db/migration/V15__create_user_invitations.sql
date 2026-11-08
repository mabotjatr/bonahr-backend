CREATE TABLE user_invitations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_user_invitations_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT uq_user_invitations_token_hash
        UNIQUE (token_hash)
);

CREATE INDEX idx_user_invitations_user_id
    ON user_invitations(user_id);