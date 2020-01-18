

all:
	./gradlew shadowJar
	@mkdir -p build
	mv build/libs/adn-v*all.jar ./build/adn.jar

install:
	mkdir -p /opt/adn -m 775
	mkdir -p /opt/adn/global -m 777
	mv ./build/adn.jar /opt/adn/adn.jar
	chmod 755 /opt/adn/adn.jar
	mv ./adn.bin /bin/adn

install-local: all
	@echo Installing globally, needs root
	sudo make install
	@mkdir -p ~/.adn/data
	@if [ -f ~/.adn/config ]; \
		then \
			echo config file already created;  \
		else \
			echo creating config at ~/.adn/config; \
			cat default.config > ~/.adn/config; \
	fi
