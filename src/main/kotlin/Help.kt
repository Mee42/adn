package dev.mee42

const val VERSION = "v0.0.1"

val HELP_MENU = """
ADN $VERSION, a text-content delivery network.
Usage: adn [OPTIONS] [ID FOR OUTPUT]

Misc Arguments:
 -h, --help            outputs this help menu
     --version         outputs versioning information
     --license         outputs the project license
 -v, --verbose         enable verbose output
     
There are three modes that are used in adn: input, output, and server.
Options for a mode that isn't used will be ignored. Some options are used twice.

Input mode will take in the input from stdin
Options:
 -i, --in              force input mode
 -s, --server [server] the server to use. Respects aliases
 -O, --output-format   the output format to use. See [output formats] in the man pages
 -u, --url             force output format to URL
 -n, --name [name]     a requested ID for the server to optionally use

Output Mode will take in an ID from stdin or a free floating argumentCx
Options:
 -o, --out             force output mode
 -s, --server          the server to use. Respects aliases. Overrides passed in server.
 
Server Mode will serve an simple http server. ADN is designed to have many server implementations
This server is an in-memory server, and ignores the parameter.
It has no rate limiter, and serves pages with syntax highlighting.
Options:
     --run-server      force server mode
     --port [port]     set the port of the server
     
Example Usage:

Piping text to the default server
    $ echo "Hello, World!" | adn
    $ cat file.txt | adn
  Using a specific server
    $ echo "text" | adn -s [server url]
  Using the localhost cache
    $ echo "text" | adn -s localhost
  Requesting text from the default server
    $ adn [id]
  Requesting text from an id with a known server
    $ adn [server]:[id]
    $ adn -s [server] [id]
""".trimIndent()
