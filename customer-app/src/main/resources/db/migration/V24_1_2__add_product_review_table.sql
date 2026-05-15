create table customer_app.t_product_review
(
    id            uuid primary key,
    c_product_id int not null,
    c_rating      int not null,
    c_review      varchar(1000)
);
