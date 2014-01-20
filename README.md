SIPUCE
======

A simplified UCE implementation that uses SIP for signalling and Socketswitching for fast connection setup.
Like ICE, the SIPUCE agent uses SIP to exchange endpoints and initiate the calls. However, unlike ICE, UCE uses TCP connections and simultaneously opens a relay and a holepunching connection to connect to the target. Since relaying is usually faster than holepunching the peers start exchanging data as soon as the relay connection is established. However, a direct peer-to-peer connection is prefered. When the holepunching process is successful, the peers use a novel technique, called `socketswitching` to switch from the relayed to the direct connection. This combines the fast connection setup of relayed connections with the ressource light and more secure holepunching connections.

The code was written through the course of a supervised student project. It is partly based on the original UCE sources (see UCE) and other unpublished code from student projects. It is released under the GNU GENERAL PUBLIC LICENSE 3 or newer. Please note that this is a research project and not a final or productive product. There are currently no plans to continue the project.

