import re
import csv
import requests

standard_headers = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}

def make_headers(token, tenant):
    headers = standard_headers.copy()
    headers["X-Okapi-Token"] = token
    headers["X-Okapi-Tenant"] = tenant
    return headers

def get_courselisting_by_external_id(okapi_url, tenant, token, external_id):
    courselisting_url = "%s/%s?query=externalId=%s" % (okapi_url, "coursereserves/courselistings",
            external_id)
    response = requests.get(courselisting_url, headers=make_headers(token, tenant))
    if response.status_code is not 200:
        raise LoaderException("Unable to get courselisting with externalId %s, %s" %
                (external_id, response.text))
    if len(response.json()["courseListings"]) < 1:
            return None
    return response.json()["courseListings"][0]["id"]

def post_courselisting(okapi_url, tenant, token, external_id, term_id):
    courselisting_url = "%s/%s" % (okapi_url, "coursereserves/courselistings")
    payload = { "externalId" : external_id, "termId" : term_id }
    response = requests.post(courselisting_url, headers=make_headers(token, tenant),
            json=payload)
    if response.status_code is not 201:
        raise LoaderException("Unable to create courselisting with payload %s: %s" %
                (payload, response.text))
    return response.json()["id"]

def get_department_by_name(okapi_url, tenant, token, name):
    department_url = "%s/%s?query=name=%s" % (okapi_url, "coursereserves/departments", name)
    response = requests.get(department_url, headers=make_headers(token, tenant))
    if response.status_code is not 200:
        raise LoaderException("Unable to get department with name %s, %s" %
                ( name, response.text ))
    if len(response.json()["departments"]) < 1:
        return None
    return response.json()["departments"][0]["id"]

def post_department(okapi_url, tenant, token, name):
    department_url = "%s/%s" % (okapi_url, "coursereserves/departments")
    payload = { "name" : name }
    response = requests.post(department_url, headers=make_headers(token, tenant),
            json=payload)
    if response.status_code is not 201:
        raise LoaderException("Unable to add department with payload %s: %s" %
                (payload, response.text))
    return response.json()["id"]
    
def get_instructor_by_courselisting_and_name(okapi_url, tenant,
        token, courselisting_id, name):
    instructor_url = "%s/%s/%s/instructors?query=name=%s" % (okapi_url,\
            "coursereserves/courselistings", courselisting_id, name)
    response = requests.get(instructor_url, headers=make_headers(token, tenant))
    if response.status_code is not 200:
        raise LoaderException("Unable to get instructor from url %s with name %s, %s" %
                ( instructor_url, name, response.text ))
    if len(response.json()["instructors"]) < 1:
        return None
    return response.json()["instructors"][0]["id"]

def post_instructor(okapi_url, tenant, token, courselisting_id, name):
    instructor_url = "%s/%s/%s/instructors" %\
            (okapi_url, "coursereserves/courselistings", courselisting_id)
    payload = { "name" : name, "courseListingId": courselisting_id }
    response = requests.post(instructor_url, headers=make_headers(token, tenant),
            json=payload)
    if response.status_code is not 201:
        raise LoaderException("Unable to add instructor to url %s with payload %s: %s" %
                (instructor_url, payload, response.text))
    return response.json()["id"]

def get_reserve_by_item_id(okapi_url, tenant, token, item_id):
    reserve_url = "%s/%s?query=itemId=%s" % (okapi_url, "coursereserves/reserves", item_id)
    response = requests.get(reserve_url, headers=make_headers(token, tenant))
    if response.status_code is not 200:
        raise LoaderException("Unable to get reserves with url %s, %s" %\
                (reserve_url, response.text))
    if len(response.json()["reserves"]) < 1:
        return None

    return response.json()["reserves"][0]["id"]

def post_reserve(okapi_url, tenant, token, courselisting_id, item_id):
    reserve_url = "%s/%s" % (okapi_url, "coursereserves/reserves")
    payload = { "courseListingId" : courselisting_id, "itemId" : item_id }
    response = requests.post(reserve_url, headers=make_headers(token, tenant),
            json=payload)
    if response.status_code is not 201:
        raise LoaderException("Unable to add reserve to url %s with payload %s: %s" %
                (reserve_url, payload, response.text))
    return response.json()["id"]

def post_department(okapi_url, tenant, token, name):
    department_url = "%s/%s" % (okapi_url, "coursereserves/departments")
    payload = { "name" : name }
    response = requests.post(department_url, headers=make_headers(token, tenant),
            json=payload)
    if response.status_code is not 201:
        raise LoaderException("Unable to add department to url %s with payload %s: %s" %
                (department_url, payload, response.text))
    return response.json()["id"]

