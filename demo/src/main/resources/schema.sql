CREATE TABLE IF NOT EXISTS `user` (
    `user_id`			BIGINT			NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `email`				VARCHAR(255)	NOT NULL,
    `password`			VARCHAR(255)	NOT NULL,
    `username`			VARCHAR(30)		NULL,
    `nickname`			VARCHAR(30)		NULL
);

CREATE TABLE IF NOT EXISTS `dexcom` (
    `dexcom_id`			BIGINT			NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id`			BIGINT			NOT NULL,
    `is_connected`		VARCHAR(20)		NULL,
    `max_glucose`		INT			    NOT NULL,
    `min_glucose`		INT			    NOT NULL,
    `last_egv_time`		TIMESTAMP		NULL
    );

CREATE TABLE IF NOT EXISTS `dexcom_auth` (
    `dexcom_id`			BIGINT			NOT NULL PRIMARY KEY,
    `access_token`		TEXT			NULL,
    `refresh_token`		TEXT        	NULL,
    `updated_at`		TIMESTAMP		NULL,
    `expires_in`		TIMESTAMP		NULL
    );


CREATE TABLE IF NOT EXISTS `glucose` (
    `glucose_value_id`	BIGINT			NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `dexcom_id`			BIGINT			NOT NULL,
    `value`				INT			    NULL,
    `displayApp`		VARCHAR(100)	NULL,
    `trend`				VARCHAR(20)		NULL,
    `recorded_at`		TIMESTAMP		NOT NULL
);