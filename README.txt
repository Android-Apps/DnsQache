* DnsQache DNS and HTTP/HTTPS Proxy for Tether Root Users
--------------------------

DNS and HTTP/HTTPS Proxy caching for Tether Root Users

First and foremost:

*** Use this application at your own risk. It is possible that use of this program may violate your carrier's Terms of Use/Terms of Service. Read the DISCLAIMER.txt file before committing to the use of this application.***


Second, the author of this program would like to thank Harry Mue and Sofia Lemons for the fantastic Android Wifi Tether application for its use in both using that application andd and in learning Android. Some of the code in DnsQache borrow from ideas and indeed implementation from Harry's and Sofia's code base, therefore this body of code carries his license and Copyright (duly noted in the DISCLAIMER.txt and the code, where appropriate).

This program enables the use of custom DNS name servers and caching as well as HTTP/HTTPS proxy and caching for Android Rooted phones.

The DNS cache is implemented via dnsmasq and is always active when DnsQache is active. For Android versions lower than Jelly Bean, the application manages the network settings (e.g., system properties and /etc/resolv.conf) so that all DNS requests resolve via the local dnsmasq server. For Android versions Jelly Bean and higher, the application sets rules via IPTABLES (http://www.netfilter.org/projects/iptables/) to redirect all DNS requests to the local dnsmasq server. In that way, DNS caching is always active when DnsQache is active. This is true whether or not the phone is being used for tethering.

For users of the venerable Android Wifi Tether application (https://code.google.com/p/android-wifi-tether), the authors of that application, on Jul 18, 2013, merged in code fixes submitted by the author of this program that prevents the wifi-tether application from listening on the localhost interfact, which is required for DNS caching and not used by wifi-tether. To assure you are using the version of code in which that 'fix' exists, you should be using wifi_tether_v3_4-experimental1.apk or higher (see https://code.google.com/p/android-wifi-tether/downloads/list). Tethered users gain the benefit of dns caching provided by DnsMasq.  
The Proxy settings allow for the use of either polipo (http://www.pps.univ-paris-diderot.fr/~jch/software/polipo/) or tinyproxy (https://banu.com/tinyproxy/). Tinyproxy does not perform page caching, as does polipo, so the general preference ends up use of polipo.

Generally, for proxy use, clients (your laptop for example) can connect via wifi to your phone, when tethering, and get access to the internet using the mobile connection (4G, 3G, 2G) of your phone. That may occur through custom ROMs as hative hotspot or via the wifi-tether application. When DnsQache is active, custom DNS name servers and caching is automatically enabled for tethered users as well as the phone.

To have tethered users go through the proxy instead of directly NATed connection through the phone, the . As noted, when using the polipo proxy, proxy caching of content takes place on the phone, though secured pages (HTTPS) should not end up cached. 

** PREREQUISITES **

1) A rooted Android phone;
2) Your phone's kernel must be netfilter-enabled. Most stock kernels support netfilter (IPTABLES), but it's a good idea to check to be sure.
3) If using Harry Mue's and Sofia Lemons' wifi tether application, assure you use a version released after July 18, 2013.

