[

## Error Codes
- 0: Worked perfectly
- 1: Couldn't find document
- 2: Error in user input
- 3: Internal system error
- 4: Feature is not yet complete
- 5: Program has reached an invalid state
- 6: Rate limit hit

## Server response codes
- 404: Content not found
- 405: Param not supported
- 413: Content to large
- 429: Rate limit hit

Server spec:
GET: /
  returns whatever you want. Probably some info and links to the github
GET: /raw/:id
  returns the packet named `:id` in raw format.
GET: /down/:id/:filename?
  returns the packet named `:id` with a download flag in the response header.
  `:filename` is optional but can be used to specify the name of the downloaded file.
GET: /:id
  returns the packet named `:id` in the best possible format
POST /submit/:param
  submits the body of the request with optional server-specific parameter.
  See the [server parameters] section for more information on correct defaults
  optional headers:
    - name: the name of the packet, does not need to be respected by the server

Disallowed resource names: these should 404, and packets should not be allowed to be created with this name
 - `raw`: as `/raw` would hit two endpoints

Reserved resource names: these should return content from the server like a normal packet, but are more standardized.
Packets can not be made with any of these names
 - `help`: general info about adn and the server
 - `info`: alias for help
 - `stats`: general stats about the server
 - `log`: a public log for the server