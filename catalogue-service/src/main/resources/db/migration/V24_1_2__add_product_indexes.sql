create index if not exists idx_product_title on catalogue.t_product (lower(c_title));
create index if not exists idx_product_details on catalogue.t_product (lower(c_details));
