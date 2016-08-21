var createLogger = Java.type('org.slf4j.LoggerFactory').getLogger;

var log = createLogger('config.log');

function mesosPortAt(index, defaultPort) {
  var fromMesos = env('PORT' + index);
  var portReturned = fromMesos ? parseInt(fromMesos) : defaultPort;
  log.info('Mesos Port At ' + index + ' was ' + portReturned + ' default was ' + defaultPort);
  return portReturned;
}

function getDockerHostAndPort() {
  var socatPort = env('SOCAT_PORT') ? env('SOCAT_PORT') : '2375';
  return getDockerHost() + ':' + socatPort;
}

function getDockerHost() {
  try {
    if (isMacOS() && !env('DOCKER_HOST')) {
      var vb = shell('docker info').split('\n').filter(function (line) {
        return line.contains('provider=virtualbox');
      });
      if (vb.length) {
        return '192.168.99.100';
      } else {
        return '127.0.0.1';
      }
    } else {
      return dockerHostOrDefault('127.0.0.1');
    }
  }catch (err) {
    log.error("not connected to DOCKER");

    return '192.168.99.100';
  }
}
