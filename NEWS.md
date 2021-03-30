## 1.3.0 IN PROGRESS
* Added `numberOfStudents` field to course records (MODCR-53)

## 1.2.0 2021-03-10
* Upgrade to RMB 32.2.0

## 1.1.2 2021-01-28
* Add ForeignKey entry for Coursetype into Courselistings table (MODCR-50)

## 1.1.1 2020-11-06
* Update to RMB v31.1.5, Vertx 3.9.4 (MODCR-46)

## 1.1.0 2020-10-05
* Update to RMB v31, JDK 11 (MODCR-43)

## 1.0.7 2020-09-15
* Allow tenant init even if load sample fails (MODCR-41)

## 1.0.6 2020-09-10
* Fix bug with tenant init parameters not being parsed (MODCR-42)

## 1.0.5 2020-06-03
* Fix bug with instructorObject cache not getting refreshed on PUT (MODCR-39)

## 1.0.4.2020-06-02
* Update to RMB 30.0.1 (MODCR-38)

## 1.0.3 2020-04-27
* Write changes in temporaryLoanTypeId back to item record on POST/PUT for reserves

## 1.0.2 2020-04-07
* Add missing module permissions for location and servicepoint lookup

## 1.0.1 2020-04-06

* Populate temporary location field from CourseListing (MODCR-17,MODCR-31)
* Properly pull call number from Item/Holdings records (MODCR-29,MODCR-30)
* Update RAML capitalization to work on MacOS (MODCR-2,  MODCR-25)
* Fix bug with deletion of a Reserve with no corresponding Item (MODCR-22)
* Fix parameters to loadSample on tenant init (MODCR-23, MODCR-35)
* Change tests to use shared HttpClient rather than new (MODCR-26)
* Fix population for electronicAccess fields with fallback (MODCR-28)
* Scrub fields marked as dynamic from POST/PUT (MODCR-34)
* Fix issue with field expansion for lists of Reserves (MODCR-36)


## 1.0.0 2020-03-13

* Initial release
