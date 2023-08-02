# Start server
docker run --rm -p 4333:4222 nats

# Connect client
telnet localhost 4333
sub subject1 id1

# Connect producer
telnet localhost 4333
pub subject1 5
hello

