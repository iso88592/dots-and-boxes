FROM mcr.microsoft.com/dotnet/sdk:9.0
WORKDIR /dab

RUN apt update && apt install -y ssh git

ENV ASPNETCORE_HTTP_PORTS=5000
ENV DOTNET_RUNNING_IN_CONTAINER=true
ENV ASPNETCORE_ENVIRONMENT=Development
ENV ASPNETCORE_URLS=http://+:5000

ARG SERVERHOST

EXPOSE 5000

COPY DotsAndBoxesClient/DotsAndBoxesClient .

COPY docker/InitialRepo/ /docker/InitialRepo/

RUN dotnet restore
RUN dotnet publish -c Release -o out

