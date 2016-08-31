var config = {
  todo: {
    statsd: "udp://grafana.marathon.mesos:12103",
    cassandra: {
      uri: "discovery:dns:SRV://nodes.cassandra.mesos"
    }
  },
  discoveryURIs : [
    "marathon://marathon.mesos:8080/"
  ]
};


