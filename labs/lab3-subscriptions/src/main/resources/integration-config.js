var config = {
  subscription: {
    statsd: "udp://grafana.marathon.mesos:12103",
    cassandra: {
      uris: [
        "cassandra://node-0.cassandra.mesos:9042",
        "cassandra://node-1.cassandra.mesos:9042",
        "cassandra://node-2.cassandra.mesos:9042"
      ],
      replicationFactor: 2
    }
  },
  asset: {
    statsd: "udp://grafana.marathon.mesos:12103",
    cassandra: {
      uris: [
        "cassandra://node-0.cassandra.mesos:9042",
        "cassandra://node-1.cassandra.mesos:9042",
        "cassandra://node-2.cassandra.mesos:9042"
      ],
      replicationFactor: 2
    }
  },
  entitlement: {
    statsd: "udp://grafana.marathon.mesos:12103",
    cassandra: {
      uris: [
        "cassandra://node-0.cassandra.mesos:9042",
        "cassandra://node-1.cassandra.mesos:9042",
        "cassandra://node-2.cassandra.mesos:9042"
      ],
      replicationFactor: 2
    }
  }
};


