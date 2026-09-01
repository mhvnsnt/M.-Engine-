package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Persistent hydration source for the Owner Context Graph.
 *
 * The directive is explicit that owner information must not be hardcoded in
 * Kotlin source. Before this, `ContextReconstructionEngine` built its
 * terminology constraint list from a literal in the middle of a function, so the
 * owner's stated preference could only be changed by editing and recompiling the
 * application.
 *
 * These tables make owner context data. Provenance matters here: a preference
 * the owner stated explicitly is a different kind of fact from one M. Engine
 * inferred, and `provenance` keeps them distinguishable.
 */
@Entity(tableName = "owner_goals")
data class OwnerGoalEntity(
    @PrimaryKey val id: String,
    val description: String,
    /** e.g. SECURITY, ABUNDANCE, INFLUENCE — matches OwnerGoal.category. */
    val category: String,
    val timeHorizon: String,
    val priority: Int,
    /** EXPLICIT | OBSERVED | INFERRED | CONFIRMED — never collapse these. */
    val provenance: String = "EXPLICIT",
    val createdAt: Long = System.currentTimeMillis(),
    /** Set when a later goal replaces this one; the row itself is preserved. */
    val supersededBy: String? = null,
)

@Entity(tableName = "terminology_preferences")
data class TerminologyPreferenceEntity(
    @PrimaryKey val rejectedTerm: String,
    val preferredTerm: String,
    val context: String,
    val provenance: String = "EXPLICIT",
    val createdAt: Long = System.currentTimeMillis(),
    val supersededBy: String? = null,
)

@Dao
interface OwnerContextDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: OwnerGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreference(pref: TerminologyPreferenceEntity)

    /** Active goals only — superseded rows remain readable for provenance. */
    @Query("SELECT * FROM owner_goals WHERE supersededBy IS NULL ORDER BY priority DESC")
    suspend fun activeGoals(): List<OwnerGoalEntity>

    @Query("SELECT * FROM owner_goals ORDER BY createdAt ASC")
    suspend fun allGoals(): List<OwnerGoalEntity>

    @Query("SELECT * FROM terminology_preferences WHERE supersededBy IS NULL")
    suspend fun activePreferences(): List<TerminologyPreferenceEntity>

    @Query("SELECT * FROM terminology_preferences")
    suspend fun allPreferences(): List<TerminologyPreferenceEntity>

    @Query("UPDATE owner_goals SET supersededBy = :successorId WHERE id = :id")
    suspend fun supersedeGoal(id: String, successorId: String)

    @Query("UPDATE terminology_preferences SET supersededBy = :successorTerm WHERE rejectedTerm = :term")
    suspend fun supersedePreference(term: String, successorTerm: String)

    @Query("SELECT COUNT(*) FROM terminology_preferences")
    suspend fun preferenceCount(): Int
}
