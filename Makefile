.PHONY: up down seed check logs

up:
	cp -n .env.example .env || true
	docker compose -f docker-compose.prod.yml up -d --build

down:
	docker compose -f docker-compose.prod.yml down

seed:
	@chmod +x scripts/seed.sh
	@./scripts/seed.sh

check:
	@./scripts/check.sh

logs:
	docker compose -f docker-compose.prod.yml logs -f api
