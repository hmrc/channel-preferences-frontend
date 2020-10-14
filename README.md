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

### GET /paperless/status

Takes the following parameters, which should all be encrypted and encoded using the query parameter encryption library (https://github.com/hmrc/crypto/blob/master/src/main/scala/uk/gov/hmrc/crypto/ApplicationCrypto.scala#L31)

| Name             | Description |
| ---------------- | ----------- |
| `returnUrl`      | The encrypted URL that the user will be redirected to at the end of any journeys starting from this partial |
| `returnLinkText` | The text that will be used to display the return URL |


Responds with:

| Status                        | Description |
| ----------------------------- | ----------- |
| 200 Ok                        | With a paperless preference status |

When calling this endpoint with the correct parameters, you will always recieve a paperless status even if a customer has no preference status set.


JSON for customer who is a paperless customers.
```json
{"status":{"name":"ALRIGHT","category":"INFO","text":"You chose to get your Self Assessment tax letters online"},"url":{"link":"/paperless/check-settings?returnUrl=estmi4JljKudAQ%2BTRvoPhRkCZT02m5YaCTH3xBjDQAg%3D&returnLinkText=7xlv71LPbfS4KiKJ12w2jA%3D%3D","text":"Check your settings"}}
```
JSON for customer who has no preference.
```json
{"status":{"name":"NEW_CUSTOMER","category":"ACTION_REQUIRED","text":"Get your Self Assessment tax letters online"},"url":{"link":"/paperless/check-settings?returnUrl=estmi4JljKudAQ%2BTRvoPhRkCZT02m5YaCTH3xBjDQAg%3D&returnLinkText=7xlv71LPbfS4KiKJ12w2jA%3D%3D","text":"Get tax letters online"}}
```
JSON for customer who needs to verify there email address.
```json
{"status":{"name":"EMAIL_NOT_VERIFIED","category":"ACTION_REQUIRED","text":"You need to verify your email address before you can get Self Assessment tax letters online"},"url":{"link":"/paperless/check-settings?returnUrl=estmi4JljKudAQ%2BTRvoPhRkCZT02m5YaCTH3xBjDQAg%3D&returnLinkText=7xlv71LPbfS4KiKJ12w2jA%3D%3D","text":"Verify your email address"}}
```
JSON for customer who has opted out of paperless.
```json
{"status":{"name":"PAPER","category":"ACTION_REQUIRED","text":"You have not yet chosen to get your Self Assessment tax letters online"},"url":{"link":"/paperless/check-settings?returnUrl=estmi4JljKudAQ%2BTRvoPhRkCZT02m5YaCTH3xBjDQAg%3D&returnLinkText=7xlv71LPbfS4KiKJ12w2jA%3D%3D","text":"Get tax letters online"}}
```
Run functional test
===================

`sbt it:test`

Opt-in out page versioning
===================

# Terms

- `OptInCohort`  an object that helps to associate a number to an opt in page. Currently there are 2 cohorts IPage8 and TCPage9
- `OptInPage`   a twirl tempalate to generate an opt in form. (i_page8.scala.html, tc_page9.scala.html)
- `PageType` a type of the opt in page. (PageType.IPage or PageType.TCPage)
- `majorVersion`  a version number that is used to trigger re-opt-in process.
- `minorVersion`  a version number that doesn't trigger re-opt-in.


# How to add a new vesion of opt in page

 Choose next availabel cohort number in controllers.internal.OptInCohort and create a new `OptInCohort`.

``` scala
object IPage10 extends OptInCohort {
  override val id: Int = 10
  override val name: String = "IPage10"
  override val terms: TermsType = // required termtype
  override val pageType: PageType = // required PageType
  override val majorVersion: Int = // required version
}
```

 Add the new `OptInCohort` controllers.internal.OptInCohortConfigurationValues

 ``` scala

object OptInCohortConfigurationValues extends ConfiguredCohortValues[OptInCohort] {
  val availableValues = List(IPage8, TCPage9, IPage10 )
}

 ```

Enable a new cohort in application.conf

``` json
abTesting.cohort.IPage10.enabled = true
```

Create a cohort test. As an example please see: controllers.internal.ReadOnlyIPage8CohortSpec. Never update existing tests.
All existing `OptInCohort` are read-only.


Create a new twirl template. views.sa.prefs.cohorts.i_page10.scala.html

Create a test for all text fields (English and Welsh) in the new template. Never update existing tests. All existing opt in pages are read-only.
As an example use ReadOnlyIPage8Spec.

Add a new template to
views.sa.prefs.sa_printing_preference.scala.hmtl

``` html
@this(
  iPage8: views.html.sa.prefs.cohorts.i_page8,
  tcPage9: views.html.sa.prefs.cohorts.tc_page9,
  iPage10: views.html.sa.prefs.cohorts.tc_page10
)
@(emailForm: Form[_], submitPrefsFormAction: Call, cohort: controllers.internal.OptInCohort)(implicit request: AuthenticatedRequest[_], messages: Messages)

@import views.html.sa.prefs.cohorts._
    @import controllers.internal._

@{cohort match {
        case IPage8 => iPage8(emailForm, submitPrefsFormAction)
        case TCPage9 => tcPage9(emailForm, submitPrefsFormAction)
        case TCPage10 => iPage10(emailForm, submitPrefsFormAction)
    }
}

```


# How change the text of the opt-in page.

   All messages for IPage type should be located in `ipage.messages` and `ipage.messages.cy`, not in `message` and `message.cy`

   All messages for TCPage type should be located in `tcpage.messages` and `tcpage.messages.cy`, not in `message` and `message.cy`

   These files should be append only. A new page should have a new set of page related messages appended to the end of the file.



# How to change the current version of Opt In Page

Change a cohort in CohortCurrent in controllers.internal.OptInCohort

``` scala

object CohortCurrent {
  val ipage = IPage10,
  val tcpage = TCPage9
}

```

# How to enable/disable the re-optin declined survey

Set the `reOptInPage10SurveyEnabled` config property to true to enable the survey (default is disabled)

``` json
survey.ReOptInPage10.enabled = true
```

# How to amend/add a new question in the re-optin survey

Amend both `survey.messages` and `survey.messages.cy` ensuring to add a new key for the multiple choice answers *NOTE: DO NOT AMEND AN EXISTING KEY VALUE, INSTEAD, ADD A NEW KEY AS IN THE EXAMPLE BELOW*

```
paperless.survey.reoptin_declined.choice.3=I enjoy coding
```

Instead of amending `choice.3`, simply add `choice.6` for instance as such:

```
paperless.survey.reoptin_declined.choice.6=I dislike coding
```

You will also need to amend `views.sa.prefs.surveys.reoptin_declined_survey.scala.html` and amend the Randomized list:

```
               items = Random.shuffle(List(1,2,3,4,5)).map(num =>
```

to:

```
                items = Random.shuffle(List(1,2,6,4,5)).map(num =>
```

You will also need to amend the model file `controllers.internal.SurveyDetailsForm` and the object/case class from:

```scala
object SurveyReOptInDeclinedDetailsForm {

  val reasonMaxLength = 500

  def apply(): Form[Data] =
    Form(
      mapping(
        "choice1" -> optional(boolean),
        "choice2" -> optional(boolean),
        "choice3" -> optional(boolean),
        "choice4" -> optional(boolean),
        "choice5" -> optional(boolean),
        "reason"  -> optional(text(maxLength = reasonMaxLength))
      )(Data.apply)(Data.unapply)
    )

  case class Data(
    choice1: Option[Boolean],
    choice2: Option[Boolean],
    choice3: Option[Boolean],
    choice4: Option[Boolean],
    choice5: Option[Boolean],
    reason: Option[String]
  )
}
```

to:

```scala
object SurveyReOptInDeclinedDetailsForm {

  val reasonMaxLength = 500

  def apply(): Form[Data] =
    Form(
      mapping(
        "choice1" -> optional(boolean),
        "choice2" -> optional(boolean),
        "choice6" -> optional(boolean),
        "choice4" -> optional(boolean),
        "choice5" -> optional(boolean),
        "reason"  -> optional(text(maxLength = reasonMaxLength))
      )(Data.apply)(Data.unapply)
    )

  case class Data(
    choice1: Option[Boolean],
    choice2: Option[Boolean],
    choice6: Option[Boolean],
    choice4: Option[Boolean],
    choice5: Option[Boolean],
    reason: Option[String]
  )
}
```

As a result tests will fail and they will need to be fixed, including amending the `controllers.internal.SurveyController` methods `displayReOptInDeclinedSurveyForm` and method `auditSurvey`.

Unless the question means semantically the same thing as the original question but with different grammar, a new key should be selected for a different answer to be presented to the user or it will affect the results during querying.

# How to get a rendered opt-in page by a cohort number

GET /paperless/opt-in-cohort/display/:cohort

# How to get a list of all cohorts

GET /paperless/opt-in-cohort/list

