var config = {

  todo: {
    statsd: "udp://" + getDockerHost() + ":8125",
    cassandra: {
      uris: ["cassandra://" + getDockerHost() + ':' + 39042],
      replicationFactor: 1
    }
  }
}