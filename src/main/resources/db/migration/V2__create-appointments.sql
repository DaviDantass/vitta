create table appointments(

    id bigint not null auto_increment,
    doctor_id bigint not null,
    patient varchar(100) not null,
    date_time datetime not null,

    primary key(id),
    constraint fk_appointments_doctor_id foreign key(doctor_id) references doctors(id)

);
