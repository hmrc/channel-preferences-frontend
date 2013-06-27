# DOCKER-VERSION 0.4.0

from nickstenning/java7
add . /src

expose 8080

cmd ["/src/start", "-Dhttp.port=8080"]