def get_department_by_name(okapi_url, tenant, token, name):
    department_url = "%s/%s?query=name=%s" % (okapi_url, "coursereserves/departments", name)
    response = requests.get(department_url, headers=make_headers(token, tenant))
    if response.status_code is not 200:
        raise LoaderException("Unable to get department with url %s, %s" %\
                (department_url, response.text))
    if len(response.json()["departments"]) < 1:
        return None

    return response.json()["departments"][0]["id"]

def lookup_user_by_first_last_location(okapi_url, tenant, token, first_name,\
        last_name, location):
    user_url = "%s/%s?query=personal.lastName=%s+AND+personal.firstName=%s" % (okapi_url, "users",\
            last_name, first_name)
    response = requests.get(user_url, headers=make_headers(token, tenant))
    if response.status_code is not 200:
        raise LoaderException("Unable to get users with url %s, %s" %\
                (user_url, response.text))
    if len(response.json()["users"]) < 1:
        #print("No users returned for URL %s" % user_url)
        return None

    users = response.json()["users"]
    for user in users:
        user_location = None
        try:
            user_location = user["personal"]["addresses"][0]["city"]
        except Exception as e:
            print("Unable to get location: %s" % e)
        if user_location and user_location.upper() == location.upper():
            return user["id"]
        else:
            print("%s and %s don't match for locations" % (location, user_location))
    return None

def lookup_item_by_former_id(okapi_url, tenant, token, former_id):
    item_url = "%s/%s?query=formerIds=%s" % (okapi_url, "item-storage/items", former_id)
    response = requests.get(item_url, headers=make_headers(token, tenant))
    if response.status_code is not 200:
        raise LoaderException("Unable to get items with url %s, %s" %\
                (item_url, response.text))
    if len(response.json()["items"]) < 1:
        print("No items returned for url %s" % item_url)
        return None

    return response.json()["items"][0]["id"]

def post_course(okapi_url, tenant, token, courselisting_id, department_id, name, description):
    course_url = "%s/%s" % (okapi_url, "coursereserves/courses")
    payload = { "courseListingId": courselisting_id, "name" : name,\
            "description" : description, "departmentId": department_id }
    response = requests.post(course_url, headers=make_headers(token, tenant),
            json=payload)
    if response.status_code is not 201:
        raise LoaderException("Unable to add course to url %s with payload %s: %s" %
                (course_url, payload, response.text))

    return response.json()["id"]

def get_course_by_name(okapi_url, tenant, token, name):
    course_url = "%s/%s?query=name=%s" % (okapi_url, "coursereserves/courses", name)
    response = requests.get(course_url, headers=make_headers(token, tenant))
    if response.status_code is not 200:
        raise LoaderException("Unable to get course with url %s, %s" %\
                (course_url, response.text))
    if len(response.json()["courses"]) < 1:
        return None

    return response.json()["courses"][0]["id"]

def post_term(okapi_url, tenant, token, name, start_date, end_date):
    term_url = "%s/%s" % (okapi_url, "coursereserves/terms")
    payload = { "name" : name, "startDate" : start_date, "endDate" : end_date }
    response = requests.post(term_url, headers=make_headers(token, tenant),
            json=payload)
    if response.status_code is not 201:
        raise LoadException("Unable to add course to url %s with payload %s: %s" %
                (term_url, payload, response.text))
    return response.json()["id"]

def get_term_by_name(okapi_url, tenant, token, name):
    term_url = "%s/%s?query=name=%s" % ( okapi_url, "coursereserves/terms", name)
    response = requests.get(term_url, headers=make_headers(token, tenant))
    if response.status_code is not 200:
        raise LoaderException("Unable to get term with url %s: %s" %\
                (term_url, response.text))
    if len(response.json()["terms"]) < 1:
        return None

    return response.json()["terms"][0]["id"]
    

class LoaderException(Exception):
    pass

