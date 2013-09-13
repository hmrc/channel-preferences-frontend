# DOCKER-VERSION 0.4.0

from nickstenning/java7
run DEBIAN_FRONTEND=noninteractive apt-get install -y unzip
add . /src
run unzip -d /src /src/govuk-tax/dist/govuk-tax-0.0.1-SNAPSHOT.zip
run rm -rf /src/govuk-tax

expose 8080

cmd ["sh", "-ex", "/src/start-docker.sh"]
