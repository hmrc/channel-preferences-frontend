# DOCKER-VERSION 0.4.0

from nickstenning/java7
run DEBIAN_FRONTEND=noninteractive apt-get install -y unzip
add . /src
run unzip -d /src /src/sa-prefs/dist/sa-prefs-0.0.1-SNAPSHOT.zip
run rm -rf /src/dist/sa-prefs

expose 8080

cmd ["sh", "-ex", "/src/start-docker.sh"]
