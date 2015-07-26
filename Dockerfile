FROM ubuntu:14.04
MAINTAINER Niklas Nielsen <nik@qni.dk>

RUN sudo apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF
RUN echo "deb http://repos.mesosphere.io/ubuntu trusty main" | sudo tee /etc/apt/sources.list.d/mesosphere.list
RUN cat /etc/apt/sources.list.d/mesosphere.list


RUN apt-get update -q

#Install Dependencies
RUN apt-get -qy install         \
    ca-certificates   \
    build-essential             \
    git       \
    make                        \
    mesos     \
    wget      \
    openjdk-6-jre-headless  \
    --no-install-recommends

RUN wget https://dl.bintray.com/sbt/debian/sbt-0.13.7.deb
RUN dpkg -i sbt-0.13.7.deb

RUN git clone https://github.com/mesosphere/mesosaurus.git
WORKDIR mesosaurus
RUN cd task && make
RUN sbt compile
