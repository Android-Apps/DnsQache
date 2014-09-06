* DnsQache DNS and HTTP/HTTPS Proxy for Tether Root Users
--------------------------

DNS and HTTP/HTTPS Proxy caching for Tether Root Users

First and foremost:

*** Use this application at your own risk. It is possible that use of this
program may violate your carrier's Terms of Use/Terms of Service. Read the
DISCLAIMER.txt file before committing to the use of this application.***


Second, the author of this program would like to thank Harry Mue and Sofia
Lemons for the fantastic Android Wifi Tether application for its use in both
using that application and and in learning Android. Some of the code in
DnsQache borrow from ideas and indeed implementation from Harry's and Sofia's
code base, therefore this body of code carries his license and Copyright (duly
noted in the DISCLAIMER.txt and the code, where appropriate).

** PREREQUISITES **

1) A rooted Android phone with busybox -- you should know how to, and have
already, rooted your phone and get busybox operational on the same phone;
2) Your phone's kernel must be netfilter-enabled (most stock kernels support
netfilter (IPTABLES), but it's a good idea to check to be sure);
3) If using Harry Mue's and Sofia Lemons' wifi tether application, assure you
use a version released after July 18, 2013.

** General Description and Use **
This program enables the use of custom DNS name servers and caching as well as
HTTP/HTTPS proxy and caching for Android Rooted phones.


Custom DNS Provider and Query Caching

To use the program for just DNS caching and redirection, install and start the
program. Thereafter, check the options in the "System|Settings: DNS" menu and
tune to your liking.

The DNS cache is implemented via dnsmasq and is always active when DnsQache is
active. For Android versions lower than Jelly Bean, the application manages the
network settings (e.g., system properties and /etc/resolv.conf) so that all DNS
requests resolve via the local dnsmasq server. For Android versions Jelly Bean
and higher, the application sets rules via IPTABLES
(http://www.netfilter.org/projects/iptables/) to redirect all DNS requests to
the local dnsmasq server. In that way, DNS caching is always active when
DnsQache is active. This is true whether or not the phone is being used for
tethering.

For users of the fantastic Android Wifi Tether application
(https://code.google.com/p/android-wifi-tether), the authors of that
application, on Jul 18, 2013, merged in code fixes submitted by the author of
this program that prevents the wifi-tether application from listening on the
localhost interface, which is required for DNS caching and not used by
wifi-tether. To assure you are using the version of code in which that 'fix'
exists, you should be using wifi_tether_v3_4-experimental1.apk or higher (see
https://code.google.com/p/android-wifi-tether/downloads/list). Tethered users
gain the benefit of dns caching and proxy services provided by DnsMasq.

At first, it may be a goood idea to turn on 'Log Queries' in the DNS settings.
That will allow you to view all DNS queries made by your phone and to endpoint
to which the queries were redirected. To see those, you can use logcat (or an
application that displays the same) or use the "System|View Log" menu, the
latter of which will take a snapshot of the existing logcat log as it relates
to dnamasq and present the information. If no log shows (e.g., the log says the
log file is not available), then it means you did not turn on Log Queries and
restart the service, or the service failed due to bad settings. The most common
issue there is if you use custom IP addresses for the DNS name servers and
dnsmasq ultimately fails to start due to such configuration error.


Using the HTTP/HTTPS Proxy

Generally, for proxy use, clients (your laptop for example) can connect via
wifi to your phone, when tethering, and get access to the internet using the
mobile connection (4G, 3G, 2G) of your phone. That may occur through custom
ROMs as hative hotspot or via the wifi-tether application. When DnsQache is
active, custom DNS name servers and caching is automatically enabled for
tethered users as well as the phone.

To use proxy services, the "System|Settings: Proxy" menu. Once saved, the
options will take effect only after the service is (re)started. To restart,
simply 'stop' and 'start' the DnsQache service. To do that, return to the main
page of the app and the big 'DQ' will be either green or grey. If grey, it
means the service is not currently running, so just touch the DQ to start it.
If green (and you need to restart), just touch the DQ and give it time to shut
down (goes grey) and touch it again (start).

The Proxy settings allow for the use of either polipo
(http://www.pps.univ-paris-diderot.fr/~jch/software/polipo/) or tinyproxy
(https://banu.com/tinyproxy/). Tinyproxy does not perform page caching, as does
polipo, so the general preference ends up use of polipo.

The settings require that you to specify one or more CIDR ranges for client
addresses allowed to connect to the proxy. The CIDRs of interest generally are
the network addresses (e.g., 172.20.21.0/24) of your mobile hotspot (tether)
network. You can view that in your phone settings, or the settings of your
wifi-tether application. DnsQache sets the proxy connection port to 3128, so
any client you want to proxy *must* set the proxy port to 3128. A later release
will allow changing the port.

To have tethered users go through the proxy instead of directly NATed
connection through the phone, the tethered user must set their device
(computer, et al) to use a proxy. Set the proxy settings to use the IP
address of your tethering gateway (the IP address of your wifi when tethering
-- usually that is the ".1" address of the CIDR you specified for your
client addresses for tethering or mobile hotspot application). As noted, when
using the polipo proxy, proxy caching of content takes place on the phone,
though secured pages (HTTPS) should not end up cached. 


** Some Credits: **
The front page, and indeed the service layer code was inspired by, and to some
extent, borrows from the venerable android-wifi-tether project originally
authored by Harry Mue (harald.mue@gmail.com) and Sofia Lemons. While inspired
obviously DnsQache is different in its nature and thus the code is quite
different as a whole. Still -- reading and contributing to Harry's code was
what got things rolling, so that body of work deserves significant mention, and
that exists also in the sources.
