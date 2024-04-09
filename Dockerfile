FROM amazoncorretto:17

WORKDIR /app

COPY ./build/libs/chosim-0.0.1-SNAPSHOT.jar /app/maimu-api.jar
COPY ./entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]