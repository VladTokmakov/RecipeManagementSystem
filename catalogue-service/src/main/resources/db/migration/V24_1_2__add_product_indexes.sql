-- Ужесточение схемы catalogue: ограничения на пустое/пробельное название уже есть,
-- добавляем индекс для поиска по названию и описанию.
create index if not exists idx_product_title on catalogue.t_product (lower(c_title));
create index if not exists idx_product_details on catalogue.t_product (lower(c_details));
