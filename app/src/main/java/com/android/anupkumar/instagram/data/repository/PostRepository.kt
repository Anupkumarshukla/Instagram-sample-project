package com.android.anupkumar.instagram.data.repository

import com.android.anupkumar.instagram.data.local.db.DatabaseService
import com.android.anupkumar.instagram.data.model.Dummy
import com.android.anupkumar.instagram.data.model.Post
import com.android.anupkumar.instagram.data.model.User
import com.android.anupkumar.instagram.data.remote.NetworkService
import com.android.anupkumar.instagram.data.remote.request.DummyRequest
import com.android.anupkumar.instagram.data.remote.request.PostCreationRequest
import com.android.anupkumar.instagram.data.remote.request.PostLikeModifyRequest
import io.reactivex.Single
import javax.inject.Inject

class PostRepository @Inject constructor(
    private val networkService: NetworkService,
    private val databaseService: DatabaseService
) {

    fun fetchDummy(id: String): Single<List<Dummy>> =
        networkService.doDummyCall(DummyRequest(id)).map { it.data }

    fun fetchHomePostList(firstPostId: String?, lastPostId: String?, user: User): Single<List<Post>> {
        return networkService.doHomePostListCall(
            firstPostId,
            lastPostId,
            user.id,
            user.accessToken
        ).map { it.data }
    }

    fun makeLikePost(post: Post, user: User): Single<Post> {
        return networkService.doPostLikeCall(
            PostLikeModifyRequest(post.id),
            user.id,
            user.accessToken
        ).map {
            post.likedBy?.apply {
                this.find { postUser -> postUser.id == user.id } ?: this.add(
                    Post.User(
                        user.id,
                        user.name,
                        user.profilePicUrl
                    )
                )
            }
            return@map post
        }
    }

    fun makeUnlikePost(post: Post, user: User): Single<Post> {
        return networkService.doPostUnlikeCall(
            PostLikeModifyRequest(post.id),
            user.id,
            user.accessToken
        ).map {
            post.likedBy?.apply {
                this.find { postUser -> postUser.id == user.id }?.let { this.remove(it) }
            }
            return@map post
        }
    }

    fun createPost(imgUrl: String, imgWidth: Int, imgHeight: Int, user: User): Single<Post> =
        networkService.doPostCreationCall(
            PostCreationRequest(imgUrl, imgWidth, imgHeight), user.id, user.accessToken
        ).map {
            Post(
                it.data.id,
                it.data.imageUrl,
                it.data.imageWidth,
                it.data.imageHeight,
                Post.User(
                    user.id,
                    user.name,
                    user.profilePicUrl
                ),
                mutableListOf(),
                it.data.createdAt
            )
        }

}