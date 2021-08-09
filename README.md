This application illustrates interacting with Okta management APIs using a web client.

The application requires a "privateKey.pem" file in the src/main/resources folder containing the private key (in X.509 PEM format) you generated for your Okta application. (See https://developer.okta.com/docs/guides/implement-oauth-for-okta-serviceapp/create-publicprivate-keypair/.)

The application expects the following application arguments:
- `okta-domain`: your Okta domain, e.g. dev-12345678.okta.com
- `client-id`: the client id of the service app in your Okta account. For more details, see https://developer.okta.com/docs/guides/implement-oauth-for-okta-serviceapp/overview/