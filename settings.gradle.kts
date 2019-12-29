plugins {
    `gradle-enterprise`
}

include(
        ":app:android-base", "app:server:functions",

        ":library:common",
        ":library:core", ":library:core-model", ":library:core-data", ":library:core-ui",
        ":library:shared", ":library:shared-scouting",

        ":feature:teams", ":feature:autoscout",
        ":feature:scouts", ":feature:templates",
        ":feature:exports",
        ":feature:trash", ":feature:settings"
)
