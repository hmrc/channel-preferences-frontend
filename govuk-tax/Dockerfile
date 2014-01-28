FROM docker.registry/java:7u51

RUN DEBIAN_FRONTEND=noninteractive apt-get install -y unzip
ADD . /src
RUN unzip -d /src /src/govuk-tax/target/universal/govuk-tax-*.zip
RUN rm -rf /src/govuk-tax

EXPOSE 8080

CMD ["sh", "-ex", "/src/start-docker.sh"]
