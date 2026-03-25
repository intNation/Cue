
@Entity(entityName = "User")
data class UserEntity{

    @primaryKey(autoGenerate = true)
    val id: Long = 0

    @ColumnInfo(name = "first_name")
    val first_name: String

    @ColumnInfo(name = "last_name")
    val last_name: String

    @ColumnInfo(name = "email")
    val email: String

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
}
