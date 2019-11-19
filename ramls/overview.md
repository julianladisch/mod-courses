<!--
$ indir .. ../folio-tools/generate-api-docs/generate_api_docs.py -r mod-courses -l info

By observation, the "Overview" heading generated from the `documentation.title` entry in `courses.raml` is rendered as a level-3 heading in our first docmentation generation and as a level-4 heading in our second. So the only way to safely have headings within this document nested beneath this is to start at level 4.
-->

The Course Reserves API is all about courses, and the items that have been reserved to them. (A reserved item is typically moved from its usual location to a special temporary location, along with other items reserved to the same course. From there, it can be picked up and checked out by students registered to the course in question.)

There are ten types implemented by this module. Of these, six
(`role`,
`term`,
`coursetype`,
`department`,
`processingstatus`
and
`copyrightstatus`)
are simple controlled vocabulaties used by the four more meaningful types. Of these six, `term` has a name, a start-date and and end-date; all the other five consist simple of a name and a description.

The four main types are more complex:

#### course

This represents a course, and has fields like `name`, `description`, `departmentId` (linking to a department within the controlled vocabulary of that name), `courseNumber` and `sectionName`.

In this type and in courselisting records (see below), some linked objects are expanded inline when a record is fetched: these are furnished in fields whose names end with `object`: for example, there is `departmentObject` field corresponding to `departmentId`, containing the `name` and `description` from the controlled vocabulary entry. 

However, some other fields that we might expect to see in a course record (e.g. `registrarId`, `termId`) are not present. This is because of the way we represent cross-listed courses. A courselisting record contains fields common to a set of cross-listed courses: each course belongs to exactly one courselisting and has a `courseListingId` field that specified this. Fetched records also have a corresponding `courseListingObject`, so that the Registrar ID of a course can be found in `courseListingObject.registrarId`.

#### courselisting

This represents the information common to a set of cross-listed courses, and has fields like `registrarId`, `externalId`, `termId` (and associated `termObject`) and `courseTypeId` (and associated `courseTypeObject`). It also has two further fields that link to objects defined in FOLIO's Inventory module:
`servicepointId` and `locationId`.

#### instructor

Instructors do not exist in isolation (unlike the six controlled-vocabulary objects) but only in the context of a specific courselisting: they are accessed at `/coursereserves/courselistings/{listing_id}/instructors`. Each instructor record contains an optional `userId` pointing into FOLIO's Users module, and data fields such as `name` and `barcode` which may be either copied from the User record (when it exists) or manually filled (when it does not).

#### reserve

Similarly, reserves exist in the context of specific courselisting: they are accessed at `/coursereserves/courselistings/{listing_id}/reserves`. Each reserve record contains a mandatory `itemId` pointing into FOLIO's Inventory module, and a `copiedItem` subrecord containing data fields such as `title` and `contributors` which are copied from the Item record to facilitate searching.

In addition to the fields pertaining to the reserved item, this record contains information about the reserve itself, including `processingStatusId` and `startDate` and `endDate`. These last two are inherited from the term associated with the courselisting that the reserve belongs to, but can be overridden.

