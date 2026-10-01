CREATE TABLE patients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    cpf VARCHAR(14) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_patients_email UNIQUE (email),
    CONSTRAINT uk_patients_cpf UNIQUE (cpf)
);
