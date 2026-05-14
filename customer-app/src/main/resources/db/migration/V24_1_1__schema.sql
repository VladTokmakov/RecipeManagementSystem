create schema if not exists customer_app;

create table customer_app.t_favourite_product
(
    id        uuid primary key,
    c_product_id int not null,
    c_user_id    varchar(255) not null,
    unique (c_product_id, c_user_id)
);
