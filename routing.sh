#!/bin/bash -e

function checkif() {
	ip addr show $1 2>&1 >/dev/null
	echo $?
}

if [[ $# -ne 3 ]]; then
	echo "Please use these 3 parameters:"
	echo -e "\trouting.sh <host interface> <p2p interface> <bridge>"
	echo -e "\thost interface: most likely your wlan interface like wlp0s20f3"
	echo -e "\tp2p interface: a point-to-point interface between your router towards the client, most likely enp1s0"
	echo -e "\tbridge: your docker based bridge, most likely br-c372a625f75d"
	exit 1
fi

hostif="$1"

if [[ $(checkif ${hostif}) -ne 0 ]]; then
	echo "Interface ${hostif} is invalid!"
	exit 1
fi

p2pif="$2"

if [[ $(checkif ${p2pif}) -ne 0 ]]; then
	echo "Interface ${p2pif} is invalid!"
	exit 1
fi

bridge="$3"

if [[ $(checkif ${bridge}) -ne 0 ]]; then
	echo "Interface ${bridge} is invalid!"
	exit 1
fi
iptables -P FORWARD ACCEPT
nft add table ip nat
nft add chain ip nat postrouting { type nat hook postrouting priority 100 \; }
nft add rule ip nat postrouting oifname "${hostif}" masquerade
iptables -I DOCKER-USER -i ${p2pif} -o ${bridge} -j ACCEPT
