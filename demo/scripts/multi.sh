# First
docker run --rm --name=nats1 -p 4222:4222 -p 6222:6222 -p 8222:8222 -v "/home/piotr/git/going-nats/conf/":"/etc/nats/" nats -c /etc/nats/multi1.conf
# docker run --rm --name=nats1 -p 4222:4222 -p 6222:6222 -p 8222:8222 -v "/Users/pciruk/hub/going-nats/conf/":"/etc/nats/" nats -c /etc/nats/multi1.conf

# Second
docker run --rm --name=nats2 --link nats1 -p 4223:4222 -p 6223:6222 -p 8223:8222 -v "/home/piotr/git/going-nats/conf/":"/etc/nats/" nats -c /etc/nats/multi2.conf
# docker run --rm --name=nats2 --link nats1 -p 4223:4222 -p 6223:6222 -p 8223:8222 -v "/Users/pciruk/hub/going-nats/conf/":"/etc/nats/" nats -c /etc/nats/multi2.conf


# Third
docker run --rm --name=nats3 --link nats2 -p 4224:4222 -p 6224:6222 -p 8224:8222 -v "/home/piotr/git/going-nats/conf/":"/etc/nats/" nats -c /etc/nats/multi3.conf
# docker run --rm --name=nats3 --link nats2 -p 4224:4222 -p 6224:6222 -p 8224:8222 -v "/Users/pciruk/hub/going-nats/conf/":"/etc/nats/" nats -c /etc/nats/multi3.conf
