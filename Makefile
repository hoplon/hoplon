.PHONY: help docs deploy

help:
	@echo "USAGE: make [help|docs|deploy]"

docs: docs/index.html

deploy: .deploy.time

docs/index.html: $(shell find src -type f)
	lein marg -f index.html

.deploy.time: docs/index.html
	ghp-import -p docs
	date > .deploy.time
