FROM clojure:openjdk-11-tools-deps-1.10.3.933

WORKDIR /tmp

RUN \
    apt-get update && \
    apt-get install -y curl make && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /usr/src/app

RUN curl -fsSL https://deb.nodesource.com/setup_16.x | bash -

RUN \
    apt-get update && \
    apt-get install -y nodejs

WORKDIR /usr/src/app

COPY package.json package-lock.json deps.edn /usr/src/app/

RUN ls -l /usr/src/app/*

RUN npm install

RUN clojure -P -M:dev:cljs:compile:uberjar

COPY . /usr/src/app

RUN make

EXPOSE 4000

CMD ["make", "run"]
