echo "Building local image"
sbt clean 'Docker/publishLocal'

echo "Running the image"
docker run --rm show-time:latest
