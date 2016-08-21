


#### DCOS commands
```
#Log into dcos after running env script for correct server
dcos auth login


#show app list
dcos marathon app list


#install cassandra framework/service
dcos package install cassandra

#show cassandra connection information
dcos cassandra connection

#List mesos tasks
dcos task

#Log into master node
ssh-add ~/.ssh/US-WEST-2-KEY-MYCOMPANY-001-D.pem
dcos node ssh --master-proxy --leader

```

## Build and deploy


#### Build and deploy to mesos
```
dcos marathon app remove sample-dcos2
gradle clean build distZip
aws s3 cp  ./build/distributions/sample-dcos-0.9.4.zip  s3://sample-deploy --acl public-read
dcos marathon app add mesos-deploy.json
```


#### Remove a service
```
dcos marathon app remove sample-dcos2
```

#### Read Todos
```
curl http://public-slave:10101/v1/todo-service/todo
```
In this case, I set an entry in /etc/hosts for the public-slave for my DCOS cluster instance.


#### Add a TODO
```
 curl -X POST http://public-slave:10101/v1/todo-service/todo \
 -d '{"name":"todo", "description":"hi", "id":"abc", "createdTime":1234}' -H "Content-type: application/json"
```


#### Query downstream services
```
 curl -X POST http://public-slave:10101/v1/todo-service/service  \
 -d '"discovery:dns:A:///sample-dcos2.marathon.mesos?port=8080"' -H "Content-type: application/json" | jq .
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   152  100    94  100    58    455    280 --:--:-- --:--:-- --:--:--   460
[
  "service://10.16.204.92:8080/",
  "service://10.16.204.94:8080/",
  "service://10.16.204.95:8080/"
]


```



## Configure Elasticsearch, Kibana and Logstash

Install Elasticsearch, Kibana and Logstash

```
dcos package install  elasticsearch
dcos package install  kibana
dcos package install  logstash
```

Modify logstash config in Marathon and restart.

#### Change protocol and args in logstash marathon config as follows
```

  "portDefinitions": [
    {
      "port": 10514,
      "protocol": "udp",
      "labels": {}
    },
    ...
  "args": [
    "-e",
    "input { udp { port => 10514 codec => json }} output { elasticsearch { hosts => 'elasticsearch-executor.elasticsearch.mesos:1025' codec => json } }",
    "-w",
    ...
```
This setups port for logstash.

#### Services should be reachable via from cluster
```
dig SRV _logstash._tcp.marathon.mesos
```

#### Multiple A records from within cluster
```
$ dig logstash.marathon.mesos
; <<>> DiG 9.10.2-P2 <<>> logstash.marathon.mesos
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 44095
;; flags: qr aa rd ra; QUERY: 1, ANSWER: 3, AUTHORITY: 0, ADDITIONAL: 0

;; QUESTION SECTION:
;logstash.marathon.mesos.       IN      A

;; ANSWER SECTION:
logstash.marathon.mesos. 60     IN      A       10.16.204.92
logstash.marathon.mesos. 60     IN      A       10.16.204.95
logstash.marathon.mesos. 60     IN      A       10.16.204.93

;; Query time: 1 msec
;; SERVER: 198.51.100.1#53(198.51.100.1)
;; WHEN: Mon Aug 15 20:38:01 UTC 2016
;; MSG SIZE  rcvd: 89

```


#### REST logstash lookup A record
```
 curl -X POST http://public-slave:10101/v1/todo-service/service  \
 -d '"discovery:dns:A:///logstash.marathon.mesos?port=10514"' -H "Content-type: application/json" | jq .
 
   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                  Dload  Upload   Total   Spent    Left  Speed
 100   152  100    97  100    55    635    360 --:--:-- --:--:-- --:--:--   646
 [
   "service://10.16.204.96:10514/",
   "service://10.16.204.92:10514/",
   "service://10.16.204.94:10514/"
 ]

```


####REST logstash lookup SRV record
```
$  curl -X POST http://public-slave:10101/v1/todo-service/service  \
 -d '"discovery:dns:SRV:///_logstash._udp.marathon.mesos?port=10514"' -H "Content-type: application/json" | jq .
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   322  100   259  100    63   1107    269 --:--:-- --:--:-- --:--:--  1116
[
  "service://logstash-m67wg-s2.marathon.mesos:10514/marathon.mesos?priority=0&weight=0",
  "service://logstash-7myb1-s5.marathon.mesos:10514/marathon.mesos?priority=0&weight=0",
  "service://logstash-om5ha-s4.marathon.mesos:10514/marathon.mesos?priority=0&weight=0"
]

```


#### Configure logstash using A record DNS lookup logback.xml
```
<configuration>


    <appender name="STASH-UDP" class="net.logstash.logback.appender.LogstashSocketAppender">
        <host>logstash.marathon.mesos:10514/host>
        <port>5001</port>
        <customFields>{"serviceName":"sample-dcos-qbit","serviceHost":"${HOST}","servicePort":"${PORT0}","serviceId":"sample-dcos-qbit-${HOST}-${PORT0}","serviceAdminPort":"${PORT1}"}</customFields>
    </appender>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>
                %magenta(%d{HH:mm:ss.SSS}) %yellow([%thread]) %highlight(%-5level) %cyan(%logger{15}) - %msg%n
            </pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="STASH-UDP"/>
    </root>

    <logger name="io.advantageous" level="DEBUG"/>

</configuration>

```

## Todo
* shutdown cassandra cluster before reconnecting
* add shutdown to todo service that calls close on TodoRepo
* add ELK stack and statsd stack to startup docker in gradle build


## Notes

Send statsd app to marathon.
```
curl -X POST -H "Content-type: application/json" -d @statsd.json http://172.17.0.6:8080/v2/apps 
```

