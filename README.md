preferences-frontend
====================

UI for managing print suppression email addresses & preferences.

For setting and changing preferences, whole pages are returned. Partial views are provided for integration into other frontends (such as business-tax-account).

# Public API 

| Path                                                                | Supported Methods | Description
| ------------------------------------------------------------------- | ----------------- | -------------
| `/paperless/preferences`                                            | GET               | Returns a customer's current paperless preferences
| `/paperless/activate`                                               | PUT               | Decides if a paperless choice needs to be made, taking an optional body to specify a particular version of terms and conditions
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

### GET /paperless/preferences

Responds with:

| Status                        | Description |
| ----------------------------- | ----------- |
| 200 Ok                        | If the user has previously accepted paperless |
| 404 Not Found                 | If no preference is found for the user |

```json
{
  "termsAndConditions": {
    "generic": {
      "accepted": true
    },
    "taxCredits": {
      "accepted": true
    }
  },
  "email": {
    "email": "test@example.com",
    "isVerified": true,
    "hasBounces": false
  }
}
```

### PUT /paperless/activate

Takes the following parameters, which should all be encrypted and encoded using the query parameter encryption library (https://github.com/hmrc/crypto/blob/master/src/main/scala/uk/gov/hmrc/crypto/ApplicationCrypto.scala#L31):

| Name                 | Description |
| -------------------- | ----------- |
| `returnUrl`          | The encrypted URL that the user will be redirected to at the end of any journeys resulting from this call, the URL will be passed to the redirectUserTo response |
| `returnLinkText`     | The text that will be used to display the return URL, the parameter will be passed to the redirectUserTo response |
| `termsAndConditions` | Optional terms and conditions - will default to "generic" is this is missing |
| `alreadyOptedInUrl`  | Optional If the user has already specified a preference and they have opted in for paperless then automatically redirect them to this URL | 
| `email`              | Optional customer's email address if already known |

Responds with:

| Status                        | Description |
| ----------------------------- | ----------- |
| 412 Precondition failed       | If the user needs to be redirected to a preferences-frontend page to set their paperless options |
| 409 Conflict                  | If the email provided is different than the one the user actually has. The user cannot be activated. |
| 200 Ok                        | If the user has previously accepted paperless |

When 200 is returned, the body of the response will contain details of the user's preference status (`optedIn` and `verifiedEmail`).  
This is specific to the type of terms and conditions that was passed in the body if present, otherwise for "generic":  

`optedIn: true` means that the user has signed up for paperless. It is `true` even if the email is not verified yet.  
`optedIn: false` means that the user has decided to not opt in for paperless.  
`verifiedEmail: true` [this attribute is present only when `optedIn: true`] means that when the user optedIn he also verified his email address.  
`verifiedEmail: false` [this attribute is present only when `optedIn: true`] means that the user has not verified the email he entered yet.  

```json
{
    "optedIn": true,
    "verifiedEmail": true
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

| Name                 | Description |
| -------------------- | ----------- |
| `returnUrl`          | The encrypted URL that the user will be redirected to at the end of any journeys starting from this partial |
| `returnLinkText`     | The text that will be used to display the return URL |
| `termsAndConditions` | Optional terms and conditions - will default to "generic" is this is missing |

### GET /paperless/warnings

Takes the following parameters:

| Name             | Description |
| ---------------- | ----------- |
| `returnUrl`      | The encrypted URL that the user will be redirected to at the end of any journeys starting from this partial |
| `returnLinkText` | The text that will be used to display the return URL |

This does not need terms and conditions to be specified because these warnings relating to email (pending email verification and email bounces) and 
email addresses are global to all of customer's preferences rather than to specific terms and conditions.

Run functional test
===================

`sbt it:test`
