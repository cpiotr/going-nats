# Going NATS

## Single node
```
docker run --rm -p 4333:4222 nats
telnet localhost 4222
```

## Multiple nodes
```
docker run --rm --name=nats1 -p 4222:4222 -p 6222:6222 -p 8222:8222 -v "/home/piotr/git/going-nats/conf/":"/etc/nats/" nats -c /etc/nats/multi1.conf
docker run --rm --name=nats2 --link nats1 -p 4223:4222 -p 6223:6222 -p 8223:8222 -v "/home/piotr/git/going-nats/conf/":"/etc/nats/" nats -c /etc/nats/multi2.conf
```

```
telnet localhost 4222
sub subject1 id1
```

```
telnet localhost 4223
pub subject1 5
hello
```

## Headers
```
connect {"headers":true}
```

## Slow consumer heap inspection
```
jcmd `jcmd | grep -i slowconsumer | awk '{print $1}'` GC.class_histogram -all | head
```

## Joke
43 Why did the two Java methods get a divorce?
35 Because they had constant arguments
