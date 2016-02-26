preferences-frontend
====================

UI for managing print suppression email addresses & preferences.

For setting and changing preferences, whole pages are returned. Partial views are provided for integration into other frontends (such as business-tax-account).

# Public API 

| Path                                                                | Supported Methods | Description
| ------------------------------------------------------------------- | ----------------- | -------------
| `/paperless/email-address/change`                                   | GET, POST         | Displays/submits a form for changing a customer's email address
| `/paperless/email-address/change/confirmed`                         | GET               | Displays confirmation that a customer's email address has been changed
| `/paperless/stop`                                                   | GET, POST         | Displays/submits a form for switching a customer to paper
| `/paperless/stop/confirmed`                                         | GET               | Displays confirmation that a customer has switched to paper
| `/paperless/resend-validation-email`                                | POST              | Sends a new verification link to a customer, and then displays a confirmation page [More...](#post-paperlessresent-validation-email)   
| `/paperless/choose`                                                 | GET, POST         | Redirects to or submits a form for switching a customer to paperless
| `/paperless/choose/:cohort`                                         | GET               | Displays a form for switching a customer to paperless
| `/paperless/choose/nearly-done`                                     | GET               | Confirms that a user has submitted their preference for paper or paperless
| `/paperless/upgrade`                                                | GET, POST         | Displays/submits a form for upgrading a customer to the latest terms and conditions for paperless
| `/paperless/upgrade/confirmed`                                      | GET               | Confirms that a user has submitted their acceptance or refusal of the latest terms and conditions for paperless
| `/sa/print-preferences/:token`                                      | GET               | Redirects a user back to `return_url` with their current email address, if known    
| `/sa/print-preferences/verification/:token`                         | GET               | Submits the verification of a user's email address
| `/sa/print-preferences/assets/*file`                                | GET               | Serves up the test gif that is used by the portal to determine if the service is up    
| `/paperless/manage`                                                 | GET               | Returns an HTML partial with appropriate information and links for a customer to manage their paperless status [More...](#get-paperlessmanage)
| `/paperless/warnings`                                               | GET               | Returns an HTML partial which may contain warnings and links if a customer's paperless status is non-verified [More...](#get-paperlesswarnings)


### POST /paperless/resend-validation-email


### GET /paperless/manage

Takes the following parameters:

| Name             | Description |
| ---------------- | ----------- |
| `returnUrl`      | The URL that the user will be redirected to at the end of any journeys starting from this partial |
| `returnLinkText` | The text that will be used to display the return URL |

### GET /paperless/warnings

Takes the following parameters:

| Name             | Description |
| ---------------- | ----------- |
| `returnUrl`      | The URL that the user will be redirected to at the end of any journeys starting from this partial |
| `returnLinkText` | The text that will be used to display the return URL |



Run functional test
===================

`sm --start ASSETS_FRONTEND -f`

`sbt functional:test`