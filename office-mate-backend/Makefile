.PHONY: help setup build test clean docker-build docker-up docker-down docker-up-full docker-logs migrate init-db deploy-dev deploy-staging deploy-prod verify

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[1;33m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "$(BLUE)Officemate Backend - Available Commands$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}'
	@echo ""

setup: ## Initial setup - create .env from template
	@echo "$(BLUE)Setting up environment...$(NC)"
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "$(GREEN)✓ Created .env file from template$(NC)"; \
		echo "$(YELLOW)⚠ Please update .env with your configuration$(NC)"; \
	else \
		echo "$(YELLOW)⚠ .env file already exists$(NC)"; \
	fi

build: ## Build the application using Gradle
	@echo "$(BLUE)Building application...$(NC)"
	./gradlew clean build
	@echo "$(GREEN)✓ Build complete$(NC)"

test: ## Run tests
	@echo "$(BLUE)Running tests...$(NC)"
	./gradlew test
	@echo "$(GREEN)✓ Tests complete$(NC)"

clean: ## Clean build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	./gradlew clean
	@echo "$(GREEN)✓ Clean complete$(NC)"

docker-build: ## Build Docker image
	@echo "$(BLUE)Building Docker image...$(NC)"
	docker build -t officemate:latest .
	@echo "$(GREEN)✓ Docker image built$(NC)"

docker-build-prod: ## Build production Docker image
	@echo "$(BLUE)Building production Docker image...$(NC)"
	docker build -f Dockerfile.prod -t officemate:prod .
	@echo "$(GREEN)✓ Production Docker image built$(NC)"

docker-up: ## Start infrastructure services (PostgreSQL, Redis, MongoDB, LocalStack)
	@echo "$(BLUE)Starting infrastructure services...$(NC)"
	docker compose up -d
	@echo "$(GREEN)✓ Infrastructure services started$(NC)"
	@echo "$(YELLOW)Run 'make docker-logs' to view logs$(NC)"

docker-up-full: ## Start all services including application
	@echo "$(BLUE)Starting all services...$(NC)"
	docker compose --profile full up -d
	@echo "$(GREEN)✓ All services started$(NC)"
	@echo "$(YELLOW)Run 'make docker-logs' to view logs$(NC)"

docker-down: ## Stop all Docker services
	@echo "$(BLUE)Stopping Docker services...$(NC)"
	docker compose --profile full down
	@echo "$(GREEN)✓ Docker services stopped$(NC)"

docker-down-volumes: ## Stop Docker services and remove volumes
	@echo "$(BLUE)Stopping Docker services and removing volumes...$(NC)"
	docker compose --profile full down -v
	@echo "$(GREEN)✓ Docker services stopped and volumes removed$(NC)"

docker-logs: ## View Docker logs
	docker compose logs -f

docker-logs-app: ## View application logs only
	docker compose logs -f app

docker-restart: ## Restart all Docker services
	@echo "$(BLUE)Restarting Docker services...$(NC)"
	docker compose --profile full restart
	@echo "$(GREEN)✓ Docker services restarted$(NC)"

docker-ps: ## Show running Docker containers
	docker compose ps

migrate: ## Run database migrations
	@echo "$(BLUE)Running database migrations...$(NC)"
	@bash scripts/db/run-migrations.sh
	@echo "$(GREEN)✓ Migrations complete$(NC)"

init-db: ## Initialize database and DynamoDB tables
	@echo "$(BLUE)Initializing databases...$(NC)"
	@bash scripts/db/run-migrations.sh
	@bash scripts/init-dynamodb-tables.sh
	@echo "$(GREEN)✓ Database initialization complete$(NC)"

init-localstack: ## Initialize LocalStack AWS services (SNS, SES, KMS, DynamoDB)
	@echo "$(BLUE)Initializing LocalStack AWS services...$(NC)"
	@bash scripts/init-localstack.sh
	@echo "$(GREEN)✓ LocalStack initialization complete$(NC)"

init-aws: ## Initialize AWS services in production
	@echo "$(BLUE)Initializing AWS services...$(NC)"
	@echo "$(YELLOW)⚠ This will create resources in your AWS account$(NC)"
	@read -p "Continue? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		bash scripts/init-dynamodb-tables.sh; \
		echo "$(GREEN)✓ AWS services initialized$(NC)"; \
		echo "$(YELLOW)⚠ Please configure SNS, SES, and KMS manually$(NC)"; \
		echo "$(YELLOW)⚠ See docs/EXTERNAL_SERVICES_SETUP.md for details$(NC)"; \
	fi

verify: ## Verify setup and configuration
	@echo "$(BLUE)Verifying setup...$(NC)"
	@bash scripts/verify-setup.sh

run: ## Run application locally (requires infrastructure services)
	@echo "$(BLUE)Starting application...$(NC)"
	./gradlew bootRun

run-dev: ## Run application with dev profile
	@echo "$(BLUE)Starting application with dev profile...$(NC)"
	SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

deploy-dev: ## Deploy to development environment
	@echo "$(BLUE)Deploying to development...$(NC)"
	@bash scripts/deploy-dev.sh

deploy-staging: ## Deploy to staging environment
	@echo "$(BLUE)Deploying to staging...$(NC)"
	@bash scripts/deploy-staging.sh

deploy-prod: ## Deploy to production environment
	@echo "$(BLUE)Deploying to production...$(NC)"
	@bash scripts/deploy-prod.sh

# Docker Compose shortcuts
up: docker-up ## Alias for docker-up
down: docker-down ## Alias for docker-down
logs: docker-logs ## Alias for docker-logs
ps: docker-ps ## Alias for docker-ps

# Development workflow
dev-start: docker-up init-localstack ## Start development environment
	@echo "$(GREEN)✓ Development environment ready$(NC)"
	@echo "$(YELLOW)Run 'make run' to start the application$(NC)"

dev-stop: docker-down ## Stop development environment

dev-reset: docker-down-volumes docker-up init-db ## Reset development environment
	@echo "$(GREEN)✓ Development environment reset$(NC)"

# Production workflow
prod-build: docker-build-prod ## Build production image

prod-up: ## Start production services
	@echo "$(BLUE)Starting production services...$(NC)"
	docker compose -f docker-compose.prod.yml up -d
	@echo "$(GREEN)✓ Production services started$(NC)"

prod-down: ## Stop production services
	@echo "$(BLUE)Stopping production services...$(NC)"
	docker compose -f docker-compose.prod.yml down
	@echo "$(GREEN)✓ Production services stopped$(NC)"

prod-logs: ## View production logs
	docker compose -f docker-compose.prod.yml logs -f

# Health checks
health: ## Check application health
	@curl -f http://localhost:8080/actuator/health || echo "$(RED)Application is not healthy$(NC)"

health-services: ## Check all services health
	@echo "$(BLUE)Checking services health...$(NC)"
	@echo "PostgreSQL:"
	@docker compose exec postgres pg_isready -U postgres || echo "$(RED)PostgreSQL is not ready$(NC)"
	@echo "Redis:"
	@docker compose exec redis redis-cli ping || echo "$(RED)Redis is not ready$(NC)"
	@echo "MongoDB:"
	@docker compose exec mongodb mongosh --eval "db.adminCommand('ping')" || echo "$(RED)MongoDB is not ready$(NC)"
