private const val FIRESTORE_USERS = "users"
private const val FIRESTORE_TEAMS = "teams"
private const val FIRESTORE_TEMPLATES = "templates"

val users: dynamic
    get() = modules.firestore.collection(FIRESTORE_USERS)
val teams: dynamic
    get() = modules.firestore.collection(FIRESTORE_TEAMS)
val templates: dynamic
    get() = modules.firestore.collection(FIRESTORE_TEMPLATES)

class Modules(
        val functions: dynamic,
        val admin: dynamic,
        val firestore: dynamic,
        val moment: dynamic
)


