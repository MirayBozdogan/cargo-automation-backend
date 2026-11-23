-- Oturum anahtarları (JWT) bu sayıyı taşır. Sayı artınca o hesabın eski oturumları geçersiz olur.
-- Mevcut hesaplar 0 ile başlar; bu sürümden önce verilmiş, sayı taşımayan anahtarlar 0 kabul edilir.
ALTER TABLE users ADD COLUMN token_version integer DEFAULT 0 NOT NULL;
