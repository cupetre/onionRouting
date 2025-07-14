Peer to peer Networks / Onion Routing **

 Mix networks are routing protocols that create hard-to-trace communications
 by using a chain of proxy servers known as mixes which take in messages
 from multiple senders, shuffle them, and send them back out in random
 order to the next destination (possibly another mix node). This breaks the
 link between the source of the request and the destination, making it harder
 for eavesdroppers to trace end-to-end communications. Furthermore, mixes
 only know the node that it immediately received the message from, and the
 immediate destination to send the shuffled messages to, making the network
 resistant to malicious mix nodes.
 Each message is encrypted to each proxy using public key cryptography; the
 resulting encryption is layered like a Russian doll (except that each ”doll” is
 of the same size) with the message as the innermost layer. Each proxy server
 strips off its own layer of encryption to reveal where to send the message
 next. If all but one of the proxy servers are compromised by the tracer,
 untraceability can still be achieved against some weaker adversaries.
 The concept of mix networks was first described by David Chaum in 1981.
 Applications that are based on this concept include anonymous remailers
 (such as Mixmaster), onion routing, garlic routing, and key-based routing
 (including Tor, I2P, and Freenet).

  The goal is to have a working implementation of a P2P network with basic
 functionality of a mixnet. For simplicity’s sake, the network can strongly
 connected to avoid having to implement a DHT like p2p lookup functionality.
 Since the networking part of the project (peer discovery, message proxy,
 etc..) is inherently concurrent, and at the same time the system is distributed,
 a working implementation covers all three parts.
 Note: The scope of this project extends a bit out of the bounds of the
 course. It requires some basic understanding on networking, public key cryp
tography and security.

<img width="449" height="115" alt="image" src="https://github.com/user-attachments/assets/08cef083-f139-4fad-8321-9c96c2a07e37" />
