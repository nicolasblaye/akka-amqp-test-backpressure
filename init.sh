sbt universal:packageZipTarball
cp target/universal/akka-amqp-example-0.1.tgz ./akka-amqp.tgz
docker build . -t producer-test
docker-compose up &
sleep 20
echo 'Starting Slow Consumer'
sbt run
