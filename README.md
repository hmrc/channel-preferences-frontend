preferences-frontend
====================

UI for managing print suppression email addresses & preferences.

For setting and changing preferences, whole pages are returned. Partial views are provided for integration into other frontends (such as business-tax-account).

# Public API 

| Path                                                                | Supported Methods | Description
| ------------------------------------------------------------------- | ----------------- | -------------
| `/paperless/activate`                                               | PUT               | Decides if a paperless choice needs to be made
| `/paperless/email-address/change`                                   | GET, POST         | Displays/submits a form for changing a customer's email address
| `/paperless/email-address/change/confirmed`                         | GET               | Displays confirmation that a customer's email address has been changed
| `/paperless/stop`                                                   | GET, POST         | Displays/submits a form for switching a customer to paper
| `/paperless/stop/confirmed`                                         | GET               | Displays confirmation that a customer has switched to paper
| `/paperless/resend-validation-email`                                | POST              | Sends a new verification link to a customer, and then displays a confirmation page [More...](#post-paperlessresent-validation-email)   
| `/paperless/choose`                                                 | GET, POST         | Redirects to or submits a form for switching a customer to paperless
| `/paperless/choose/:cohort`                                         | GET               | Displays a form for switching a customer to paperless
| `/paperless/choose/nearly-done`                                     | GET               | Confirms that a user has submitted their preference for paper or paperless
| `/sa/print-preferences/:token`                                      | GET               | Redirects a user back to `return_url` with their current email address, if known    
| `/sa/print-preferences/verification/:token`                         | GET               | Submits the verification of a user's email address
| `/sa/print-preferences/assets/*file`                                | GET               | Serves up the test gif that is used by the portal to determine if the service is up    
| `/paperless/manage`                                                 | GET               | Returns an HTML partial with appropriate information and links for a customer to manage their paperless status [More...](#get-paperlessmanage)
| `/paperless/warnings`                                               | GET               | Returns an HTML partial which may contain warnings and links if a customer's paperless status is non-verified [More...](#get-paperlesswarnings)

### PUT /paperless/activate

Takes the following parameters:

| Name             | Description |
| ---------------- | ----------- |
| `returnUrl`      | The URL that the user will be redirected to at the end of any journeys resulting from this call, the URL will be passed to the redirectUserTo response |
| `returnLinkText` | The text that will be used to display the return URL, the parameter will be passed to the redirectUserTo response |

Responds with:

| Status                        | Description |
| ----------------------------- | ----------- |
| 412 Precondition failed       | If the user needs to be redirected to a preferences-frontend page to set their paperless options |
| 200 Ok                        | If the user has previously accepted paperless

When 200 is returned, the body of the response will contain details of the user's paperless status.  
`true` means that the user has signed up for paperless. It is `true` even if the email is not verified yet.  
`false` means that the user has decided to not opt in for paperless.

```json
{
    "paperless": true
}
```

When a precondition failed response is generated, the body of the response will contain a redirect url. An example response is:

```json
{
    "redirectUserTo":"/paperless/choose?returnUrl=ABdc123ReD6sFe&returnLinkText=gh32seWQ78fdE"
}
```


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
