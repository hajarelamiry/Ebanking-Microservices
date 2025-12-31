SELECT 'CREATE DATABASE user_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'user_db')\gexec

SELECT 'CREATE DATABASE account_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'account_db')\gexec
