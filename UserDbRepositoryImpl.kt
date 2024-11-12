package theweeb.dev.practicingcompose.data.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import theweeb.dev.practicingcompose.domain.model.Department
import theweeb.dev.practicingcompose.domain.model.DepartmentTheme
import theweeb.dev.practicingcompose.domain.model.QuizResult
import theweeb.dev.practicingcompose.domain.model.User
import theweeb.dev.practicingcompose.domain.model.getDepartmentTheme
import theweeb.dev.practicingcompose.domain.model.mapToFirestore
import theweeb.dev.practicingcompose.domain.model.toUser
import theweeb.dev.practicingcompose.domain.repository.UserDbRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class UserDbRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : UserDbRepository {

    private val usersRef = db.collection("users")
    private val ccsRef = usersRef
        .whereEqualTo("department", "CCS")
        .limit(50)
    private val cocRef = usersRef
        .whereEqualTo("department", "COC")
        .limit(50)
    private val cteasRef = usersRef
        .whereEqualTo("department", "CTEAS")
        .limit(50)
    private val cbeRef = usersRef
        .whereEqualTo("department", "CBE")
        .limit(50)

    override suspend fun addLogData(
        uid: String?,
        username: String?,
        didUserLogIn: Boolean
    ) {
        db.collection("logs").document().set(
            hashMapOf(
                "uid" to uid,
                "username" to username,
                "time" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE HH:mm")),
                "didUserLogIn" to didUserLogIn,
                "createdAt" to Timestamp.now()
            )
        )
    }

    override fun addUserToDb(user: User?) {
        if(user != null){
            val check = user.uid.let { usersRef.document(it) }
            check.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document.exists()) {
                        Log.d("TAG", "User already exists.")
                    } else {
                        user.uid.let {
                            usersRef.document(it).set(user.mapToFirestore())
                        }
                    }
                } else {
                    Log.d("TAG", "Error: ", task.exception)
                }
            }
        }
    }

    override suspend fun updateUserCompletionStatus(hasCompleted: Boolean) {
        val userRef = auth.currentUser?.uid?.let { usersRef.document(it) }

        try {
            userRef?.update("hasCompleted", hasCompleted)?.await()
            println("User's completion status updated successfully.")
        } catch (e: Exception) {
            println("Error updating completion status: ${e.message}")
        }
    }



    override suspend fun updateSectionProgress(sectionId: String, progressFloat: Float) {
        try {
            // Ensure the user is authenticated before proceeding
            val currentUser = auth.currentUser ?: throw IllegalStateException("User is not authenticated")

            // Reference to the user's document in Firestore
            val userDocRef = db.collection("users").document(currentUser.uid)

            // Using dot notation to update a specific section's progress in sectionsProgress map
            val progressField = "sectionsProgress.$sectionId"

            // Update the user's sectionsProgress map (converting Float to Double for Firestore)
            userDocRef.update(progressField, progressFloat.toDouble()).await()

        } catch (e: FirebaseFirestoreException) {
            // Handle Firestore-specific errors (such as document not existing)
            when (e.code) {
                FirebaseFirestoreException.Code.NOT_FOUND -> {
                    // Handle the case where the document does not exist
                    // Optionally, create the document here if necessary
                    Log.e("FirestoreError", "User document not found: ${e.message}")
                }
                else -> {
                    // Handle other Firestore exceptions
                    Log.e("FirestoreError", "Error updating section progress: ${e.message}")
                }
            }
        } catch (e: Exception) {
            // Catch any other general exceptions (network issues, etc.)
            e.printStackTrace()
            Log.e("Error", "Failed to update section progress: ${e.message}")
        }
    }

    override suspend fun clearSectionChaptersQuizzesResult(sectionId: String) {
        // Reference to the user document in Firestore
        val userDocRef = auth.currentUser?.uid?.let {
            usersRef.document(it)
        }

        userDocRef?.get()?.await()?.toUser()?.let { user ->
            // Create a mutable copy of the user's current `subSectionQuizzes`
            val updatedResults = user.subSectionQuizzes.toMutableMap().apply {
                // Set the specified sectionId's value to an empty map to clear its contents
                this[sectionId] = emptyMap()
            }

            // Update Firestore with the modified map for `subSectionQuizzes`
            userDocRef.set(mapOf("subSectionQuizzes" to updatedResults), SetOptions.merge())
                .addOnSuccessListener {
                    // Handle success if needed
                    Log.d("UserDbRepository", "Section $sectionId records successfully cleared.")
                }
                .addOnFailureListener { e ->
                    // Handle failure
                    Log.e("UserDbRepository", "Error clearing section $sectionId records", e)
                }
        }
    }

    override suspend fun updateSectionChaptersQuizzesResult(
        sectionId: String,
        chapterResults: Map<String, Boolean>
    ) {

        val userDocRef = auth.currentUser?.uid?.let {
            usersRef.document(it)
        }

        userDocRef?.get()?.await()?.toUser()?.let { user ->
            val updatedResults = user.subSectionQuizzes.toMutableMap().apply {
                this[sectionId] = chapterResults
            }

            userDocRef.set(mapOf("subSectionQuizzes" to updatedResults), SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("UserDbRepository", "Document successfully updated or inserted.")
                }
                .addOnFailureListener { e ->
                    Log.e("UserDbRepository", "Error updating or inserting document", e)
                }
        }
    }

    override suspend fun updateCreationDate() {
        auth.currentUser?.uid?.let {
            usersRef.document(it).update(
                "createdAt",
                FieldValue.serverTimestamp()
            )
        }
    }

    override fun getCocUsers() = cocRef.snapshots().map { query ->
        query.documents.map { it.toUser() }
    }

    override fun getCteasUsers() = cteasRef.snapshots().map { query ->
        query.documents.map { it.toUser() }
    }

    override fun getCbeUsers() = cbeRef.snapshots().map { query ->
        query.documents.map {
            val user = it.toUser()
            Log.d("CBEUSERs", "$user")
            user
        }
    }

    override fun getCcsUsers() = ccsRef.snapshots().map { query ->
        val users = query.documents.map {
            it.toUser()
        }
        for (dc in query.documentChanges) {
            when (dc.type) {
                DocumentChange.Type.ADDED -> Log.d(TAG, "New user: ${dc.document.data}")
                DocumentChange.Type.MODIFIED -> Log.d(TAG, "Modified user: ${dc.document.data}")
                DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed user: ${dc.document.data}")
            }
        }
        users
    }

    override fun getCurrentUser(): Flow<User> {
        val currentUserId = auth.currentUser?.uid
        return if (currentUserId.isNullOrEmpty()) {
            emptyFlow()
        } else {
            usersRef.document(currentUserId).snapshots().map {
                it.toUser()
            }
        }
    }

    override suspend fun getUsers(department: Department?): List<User> {
        return try {
            if (department != null) {
                val querySnapshot = usersRef
                    .whereEqualTo("department", department.name)
                    .orderBy("score", Query.Direction.DESCENDING)
                    .get()
                    .await()
                querySnapshot.documents.map { it.toUser() }
            } else {
                val querySnapshot = usersRef
                    .orderBy("score", Query.Direction.DESCENDING)
                    .get()
                    .await()
                querySnapshot.documents.map { it.toUser() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun updateUserScore(userId: String, score: Int) {
        usersRef.document(userId).update("score", score)
    }

    override fun updateUserDepartment(uid: String, department: String) {
        auth.currentUser?.uid?.let { usersRef.document(it).update("department", department) }
    }

    override fun getQuizResult(userId: String): Flow<List<QuizResult>> {
        val query = usersRef.document(userId).collection("quizzes").orderBy("date", Query.Direction.DESCENDING)
        return query.snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects<QuizResult>()
            }
    }

    override suspend fun setAppTheme(departmentTheme: DepartmentTheme) {
        auth.currentUser?.uid?.let {
            usersRef.document(it).update(
                "setAppTheme",
                departmentTheme.name
            )
        }
    }

    override fun getAppTheme(): Flow<DepartmentTheme> {
        return auth.currentUser?.uid!!.let { uid ->
            usersRef.document(uid).snapshots().map { query ->
                query.getDepartmentTheme(
                    fieldName = "setAppTheme"
                )
            }
        }
    }

    override suspend fun addQuizResult(userId: String, quizResult: HashMap<String, Comparable<*>>) {
        db.collection("users").document(userId).collection("quizzes").add(quizResult)
    }
}