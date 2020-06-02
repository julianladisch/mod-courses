## 1.0.4.2020-06-02
* Update to RMB 30.0.1 (MODCR-38)
* Fix bug with instructorObject cache not getting refreshed on PUT (MODCR-39)

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