class Loader:
    def __init__(self, okapi_url, tenant, term_name):
        self.okapi_url = okapi_url
        self.tenant = tenant
        self.term_name = term_name
        self.token = None
        self.courselisting_dict = {}
        self.department_dict = {}
        self.instructor_dict = {}
        self.reserve_dict = {}
        self.term_dict = {}
    
    def login(self, username, password):
        login_url = self.okapi_url + "/authn/login"
        payload = { "username" : username, "password" : password }
        response = requests.post(login_url, headers=make_headers(None, self.tenant),
                json=payload)
        if response.status_code is not 201:
            raise LoaderException("Unable to login: %s" % response.text)
        self.token = response.headers['x-okapi-token']

    def get_term(self, term_name):
        if term_name in self.term_dict:
            return self.term_dict[term_name]
        term_id = get_term_by_name(self.okapi_url, self.tenant, self.token, term_name)
        if term_id is None:
            start_date = '2020-01-09T00:00:00+0000'
            end_date = '2020-09-15T00:00:00+0000'
            term_id = post_term(self.okapi_url, self.tenant, self.token, term_name,
                    start_date, end_date)
            self.term_dict[term_name] = term_id
        return term_id

    def get_courselisting(self, external_id):
        if external_id in courselisting_dict:
            return courselisting_dict[external_id]
        courselisting = get_courselisting_by_external_id(self.okapi_url, self.tenant,
                self.token, external_id)
        return courselisting

    def get_department(self, name):
        if name in self.department_dict:
            return self.department_dict[name]
        department_id = get_department_by_name(self.okapi_url, self.tenant, self.token, name)
        if department_id is None:
            department_id = post_department(self.okapi_url, self.tenant, self.token,
                    name)
            self.department_dict[name] = department_id
        return department_id

    def get_instructor(self, courselisting_id, name):
        key = "%s_%s" % (courselisting_id, name)
        if key in self.instructor_dict:
            return self.instructor_dict[key]
        instructor_id = get_instructor_by_courselisting_and_name(self.okapi_url,
                self.tenant, self.token, courselisting_id, name)
        if instructor_id is None:
            instructor_id = post_instructor(self.okapi_url, self.tenant,
                    self.token, courselisting_id, name)
            self.instructor_dict[key] = instructor_id
        return instructor_id

    def get_reserve(self, item_id):
        pass

    def extract_course_parts(self, course):
        pass

    def get_item_list(self, item_string):
        pass

    def sanitize_instructor_name(self, name):
        pattern = r"(.+),\s?(.+?)\s*\((.+)\)"
        res = re.match(pattern, name)
        if not res:
            raise LoaderException("Unable to match input '%s' with pattern '%s'" %\
                    (name, pattern))
        return (res.group(1), res.group(2), res.group(3))

    def sanitize_course_name(self, name):
        pattern = r"([a-zA-Z]+)\s+(\d+[a-zA-Z]*?)\s+-\s+(.+)"
        res = re.match(pattern, name)
        if not res:
            raise LoaderException("Unable to match input '%s' with pattern '%s'" %\
                    (name, pattern))
        return (res.group(1), res.group(2), res.group(3))

    def read_lines_from_file(self, handle):
        reader = csv.reader(handle, delimiter='|')
        term_id = self.get_term(self.term_name)
        next(reader) #skip header
        for row in reader:
            try:
                print("Create courselisting with external id '%s'" % row[0])
                courselisting_id = post_courselisting(self.okapi_url, self.tenant,\
                        self.token, row[0], term_id)
                course_parts = self.sanitize_course_name(row[1])
                print("Find or create department with name '%s'" % course_parts[0])
                department_id = self.get_department(course_parts[0])
                course_name = "%s %s" % ( course_parts[0], course_parts[1] )
                course_description = course_parts[2]
                print("Create course belonging to courselisting '%s' with name '%s'" %\
                        (row[0], course_name))
                course_id = post_course(self.okapi_url, self.tenant, self.token,
                        courselisting_id, department_id, course_name, course_description)
                instructor_parts = self.sanitize_instructor_name(row[2])
                print("Find or create instructor with courselisting '%s' and name '%s'" %\
                        (row[0], instructor_parts[0]))
                instructor_id = self.get_instructor(courselisting_id, instructor_parts[0])
                item_id_list = row[3].split('%')
                for n in range(len(item_id_list)):
                    item_id_list[n] = item_id_list[n].strip('"')
                print("Create reserves for the following items: %s" % item_id_list)
                for item in item_id_list:
                    print("Creating reserve for item %s" % item)
                    post_reserve(self.okapi_url, self.tenant, self.token, courselisting_id,
                            item)
            except Exception as e:
                #print("Unable to process row %s: %s" % (row, e))
                raise e

    def test_lookups_from_file(self, handle):
        reader = csv.reader(handle, delimiter='|')
        term_id = self.get_term(self.term_name)
        next(reader)
        for row in reader:
            try:
                instructor_parts = self.sanitize_instructor_name(row[2])
                user_id = lookup_user_by_first_last_location(self.okapi_url,\
                        self.tenant, self.token, instructor_parts[1],\
                        instructor_parts[0], instructor_parts[2])
                print("Returned user_id for %s, %s (%s) is %s" %\
                        (instructor_parts[0], instructor_parts[1], instructor_parts[2], user_id))
                item_id_list = [ a.strip('"') for a in row[3].split('%') if a ]
                for item in item_id_list:
                    former_id = item.split("::a",1)[0]
                    item_id = lookup_item_by_former_id(self.okapi_url, self.tenant,\
                            self.token, former_id)
                    print("Got id %s for former id %s" % ( item_id, former_id))
            except Exception as e:
                #raise e
                print("Unable to process row %s: %s" % (row, e))







