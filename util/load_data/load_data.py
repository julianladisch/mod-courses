import requests

def standard_headers = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}

def make_headers(token, tenant):
    headers = standard_headers.copy()
    headers["X-Okapi-Token"] = token
    headers["X-Okapi-Tenant"] = tenant
    return headers

def get_courselisting_by_external_id(okapi_url, tenant, token, external_id):
    pass

def post_courselisting(okapi_url, tenant, token, external_id):
    pass

def get_department_by_name(okapi_url, tenant, token, name):
    pass

def post_department(okapi_url, tenant, token, name):
    pass

def get_reserve_by_name(okapi_url, tenant, token, name):
    pass


class Loader:
    def __init__(self, okapi_url, tenant):
        self.okapi_url = okapi_url
        self.tenant = tenant
        self.token = None
        self.courselisting_dict = {}
        self.department_dict = {}
        self.instructor_dict = {}
        self.reserve_dict = {}
    
    def login(self, username, password):
        login_url = self.okapi_url + "/login"
        response = requests.post(login_url, headers=make_headers(None, self.tenant))
        if response.status_code is not 201:
            raise Exception("Unable to login: %s" % response.text)
        self.token = response.headers['x-okapi-token']

    def get_courselisting(self, external_id):
        pass

    def get_department(self, name):
        pass

    def get_instructor(self, listing_and_name):
        pass

    def get_reserve(self, item_id):
        pass

    def extract_course_parts(self, course):
        pass

    def get_item_list(self, item_string):
        pass




