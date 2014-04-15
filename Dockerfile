FROM docker.tax.service.gov.uk/java:7u51

RUN DEBIAN_FRONTEND=noninteractive apt-get install -y unzip
ADD . /src
RUN unzip -d /src /src/target/universal/sa-prefs-*.zip

EXPOSE 8080

CMD ["sh", "-ex", "/src/start-docker.sh"]