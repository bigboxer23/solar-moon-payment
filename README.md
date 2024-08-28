[![CodeQL](https://github.com/bigboxer23/solar-moon-payment/actions/workflows/codeql.yml/badge.svg)](https://github.com/bigboxer23/solar-moon-payment/actions/workflows/codeql.yml)

# solar-moon-payment

This project is responsible for payment (stripe) related services.

It is designed to run as a standalone springboot webserver, and includes services for billing, creating new billing portals
and receiving webhook events from stripe itself around payments.

This isn't used in production as lambdas for specific endpoints are used instead (and included in [solar-moon-common](https://github.com/bigboxer23/solar-moon-common) 
and [solar-moon-payment-common](https://github.com/bigboxer23/solar-moon-payment-common))

## scripts
### `solar-moon-payments.service`

- service definition file for running this project as a linux service

### `install.sh`

- responsible for installing/reloading/restarting the service. It does not build and install the actual jar file ( `mvn package`
  takes care of this)
