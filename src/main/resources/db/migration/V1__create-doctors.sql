create table doctors(
    id bigint not null auto_increment,
    name varchar(100) not null,
    email varchar(100) not null unique,
    phone varchar(20) not null,
    medical_registration varchar(6) not null unique,
    specialty varchar(100) not null,

    primary key(id)
);

