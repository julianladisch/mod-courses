The script `create-records` reads records from the various
subdirectories and inserts them at the various course-reserves WSAPI
endpoints to populate a skeletal Course Reserves application database.

It is configured by the `~/.okapi` file, which it expects to contain
assignments to `OKAPI_URL`, `OKAPI_TENANT` and `OKAPI_TOKEN`
variables. For example:

	OKAPI_URL=https://simmons-test-cr-okapi.hosted-folio.indexdata.com
	OKAPI_TENANT=sim
	OKAPI_TOKEN=123abc

One easy way to set this up is by creating the file with only
`OKAPI_URL` and `OKAPI_TENANT`, then running `okapi login` using
[the Okapi command-line
client](https://github.com/thefrontside/okapi.rb).
However, if you do not have this client, you can obtain the token by
whatever other means suits you -- e.g. logging in via Stripes and
copying the token from the HTTP headers exposed by the browser's
development tools.
