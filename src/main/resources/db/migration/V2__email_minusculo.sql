-- Normaliza e-mails existentes e garante unicidade sem diferenciar maiúsculas de minúsculas.
-- A restrição UNIQUE da V1 (uk_users_email) passa a ser case-insensitive na prática, pois a
-- restrição CHECK abaixo obriga os e-mails a estarem sempre em minúsculas.
-- A migration falha (de propósito) se já existirem e-mails que diferem apenas pela caixa.
UPDATE users SET email = LOWER(TRIM(email)) WHERE email IS NOT NULL;

ALTER TABLE users ADD CONSTRAINT ck_users_email_minusculo CHECK (email IS NULL OR email = LOWER(email));
