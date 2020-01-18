

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
- 406: Content to large
- 429: Rate limit hit

Server spec. Everything needs to be https.
GET: /raw/:id
  returns the packet named `:id`, 404 if it doesn't exist.
GET: /:id
  returns the packet named `:id` in html-form
POST /submit/:param
  submits the body of the request with optional server-specific parameter.
  See the [server parameters] section for more information on correct defaults
  optional headers:
    - name: the name of the packet, does not need to be respected by the server