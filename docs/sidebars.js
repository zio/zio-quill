const sidebars = {
  sidebar: [
    {
      type: "category",
      label: "ZIO Quill",
      collapsed: false,
      link: { type: "doc", id: "index" },
      items: [
        "getting-started",
        "writing-queries",
        "extending-quill",
        "contexts",
        "code-generation",
        "logging",
        "additional-resources",
        "quill-vs-cassandra",
        "quill-vs-slick",
        "changelog",
        "how-to-contribute",
      ]
    }
  ]
};

module.exports = sidebars;
