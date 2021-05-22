package awais.instagrabber.models

import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.Utils
import java.io.Serializable
import java.util.*

class Comment(
    val id: String,
    val text: String,
    val timestamp: Long,
    var likes: Long,
    private var liked: Boolean,
    val user: User,
    val replyCount: Int,
    val isChild: Boolean,
) : Serializable, Cloneable {
    val dateTime: String
        get() = Utils.datetimeParser.format(Date(timestamp * 1000L))

    fun getLiked(): Boolean {
        return liked
    }

    fun setLiked(liked: Boolean) {
        likes = if (liked) likes + 1 else likes - 1
        this.liked = liked
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Comment

        if (id != other.id) return false
        if (text != other.text) return false
        if (timestamp != other.timestamp) return false
        if (likes != other.likes) return false
        if (liked != other.liked) return false
        if (user != other.user) return false
        if (replyCount != other.replyCount) return false
        if (isChild != other.isChild) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + likes.hashCode()
        result = 31 * result + liked.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + replyCount
        result = 31 * result + isChild.hashCode()
        return result
    }

    override fun toString(): String {
        return "Comment(id='$id', text='$text', timestamp=$timestamp, likes=$likes, liked=$liked, user=$user, replyCount=$replyCount, isChild=$isChild)"
    }
}