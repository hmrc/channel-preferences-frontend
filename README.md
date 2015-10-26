preferences-frontend
====================

UI for managing print suppression email addresses & preferences.

For setting and changing preferences, whole pages are returned. Partial views are provided for integration into other frontends (such as business-tax-account).

# Public API 

| Path                                                                | Supported Methods | Description
| ------------------------------------------------------------------- | ----------------- | -------------
| `/account/account-details/sa/update-email-address`                  | GET, POST         | Displays/submits a form for changing a customer's email address    
| `/account/account-details/sa/update-email-address-thank-you`        | GET               | Displays confirmation that a customer's email address has been changed   
| `/account/account-details/sa/opt-out-email-reminders`               | GET, POST         | Displays/submits a form for switching a customer to paper    
| `/account/account-details/sa/opt-out-email-reminders-thank-you`     | GET               | Displays confirmation that a customer has switched to paper    
| `/paperless/resend-validation-email`                                | POST              | Sends a new verification link to a customer, and then displays a confirmation page [More...](#post-paperlessresent-validation-email)   
| ~~`/account/account-details/sa/resend-validation-email`~~           | POST              | Deprecated version of `/paperless/resend-validation-email`    
| `/account/account-details/sa/login-opt-in-email-reminders`          | GET, POST         | Redirects to or submits a form for switching a customer to paperless    
| `/account/account-details/sa/login-opt-in-email-reminders/:cohort`  | GET               | Displays a form for switching a customer to paperless    
| `/account/account-details/sa/opt-in-email-reminders`                | GET, POST         | Redirects to or submits a form for switching a customer to paperless    
| `/account/account-details/sa/opt-in-email-reminders/:cohort`        | GET               | Displays a form for switching a customer to paperless        
| `/account/account-details/sa/opt-in-email-reminders-thank-you`      | GET               | Confirms that a user has submitted their preference for paper or paperless    
| `/account/account-details/sa/upgrade-email-reminders`               | GET, POST         | Displays/submits a form for upgrading a customer to the latest terms and conditions for paperless
| `/account/account-details/sa/upgrade-email-reminders-thank-you`     | GET               | Confirms that a user has submitted their acceptance or refusal of the latest terms and conditions for paperless
| `/sa/print-preferences/:token`                                      | GET               | Redirects a user back to `return_url` with their current email address, if known    
| `/sa/print-preferences/verification/:token`                         | GET               | Submits the verification of a user's email address
| `/sa/print-preferences/assets/*file`                                | GET               | Serves up the test gif that is used by the portal to determine if the service is up    
| `/paperless/manage`                                                 | GET               | Returns an HTML partial with appropriate information and links for a customer to manage their paperless status [More...](#get-paperlessmanage)
| `/paperless/warnings`                                               | GET               | Returns an HTML partial which may contain warnings and links if a customer's paperless status is non-verified [More...](#get-paperlesswarnings)
| ~~`/account/account-details/sa/email-reminders-status`~~            | GET               | Deprecated version of `/paperless/manage`
| ~~`/account/preferences/warnings`~~                                 | GET               | Deprecated version of `/paperless/warnings`


### POST /paperless/resend-validation-email


### GET /paperless/manage

Takes the following parameters:

| Name             | Description |
| ---------------- | ----------- |
| `returnUrl`      | The URL that the user will be redirected to at the end of any journeys starting from this partial |
| `returnLinkText` | The text that will be used to display the return URL |

### GET /paperless/warnings