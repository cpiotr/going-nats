# First
docker run --rm --name=nats1 -p 4222:4222 -p 6222:6222 -p 8222:8222 -v "/home/piotr/git/going-nats/conf/":"/etc/nats/" nats -c /etc/nats/multi1.conf

# Second
docker run --rm --name=nats2 --link nats1 -p 4223:4222 -p 6223:6222 -p 8223:8222 -v "/home/piotr/git/going-nats/conf/":"/etc/nats/" nats -c /etc/nats/multi2.conf