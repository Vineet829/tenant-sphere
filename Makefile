
build:
	docker compose -f local.yml up --build -d --remove-orphans

up:
	docker compose -f local.yml up -d

down:
	docker compose -f local.yml down

down-v:
	docker compose -f local.yml down -v

show-logs:
	docker compose -f local.yml logs

show-logs-api:
	docker compose -f local.yml logs api

# Flyway applies database migrations when the API starts; there is no separate migrate step.

# Usage: make superuser EMAIL=admin@example.com USERNAME=community_admin (prompts for the password).
superuser:
	docker compose -f local.yml run --rm --no-deps --entrypoint java api \
		-Dloader.main=com.tenantsphere.admin.CreateAdminCommand -cp /app/app.jar \
		org.springframework.boot.loader.launch.PropertiesLauncher $(EMAIL) $(USERNAME)

db-volume:
	docker volume inspect tenant-sphere-spring-port_tenant_prod_postgres_data

mailpit-volume:
	docker volume inspect tenant-sphere-spring-port_tenant_prod_mailpit_data

estate-db:
	docker compose -f local.yml exec postgres sh -c 'psql -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"'
