.PHONY: build install install-local install-root help

SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c
.ONESHELL:

help:
	@echo run make install to build and install

build:
	./gradlew shadowJar
	@mkdir -p build
	mv build/libs/adn-v*all.jar ./build/adn.jar

install-root:
	mkdir -p /opt/adn -m 755
	mkdir -p /opt/adn/global -m 777
	mv ./build/adn.jar /opt/adn/adn.jar
	chmod 755 /opt/adn/adn.jar
	cp ./adn.bin /bin/adn
	cp ./adnw /bin/adnw

install-local: build
	@echo Installing globally, escalating into root
	sudo make install-root
	@mkdir -p ~/.adn/data
	@if [ -f ~/.adn/config ]; \
		then \
			echo config file already created;  \
		else \
			echo creating config at ~/.adn/config; \
			cat default.config > ~/.adn/config; \
	fi

install: install-local