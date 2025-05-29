#!/bin/bash

iptables -P FORWARD ACCEPT
nft add table ip nat
nft add chain ip nat postrouting { type nat hook postrouting priority 100 \; }
nft add rule ip nat postrouting oifname "wlp0s20f3" masquerade
iptables -I DOCKER-USER -i enp1s0 -o br-c372a625f75d -j ACCEPT
