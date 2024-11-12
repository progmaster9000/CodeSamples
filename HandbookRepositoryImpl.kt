package theweeb.dev.practicingcompose.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import theweeb.dev.practicingcompose.domain.model.Chapter
import theweeb.dev.practicingcompose.domain.model.Question
import theweeb.dev.practicingcompose.domain.model.Section
import theweeb.dev.practicingcompose.domain.model.SubSection
import theweeb.dev.practicingcompose.domain.model.toChapter
import theweeb.dev.practicingcompose.domain.model.toQuestion
import theweeb.dev.practicingcompose.domain.repository.HandbookRepository
import javax.inject.Inject

class HandbookRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : HandbookRepository {

    private val sectionsCollection = db.collection("sections")

    override suspend fun upsertSubSection(subSection: SubSection) {
        try {
            val docRef = db.collection("sections")
                .document(subSection.sectionId)
                .collection("subSections")
                .document(subSection.subSectionId)

            val existingSubSection = docRef.get().await()

            val sectionDocRef = db.collection("sections").document(subSection.sectionId)
            val existingSection = sectionDocRef.get().await()

            val sectionUpdates = mutableMapOf<String, Any>()

            if (existingSubSection.exists()) {
                val existingData = existingSubSection.data ?: emptyMap<String, Any>()

                val updates = mutableMapOf<String, Any>()

                if (existingData["header"] != subSection.header) {
                    updates["header"] = subSection.header
                }

                val existingChapterIdsAndTitles = existingData["chapterIdsAndTitles"] as? Map<String, String> ?: emptyMap()
                val newChapterIdsAndTitles = subSection.chapterIdsAndTitles

                if (existingChapterIdsAndTitles != newChapterIdsAndTitles) {
                    updates["chapterIdsAndTitles"] = newChapterIdsAndTitles

                    val existingChapterIds = existingChapterIdsAndTitles.keys.toSet()
                    val newChapterIds = newChapterIdsAndTitles.keys.toSet()

                    val chaptersToDelete = existingChapterIds - newChapterIds

                    for (chapterId in chaptersToDelete) {
                        val chapterRef = db.collection("sections")
                            .document(subSection.sectionId)
                            .collection("subSections")
                            .document(subSection.subSectionId)
                            .collection("chapters")
                            .document(chapterId)

                        chapterRef.delete().await()
                        println("Deleted chapter with id: $chapterId")
                    }
                }

                if (updates.isNotEmpty()) {
                    docRef.update(updates).await()
                    println("Document updated with changes: $updates")
                } else {
                    println("No changes detected in subSection, skipping update.")
                }
            } else {
                docRef.set(subSection).await()
                println("Document created with subSectionId ${subSection.subSectionId}")
            }

            val sectionSubSectionIdsAndTitles = existingSection.data?.get("subSectionIdsAndTitles") as? Map<String, String> ?: emptyMap()
            val updatedSubSectionIdsAndTitles = sectionSubSectionIdsAndTitles.toMutableMap()

            updatedSubSectionIdsAndTitles[subSection.subSectionId] = subSection.header

            sectionUpdates["subSectionIdsAndTitles"] = updatedSubSectionIdsAndTitles
            sectionDocRef.set(sectionUpdates, SetOptions.merge()).await()

            println("Section updated with new subSectionIdsAndTitles: $updatedSubSectionIdsAndTitles")

        } catch (e: Exception) {
            println("Error upserting subSection: ${e.message}")
        }
    }

