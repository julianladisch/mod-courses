# Data model for `mod-courses`

[The `ramls` directory](ramls) contains the machine-readable specification of the WSAPI (the `*.raml` files) and the associated schemas (the `*.json` files). This file describes how the various kinds of object fit together, starting with the highest level concepts and working down to the lowest.


## Objects

* `course.json`
--
a course in the abstract, rather than a specific scheduled instance of the course. Contains fields like name ("Calculus 101"), description ("Differentiation, integration and approximation") and the department that offers the course ("Mathematics").
[[Example]](ramls/examples/course.json)

* `section.json`
--
a specific, scheduled manifestation of a course. It is taught by particular instructors (see `instructor.json`) in particular roles (see `role.json`) during a specific academic period (see `schedule.json`). In general, each course will have multiple sections. In many institutions, a single course will even have multiple sections during the same academic period, taught either by the same or different instructors. The data model aims to encompass all these different institutional practises.
[[Example]](ramls/examples/section.json)

* `instructor.json`
--
a specific person _in a specific role in a particular section_ -- not an instructor in a more general, reusable sense. This record contains an optional `userId` pointing into the users module, but may instead simply state the name of the instructor, for those cases where the instructor does not have an account in FOLIO (e.g. a guest instructor).
[[Example]](ramls/examples/instructor.json)

* `reserve.json`
--
an item reserved to a section of a course. Consists primarily of a `sectionId` and an `itemId`. Also contains a redundant copy of information about the item (or the instance that it is an instance of), so that it is possible to search reserves based on properties of reserved items.
[[Example]](ramls/examples/reserve.json)

* `role.json`
--
an entry in the very simple controlled vocabulary of roles that instructors can take (e.g. "principal instructor", "teaching assisant", "lab support").
[[Example]](ramls/examples/role.json)

* `schedule.json`
--
a schedulable period in which a course sections takes place. Consists of a display name together with start and end dates.
[[Example]](ramls/examples/schedule.json)

Roles and schedules are each managed separately, as entries in those vocabularies can be used in multiple places. By contrast, sections, instructors and reserves are each managed as part of the course API, since they exist and make sense only in the context of that hierarchy of objects: a section is a section of a course; an instructor is an instructor for a section; a reserve is reserved to a section. There are therefore three kinds of top-level object: course, role and schedule.


## Lists

The files
[`courses.json`](ramls/courses.json),
[`sections.json`](ramls/sections.json),
[`instructors.json`](ramls/instructors.json),
[`reserves.json`](ramls/reserves.json),
[`roles.json`](ramls/roles.json),
and
[`schedules.json`](ramls/schedules.json)
each represent simple lists of the relevant kind of object. Each contains a `totalRecords` field, which specifies how many objects exist that match the pertaining query, and a field named `courses`, `sections` etc. that is an array of the actual objects.


## APIs

APIs is defined in three RAMLs:
[`courses.raml`](ramls/courses.raml),
which is by far the most important; and two supporting controlled vocabularies in
[`roles.raml`](ramls/roles.raml)
and
[`schedules.raml`](ramls/schedules.raml). The following is a very high-level summary:

* `/course-reserve-storage`
  * `courses` (GET, POST, DELETE)
    * _ID_ (GET, PUT, DELETE)
      * `sections` (GET, POST, DELETE)
        * _ID_ (GET, PUT, DELETE)
	  * `instructors` (GET, POST, DELETE)
	    * _ID_ (GET, PUT, DELETE)
	  * `reserves` (GET, POST, DELETE)
	    * _ID_ (GET, PUT, DELETE)
  * `roles` (GET, POST, DELETE)
    * _ID_ (GET, PUT, DELETE)
  * `schedules` (GET, POST, DELETE)
    * _ID_ (GET, PUT, DELETE)
