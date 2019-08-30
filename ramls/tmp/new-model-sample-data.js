const data = {
  terms: [
    {
      id: 14,
      name: "Summer 2019",
      startDate: "2019-04-03",
      endDate: "2019-07-31"
    },
    {
      id: 15,
      name: "Autumn 2019",
      startDate: "2019-09-01",
      endDate: "2019-12-18"
    }
  ],
  courseListings: [
    {
      id: 21,
      term: 15,
    },
    {
      id: 22,
      term: 14,
    }
  ],
  courses: [
    {
      name: "History 201",
      department: "History",
      courseListingId: 21
    },
    {
      name: "Anthropology 101",
      department: "Anthropology",
      courseListingId: 21
    },
    {
      name: "Calculus 201",
      department: "Mathematics",
      courseListingId: 22
    }
  ],
  reserves: [
    {
      itemId: 35,
      courseListingId: 21,
      copiedData: { title: "Tudor History" },
    },
    {
      itemId: 36,
      courseListingId: 21,
      copiedData: { title: "History of WWII" },
    },
    {
      itemId: 37,
      courseListingId: 22,
      copiedData: { title: "Integration for Dummies" },
    }
  ]
};

console.log(data);
