-- Ужесточение схемы customer_app: авторство отзывов, ограничения целостности, индексы.
-- Отдельная миграция, потому что V24_1_2 уже применена (нельзя менять её checksum).

alter table customer_app.t_product_review
    add column if not exists c_user_id varchar(255),
    add column if not exists c_created_at timestamptz not null default now();

-- У старых (анонимных) отзывов автора нет. Даём каждому УНИКАЛЬНЫЙ суррогат:
-- одинаковое значение для всех нарушило бы unique (c_product_id, c_user_id),
-- если на один рецепт было несколько анонимных отзывов.
update customer_app.t_product_review
set c_user_id = 'legacy-anonymous-' || id::text
where c_user_id is null;

alter table customer_app.t_product_review
    alter column c_user_id set not null;

-- CHECK нельзя навесить, пока в таблице есть значения вне диапазона:
-- сначала приводим их к границам, иначе миграция упадёт на реальных данных.
update customer_app.t_product_review set c_rating = 1 where c_rating < 1;
update customer_app.t_product_review set c_rating = 5 where c_rating > 5;

alter table customer_app.t_product_review
    add constraint chk_product_review_rating check (c_rating between 1 and 5);

-- один отзыв на рецепт от пользователя: защита от накрутки.
-- Чистим возможные дубли (оставляем самый новый), иначе constraint не создастся.
delete from customer_app.t_product_review older
using customer_app.t_product_review newer
where older.c_product_id = newer.c_product_id
  and older.c_user_id = newer.c_user_id
  and older.id <> newer.id
  and (older.c_created_at, older.id::text) < (newer.c_created_at, newer.id::text);

alter table customer_app.t_product_review
    add constraint uk_product_review_product_user unique (c_product_id, c_user_id);

create index if not exists idx_product_review_product on customer_app.t_product_review (c_product_id);
create index if not exists idx_product_review_user on customer_app.t_product_review (c_user_id);

create index if not exists idx_favourite_product_user on customer_app.t_favourite_product (c_user_id);
create index if not exists idx_favourite_product_product on customer_app.t_favourite_product (c_product_id);