    override suspend fun deleteSubSection(subSection: SubSection) {
        try {
            val subSectionRef = db.collection("sections")
                .document(subSection.sectionId)
                .collection("subSections")
                .document(subSection.subSectionId)

            val chaptersQuery = subSectionRef.collection("chapters").get().await()

            for (chapter in chaptersQuery.documents) {
                chapter.reference.delete().await()
                println("Deleted chapter with id: ${chapter.id}")
            }

            subSectionRef.delete().await()
            println("Document with subSectionId ${subSection.subSectionId} successfully deleted.")

            val sectionRef = db.collection("sections").document(subSection.sectionId)
            val existingSection = sectionRef.get().await()

            if (existingSection.exists()) {
                val existingData = existingSection.data ?: emptyMap<String, Any>()
                val subSectionIdsAndTitles = existingData["subSectionIdsAndTitles"] as? MutableMap<String, String> ?: mutableMapOf()

                subSectionIdsAndTitles.remove(subSection.subSectionId)

                sectionRef.update("subSectionIdsAndTitles", subSectionIdsAndTitles).await()
                println("Removed subSectionId ${subSection.subSectionId} from subSectionIdsAndTitles in the section document.")
            }

        } catch (e: Exception) {
            println("Error deleting document: ${e.message}")
        }
    }

    override suspend fun addSubSection(subSection: SubSection) {
        TODO("Not yet implemented")
    }

    override suspend fun upsertChapter(chapter: Chapter) {
        try {
            if (chapter.sectionId.isBlank() || chapter.subSectionId.isBlank() || chapter.chapterId.isBlank()) {
                println("Error: sectionId, subSectionId, or chapterId cannot be empty.")
                return
            }

            val docRef = sectionsCollection
                .document(chapter.sectionId)
                .collection("subSections")
                .document(chapter.subSectionId)  // Reference the subSection to which the chapter belongs
                .collection("chapters")          // Reference the "chapters" subcollection
                .document(chapter.chapterId)      // Use the chapterId to uniquely identify the chapter

            // Perform an upsert (insert or update)
            docRef.set(chapter, SetOptions.merge()).await()

            println("Chapter successfully upserted with chapterId ${chapter.chapterId}")
        } catch (e: Exception) {
            println("Error upserting chapter: ${e.message}")
        }
    }

    override suspend fun deleteQuestion(sectionId: String, questionId: String) {
        try {
            val questionDocRef = db.collection("sections")
                .document(sectionId)
                .collection("questions")
                .document(questionId)

            questionDocRef.delete().await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun upsertQuestion(question: Question) {
        try {
            val questionCollectionRef = db.collection("sections")
                .document(question.sectionId)
                .collection("questions")

            if (question.questionId.isNotEmpty()) {
                val questionDocRef = questionCollectionRef.document(question.questionId)

                questionDocRef.set(question).await()

            } else {
                val newDocRef = questionCollectionRef.document() 
                val newQuestion = question.copy(questionId = newDocRef.id)

                newDocRef.set(newQuestion).await() 
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun getSections(): Flow<List<Section>>{
        return sectionsCollection.orderBy("sectionNumber", Query.Direction.ASCENDING).snapshots().map {
            it.toObjects<Section>()
        }
    }

    override fun getChapter(sectionId: String, subSectionId: String, chapterId: String): Flow<Chapter>{
        val chapterQuery = sectionsCollection
            .document(sectionId)
            .collection("subSections")
            .document(subSectionId)
            .collection("chapters")
            .document(chapterId)
        return chapterQuery.snapshots().map { it.toChapter() }
    }

    override fun getChapters(sectionId: String, subSectionId: String): Flow<List<Chapter>> {
        val chaptersQuery = sectionsCollection
            .document(sectionId)
            .collection("subSections")
            .document(subSectionId)
            .collection("chapters")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        return chaptersQuery.snapshots().map { query ->
            query.toObjects()
        }
    }

    override fun getQuestions(sectionId: String): Flow<List<Question>>{
        val questionQuery = sectionsCollection
            .document(sectionId)
            .collection("questions")
        return questionQuery.snapshots().map { query ->
            query.documents.map {
                it.toQuestion()
            }
        }
    }

    override fun getSubSections(sectionId: String): Flow<List<SubSection>>{
        val query = sectionsCollection
            .document(sectionId)
            .collection("subSections")
            .orderBy("createdAt", Query.Direction.ASCENDING)
        return query.snapshots().map { it.toObjects<SubSection>() }
    }

}
