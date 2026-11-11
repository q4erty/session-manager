INSERT INTO games(id, slug, title, docker_image, rate_per_minute, min_block_minutes, supports_keyboard, supports_mouse,
                  supports_gamepad, is_active)
VALUES (gen_random_uuid(), 'xonotic', 'Xonotic', 'xonotic-worker', 1.00, 1, TRUE, TRUE, FALSE, TRUE);
