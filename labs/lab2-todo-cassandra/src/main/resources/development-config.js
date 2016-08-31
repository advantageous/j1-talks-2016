var config = {

  todo: {
    statsd: "udp://" + getDockerHost() + ":8125",
    cassandra: {
      uri: "discovery:echo:service://" + getDockerHost() + ':' + 39042,
      replicationFactor: 1
    },
    discoveryURIs : [
    ]
  }
};
