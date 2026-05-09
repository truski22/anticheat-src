.PHONY: up down logs test clean build build-libs

## Start all services
up:
	docker compose up --build -d

## Stop all services
down:
	docker compose down

## Follow logs
logs:
	docker compose logs -f

## Build shared libraries
build-libs:
	cd chessfraud-libs && mvn install -q -DskipTests || true

## Run all tests
test:
	@echo "Running Java tests..."
	cd chessfraud-libs/protocol && mvn test -q || true
	cd gateway && mvn test -q || true
	cd game-service && mvn test -q || true
	cd analysis-service && mvn test -q || true
	cd user-service && mvn test -q || true
	@echo "Running Python tests..."
	cd ml-service && python -m pytest -q || true

## Build without starting
build:
	docker compose build

## Remove volumes and clean up
clean:
	docker compose down -v --remove-orphans
	@echo "Cleaned up containers and volumes."
