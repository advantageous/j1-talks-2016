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
  },
  message: {
    kafka: {
      zookeeperConnect: "192.168.99.100:2181",
      kafkaConnect: "192.168.99.100:9092",
      groupId: "group1",
      topic: "my-topic",
      clientId: "ReaktProducer",
      kafkaSerializationClass: "org.apache.kafka.common.serialization.StringSerializer",
      partitions: 2,
      replication: 1
  }
}
}