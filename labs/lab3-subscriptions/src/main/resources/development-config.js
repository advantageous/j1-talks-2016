var config = {

  subscription: {
    statsd: "udp://" + getDockerHost() + ":8125",
    cassandra: {
      uris: ["cassandra://" + getDockerHost() + ':' + 39042],
      replicationFactor: 1
    }
  },
  asset: {
    statsd: "udp://" + getDockerHost() + ":8125",
    cassandra: {
      uris: ["cassandra://" + getDockerHost() + ':' + 39042],
      replicationFactor: 1
    }
  },
  entitlement: {
    statsd: "udp://" + getDockerHost() + ":8125",
    cassandra: {
      uris: ["cassandra://" + getDockerHost() + ':' + 39042],
      replicationFactor: 1
    }
  }
}